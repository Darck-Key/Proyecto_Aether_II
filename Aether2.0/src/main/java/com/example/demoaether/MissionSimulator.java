package com.example.demoaether;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.frames.Frame;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;

/**
 * Motor de simulacion de la mision.
 *
 * Quien llama:
 * - HelloController.startSimulation() crea esta clase y llama prepareTrajectory() en un hilo aparte.
 * - HelloController.calculateOrbit() usa calculateInitialState() para un calculo rapido.
 *
 * A quien llama:
 * - ArtemisReferenceTrajectoryLoader lee el OEM nominal.
 * - OrekitTrajectoryPlanner calcula perfiles personalizados.
 * - MissionLogger escribe CSV.
 * - SimulationListener notifica a la interfaz cada cambio de estado.
 */
public class MissionSimulator {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double MOON_DISTANCE_KM = 384400.0;

    private final MissionConfig config;
    private SimulationListener listener;
    private MissionState currentState;
    private MissionLogger logger;
    private volatile boolean running;
    private volatile boolean paused;

    /**
     * Crea un simulador para una configuracion orbital.
     *
     * @param config parametros de mision que se pasaran a OrekitTrajectoryPlanner
     */
    public MissionSimulator(MissionConfig config) {
        this.config = config;
    }

    /**
     * Registra el listener que recibira estados y eventos de simulacion.
     *
     * @param listener normalmente HelloController
     */
    public void setSimulationListener(SimulationListener listener) {
        // Registra el receptor de eventos; normalmente es HelloController.
        this.listener = listener;
    }

    /**
     * Devuelve el ultimo estado emitido por la simulacion.
     *
     * @return estado actual o null si aun no se emitio ninguno
     */
    public MissionState getCurrentState() {
        return currentState;
    }

    /**
     * Indica si el ciclo de simulacion sigue activo.
     *
     * @return true si el motor esta recorriendo estados
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Indica si la simulacion esta pausada.
     *
     * @return true si el bucle esta esperando reanudacion
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Alterna entre pausa y reanudacion.
     */
    public void togglePaused() {
        paused = !paused;
    }

    /** Sincroniza la pausa del motor con TrajectoryPlayback. */
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    /**
     * Calcula el primer estado de la misma trayectoria que usara la simulacion.
     *
     * @param config parametros de mision
     * @return primer estado orbital disponible
     */
    public static MissionState calculateInitialState(MissionConfig config) {
        MissionTrajectory trajectory = buildTrajectory(config);
        if (trajectory.getStates().isEmpty()) {
            throw new IllegalStateException("La trayectoria calculada no contiene estados.");
        }
        return trajectory.getStates().get(0);
    }

    /**
     * Precalcula y registra la trayectoria fuera del hilo JavaFX.
     *
     * <p>Quien llama: HelloController.startSimulation(). A quien llama:
     * ArtemisReferenceTrajectoryLoader para el perfil nominal o
     * OrekitTrajectoryPlanner para entradas personalizadas.</p>
     */
    public MissionTrajectory prepareTrajectory() {
        running = true;
        paused = false;
        if (listener != null) {
            listener.onSimulationStarted();
        }
        MissionTrajectory trajectory = buildTrajectorySafely();
        writeTrajectoryLog(trajectory);
        return trajectory;
    }

    /** Entrega al listener un estado interpolado por TrajectoryPlayback. */
    public void publishState(MissionState state) {
        if (!running || state == null) {
            return;
        }
        currentState = state;
        if (listener != null) {
            listener.onStateUpdated(state);
        }
    }

    /** Cierra una corrida completa una sola vez y notifica al controlador. */
    public void completeSimulation() {
        if (!running) {
            return;
        }
        running = false;
        paused = false;
        if (listener != null) {
            listener.onSimulationFinished();
        }
    }

    /** Convierte una falla de precalculo/reproduccion en el callback comun de UI. */
    public void failSimulation(Exception exception) {
        running = false;
        paused = false;
        if (listener != null) {
            listener.onSimulationError(exception);
        }
    }

