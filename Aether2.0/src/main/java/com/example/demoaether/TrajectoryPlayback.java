package com.example.demoaether;

import javafx.animation.AnimationTimer;

import java.util.List;
import java.util.function.Consumer;

/**
 * Reloj visual que reproduce una MissionTrajectory sin recalcular su fisica.
 *
 * <p>Quien lo llama: HelloController despues de que MissionSimulator termina
 * el precalculo en segundo plano. A quien llama: entrega un MissionState
 * interpolado al controlador en cada pulso de JavaFX y ejecuta onFinished al
 * alcanzar exactamente el ultimo estado.</p>
 */
public final class TrajectoryPlayback extends AnimationTimer {

    /** Duracion de pared de la demo; la trayectoria y telemetria no cambian. */
    public static final double QUICK_DEMO_SECONDS = 32.0;

    private final List<MissionState> states;
    private final double trajectoryDurationSeconds;
    private final double playbackRate;
    private final boolean quickDemo;
    private final Consumer<MissionState> stateConsumer;
    private final Runnable onFinished;

    private long startNanos = -1L;
    private long pauseStartedNanos = -1L;
    private long pausedNanos;
    private boolean running;
    private boolean paused;
    private boolean firstStateSent;

    /**
     * @param trajectory estados orbitales inmutables ya calculados
     * @param playbackRate escala real de reproduccion, entre 1x y 1000x
     * @param quickDemo true para recorrer la misma trayectoria en 32 segundos
     * @param stateConsumer receptor de telemetria, normalmente MissionSimulator.publishState
     * @param onFinished cierre invocado una sola vez al llegar al ultimo estado
     */
    public TrajectoryPlayback(
            MissionTrajectory trajectory,
            double playbackRate,
            boolean quickDemo,
            Consumer<MissionState> stateConsumer,
            Runnable onFinished) {
        this.states = trajectory.getStates();
        if (states.isEmpty()) {
            throw new IllegalArgumentException("La trayectoria no contiene estados para reproducir.");
        }
        this.trajectoryDurationSeconds = states.get(states.size() - 1).getElapsedTime();
        this.playbackRate = Math.max(1.0, Math.min(1000.0, playbackRate));
        this.quickDemo = quickDemo;
        this.stateConsumer = stateConsumer;
        this.onFinished = onFinished;
    }

    @Override
    public void start() {
        startNanos = -1L;
        pauseStartedNanos = -1L;
        pausedNanos = 0L;
        paused = false;
        running = true;
        firstStateSent = false;
        super.start();
    }

    @Override
    public void handle(long now) {
        if (!running || paused) {
            return;
        }
        if (startNanos < 0L) {
            startNanos = now;
        }
        if (!firstStateSent) {
            stateConsumer.accept(states.get(0));
            firstStateSent = true;
        }

        double wallSeconds = Math.max(0.0, (now - startNanos - pausedNanos) / 1_000_000_000.0);
        double simulatedSeconds = quickDemo
                ? wallSeconds * trajectoryDurationSeconds / QUICK_DEMO_SECONDS
                : wallSeconds * playbackRate;
        simulatedSeconds = Math.min(trajectoryDurationSeconds, simulatedSeconds);
        stateConsumer.accept(sampleAt(states, simulatedSeconds));

        if (simulatedSeconds >= trajectoryDurationSeconds) {
            finish();
        }
    }

    /** Alterna la pausa manteniendo el mismo instante orbital al reanudar. */
    public boolean togglePaused() {
        if (!running) {
            return false;
        }
        long now = System.nanoTime();
        if (paused) {
            pausedNanos += Math.max(0L, now - pauseStartedNanos);
            pauseStartedNanos = -1L;
            paused = false;
        } else {
            pauseStartedNanos = now;
            paused = true;
        }
        return paused;
    }

    /** Cancela la reproduccion sin emitir el evento de mision completada. */
    public void cancel() {
        running = false;
        paused = false;
        super.stop();
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public double getTrajectoryDurationSeconds() {
        return trajectoryDurationSeconds;
    }

    private void finish() {
        if (!running) {
            return;
        }
        running = false;
        paused = false;
        super.stop();
        stateConsumer.accept(states.get(states.size() - 1));
        onFinished.run();
    }

    /**
     * Interpola entre los dos estados que rodean el tiempo solicitado.
     * Es package-private para que las pruebas E5 validen continuidad y limites.
     */
    static MissionState sampleAt(List<MissionState> states, double elapsedSeconds) {
        if (states == null || states.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos un estado.");
        }
        if (elapsedSeconds <= states.get(0).getElapsedTime()) {
            return states.get(0);
        }
        int lastIndex = states.size() - 1;
        if (elapsedSeconds >= states.get(lastIndex).getElapsedTime()) {
            return states.get(lastIndex);
        }

        int low = 0;
        int high = lastIndex;
        while (low + 1 < high) {
            int middle = (low + high) >>> 1;
            if (states.get(middle).getElapsedTime() <= elapsedSeconds) {
                low = middle;
            } else {
                high = middle;
            }
        }

        MissionState from = states.get(low);
        MissionState to = states.get(high);
        double span = Math.max(1.0e-9, to.getElapsedTime() - from.getElapsedTime());
        double ratio = (elapsedSeconds - from.getElapsedTime()) / span;
        return interpolate(from, to, ratio, elapsedSeconds);
    }

    private static MissionState interpolate(
            MissionState from,
            MissionState to,
            double ratio,
            double elapsedSeconds) {
        return new MissionState(
                elapsedSeconds,
                lerp(from.getX(), to.getX(), ratio),
                lerp(from.getY(), to.getY(), ratio),
                lerp(from.getZ(), to.getZ(), ratio),
                lerp(from.getVelocity(), to.getVelocity(), ratio),
                lerp(from.getDistanceEarth(), to.getDistanceEarth(), ratio),
                lerp(from.getDistanceMoon(), to.getDistanceMoon(), ratio),
                lerp(from.getAltitude(), to.getAltitude(), ratio),
                lerpOptional(from.getMoonX(), to.getMoonX(), ratio),
                lerpOptional(from.getMoonY(), to.getMoonY(), ratio),
                lerpOptional(from.getMoonZ(), to.getMoonZ(), ratio)
        );
    }

    private static double lerp(double from, double to, double ratio) {
        return from + (to - from) * ratio;
    }

    private static double lerpOptional(double from, double to, double ratio) {
        return Double.isFinite(from) && Double.isFinite(to) ? lerp(from, to, ratio) : Double.NaN;
    }
}