    /**
     * Ejecuta la simulacion completa.
     *
     * <p>Este metodo debe correr fuera del hilo JavaFX. Precalcula trayectoria,
     * emite MissionState al listener y registra CSV con MissionLogger.</p>
     */
    public void startSimulation() {
        // Compatibilidad para pruebas sin JavaFX. La interfaz usa TrajectoryPlayback.
        try {
            MissionTrajectory trajectory = prepareTrajectory();
            java.util.List<MissionState> states = trajectory.getStates();
            for (int index = 0; index < states.size(); index++) {
                MissionState state = states.get(index);
                while (paused && running) {
                    Thread.sleep(200);
                }
                if (!running) {
                    break;
                }

                publishState(state);
                if (index < states.size() - 1) {
                    double deltaSeconds = states.get(index + 1).getElapsedTime() - state.getElapsedTime();
                    Thread.sleep(calculateVisualDelayMillis(deltaSeconds, config.getSimulationSpeed()));
                }
            }
            if (running) {
                completeSimulation();
            }
        } catch (Exception e) {
            failSimulation(e);
        }
    }

    /**
     * Solicita detener el bucle de simulacion.
     */
    public void stopSimulation() {
        // Bandera usada por el bucle principal para terminar sin forzar el hilo.
        running = false;
        paused = false;
    }

    private long calculateVisualDelayMillis(double stepSeconds, int speed) {
        // Solo respalda pruebas sin JavaFX; la UI usa tiempo continuo en TrajectoryPlayback.
        int safeSpeed = Math.max(1, speed);
        double physicalDelay = (stepSeconds * 1000.0) / safeSpeed;
        return Math.max(1L, Math.min(250L, Math.round(physicalDelay)));
    }

    private void writeTrajectoryLog(MissionTrajectory trajectory) {
        // MissionLogger recibe los puntos fuente una sola vez; no duplica los frames interpolados.
        logger = new MissionLogger("mission-data.csv");
        try {
            for (MissionState state : trajectory.getStates()) {
                logger.logState(state);
            }
        } finally {
            logger.close();
            logger = null;
        }
    }

    private MissionState copyWithElapsedTime(MissionState state, double elapsedTime) {
        // Crea un estado final exacto para que la UI no siga contando despues de la duracion configurada.
        // Lo llama startSimulation() cuando el ultimo MissionState llega o supera el final de mision.
        return new MissionState(
                elapsedTime,
                state.getX(),
                state.getY(),
                state.getZ(),
                state.getVelocity(),
                state.getDistanceEarth(),
                state.getDistanceMoon(),
                state.getAltitude()
        );
    }

    private static MissionState fallbackInitialState(MissionConfig config) {
        // Estado minimo si Orekit no puede iniciar; mantiene telemetria y reportes funcionales.
        double distanceEarth = EARTH_RADIUS_KM + config.getInitialAltitude();
        double distanceMoon = Math.abs(MOON_DISTANCE_KM - distanceEarth);
        return new MissionState(
                0,
                distanceEarth,
                0,
                0,
                config.getInitialVelocity(),
                distanceEarth,
                distanceMoon,
                config.getInitialAltitude()
        );
    }

    private MissionTrajectory buildTrajectorySafely() {
        return buildTrajectory(config);
    }

    private static MissionTrajectory buildTrajectory(MissionConfig config) {
        // La escala 1x-1000x queda fuera de esta decision para no cambiar la ruta.
        config.validate();
        if (ArtemisReferenceTrajectoryLoader.supports(config)) {
            return ArtemisReferenceTrajectoryLoader.load();
        }
        return OrekitTrajectoryPlanner.precompute(config);
    }

    private boolean usesPresentationTrajectory() {
        // LEGACY, no invocado: la demo actual usa TrajectoryPlayback sobre la ruta real.
        return false;
    }

    private MissionTrajectory createDemoFlybyTrajectory() {
        // LEGACY, no invocado: se conserva por compatibilidad de lectura del proyecto.
        int endSecond = config.getSimulationHours() * 3600;
        int step = Math.max(1, config.getSimulationStepSeconds());
        java.util.List<MissionState> states = new java.util.ArrayList<>();
        java.util.List<String> events = new java.util.ArrayList<>();
        events.add("Demo rapida: trayectoria visual de sobrevuelo lunar completa");
        states.add(stateFromConfig(0));
        for (int second = step; second <= endSecond; second += step) {
            states.add(stateFromMissionProgress(second, endSecond));
        }
        return new MissionTrajectory(states, events, false);
    }

    private static MissionState stateFromSpacecraftState(SpacecraftState state, int elapsedSeconds) {
        // Convierte unidades de Orekit de metros a kilometros y las adapta al modelo MissionState.
        Orbit orbit = state.getOrbit();
        PVCoordinates pv = state.getPVCoordinates();
        double x = pv.getPosition().getX() / 1000;
        double y = pv.getPosition().getY() / 1000;
        double z = pv.getPosition().getZ() / 1000;
        double velocity = pv.getVelocity().getNorm() / 1000;
        double distanceEarth = pv.getPosition().getNorm() / 1000;
        double distanceMoon = distanceToMoonKm(pv, orbit.getFrame(), state.getDate());
        double altitude = distanceEarth - EARTH_RADIUS_KM;

        return new MissionState(
                elapsedSeconds,
                x,
                y,
                z,
                velocity,
                distanceEarth,
                distanceMoon,
                altitude
        );
    }

    private static MissionState stateFromOrbit(Orbit orbit, int elapsedSeconds) {
        PVCoordinates pv = orbit.getPVCoordinates();
        double x = pv.getPosition().getX() / 1000;
        double y = pv.getPosition().getY() / 1000;
        double z = pv.getPosition().getZ() / 1000;
        double velocity = pv.getVelocity().getNorm() / 1000;
        double distanceEarth = pv.getPosition().getNorm() / 1000;
        double distanceMoon = Math.abs(MOON_DISTANCE_KM - distanceEarth);
        double altitude = distanceEarth - EARTH_RADIUS_KM;

        return new MissionState(
                elapsedSeconds,
                x,
                y,
                z,
                velocity,
                distanceEarth,
                distanceMoon,
                altitude
        );
    }

    private MissionState stateFromConfig(int elapsedSeconds) {
        double distanceEarth = EARTH_RADIUS_KM + config.getInitialAltitude();
        double distanceMoon = Math.abs(MOON_DISTANCE_KM - distanceEarth);
        return new MissionState(
                elapsedSeconds,
                distanceEarth,
                0,
                0,
                config.getInitialVelocity(),
                distanceEarth,
                distanceMoon,
                config.getInitialAltitude()
        );
    }

    private MissionTrajectory createFallbackTrajectory(RuntimeException exception) {
        // LEGACY, no invocado: una falla fisica ahora se muestra como error y no como ruta ficticia.
        int endSecond = config.getSimulationHours() * 3600;
        int step = Math.max(1, config.getSimulationStepSeconds());
        java.util.List<MissionState> states = new java.util.ArrayList<>();
        java.util.List<String> events = new java.util.ArrayList<>();
        events.add("Fallback visual activado: " + safeExceptionSummary(exception));
        states.add(stateFromConfig(0));
        for (int second = step; second <= endSecond; second += step) {
            states.add(stateFromMissionProgress(second, endSecond));
        }
        return new MissionTrajectory(states, events, false);
    }

    private String safeExceptionSummary(Throwable exception) {
        // Orekit puede fallar al construir mensajes localizados dentro de modulos Java.
        // Esta lectura evita exception.getMessage() y resume el tipo/causa sin disparar ResourceBundle.Control.
        if (exception == null) {
            return "error desconocido";
        }
        Throwable cause = exception.getCause();
        if (cause == null || cause == exception) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + " causado por " + cause.getClass().getSimpleName();
    }

    private MissionState stateFromMissionProgress(int elapsedSeconds, int endSecond) {
        // Modelo visual simple de ida a la Luna, sobrevuelo y regreso para la demo/fallback.
        double progress = Math.min(1.0, Math.max(0.0, elapsedSeconds / (double) Math.max(1, endSecond)));
        double lunarApproach;
        double visualX;
        double visualY;
        double visualZ;

        if (progress < 0.48) {
            double u = smoothStep(progress / 0.48);
            lunarApproach = u;
            visualX = -205 + u * 455;
            visualY = -18 - Math.sin(u * Math.PI) * 78;
            visualZ = Math.sin(u * Math.PI) * 42;
        } else if (progress < 0.64) {
            double u = (progress - 0.48) / 0.16;
            double angle = Math.toRadians(-155 + 305 * u);
            lunarApproach = 1.0;
            visualX = 275 + Math.cos(angle) * 58;
            visualY = -18 + Math.sin(angle) * 54;
            visualZ = -18 + Math.sin(angle * 0.7) * 28;
        } else {
            double u = smoothStep((progress - 0.64) / 0.36);
            lunarApproach = 1.0 - u;
            visualX = 235 - u * 440;
            visualY = 28 + Math.sin(u * Math.PI) * 72;
            visualZ = 22 - Math.sin(u * Math.PI) * 38;
        }

        double distanceEarth = EARTH_RADIUS_KM + config.getInitialAltitude()
                + lunarApproach * (MOON_DISTANCE_KM - EARTH_RADIUS_KM - config.getInitialAltitude() - 6500);
        double distanceMoon = Math.max(1800, Math.abs(MOON_DISTANCE_KM - distanceEarth));
        double altitude = Math.max(config.getInitialAltitude(), distanceEarth - EARTH_RADIUS_KM);
        double velocity = config.getInitialVelocity()
                + Math.sin(progress * Math.PI * 2.0) * 0.55
                - Math.max(0, progress - 0.7) * 1.2;

        return new MissionState(
                elapsedSeconds,
                visualX,
                visualY,
                visualZ,
                Math.max(0.8, velocity),
                distanceEarth,
                distanceMoon,
                altitude
        );
    }

    private static double smoothStep(double value) {
        // Suaviza transiciones visuales de la trayectoria de respaldo.
        double t = Math.min(1.0, Math.max(0.0, value));
        return t * t * (3.0 - 2.0 * t);
    }

    private static NumericalPropagator createNumericalPropagator(Orbit initialOrbit, MissionConfig config) {
        // Propagador numerico base con gravedad terrestre, lunar y solar.
        double[][] tolerances = NumericalPropagator.tolerances(10.0, initialOrbit, initialOrbit.getType());
        DormandPrince853Integrator integrator = new DormandPrince853Integrator(
                0.001,
                Math.max(60.0, config.getSimulationStepSeconds()),
                tolerances[0],
                tolerances[1]
        );
        integrator.setInitialStepSize(Math.min(30.0, Math.max(1.0, config.getSimulationStepSeconds())));

        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(initialOrbit, config.getSpacecraftMass()));
        propagator.addForceModel(new NewtonianAttraction(Constants.WGS84_EARTH_MU));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getMoon()));
        propagator.addForceModel(new ThirdBodyAttraction(CelestialBodyFactory.getSun()));
        return propagator;
    }

    private static double distanceToMoonKm(PVCoordinates spacecraftPv, Frame frame, AbsoluteDate date) {
        // Consulta la posicion lunar en Orekit para calcular distancia nave-Luna.
        CelestialBody moon = CelestialBodyFactory.getMoon();
        Vector3D moonPosition = moon.getPVCoordinates(date, frame).getPosition();
        return Vector3D.distance(spacecraftPv.getPosition(), moonPosition) / 1000.0;
    }
}
