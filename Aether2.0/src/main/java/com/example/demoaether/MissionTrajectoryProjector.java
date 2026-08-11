package com.example.demoaether;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Proyecta coordenadas EME2000 a las coordenadas visuales del mapa AETHER.
 *
 * <p>Quien la llama: MissionMap3D.configureTrajectory(). A quien llama:
 * no llama a la interfaz ni a Orekit; consume los vectores ya calculados en
 * MissionState. La Tierra ocupa el centro y el eje horizontal apunta hacia la
 * Luna durante el periapsis. {@link #project(MissionState)} conserva la
 * transformacion fisica rigida de Orion; {@link #projectForDisplay(MissionState)}
 * aplica solamente una lente local de presentacion. La Luna usa sus vectores
 * reales en una vista orbital inclinada independiente para mantenerla visible
 * sin deformar ni amontonar la trayectoria de la nave.</p>
 */
final class MissionTrajectoryProjector {

    static final double EARTH_X = 0.0;
    static final double MOON_X = 300.0;
    static final double DISPLAY_EARTH_RADIUS = 16.0;
    static final double DISPLAY_MOON_RADIUS = 12.5;
    static final double EARTH_DISPLAY_INFLUENCE_RADIUS = 50.0;
    static final double MOON_DISPLAY_INFLUENCE_RADIUS = 34.0;
    private static final double EARTH_MEAN_RADIUS_KM = 6_378.137;
    private static final double MOON_MEAN_RADIUS_KM = 1_737.4;
    private static final double EARTH_ROUTE_CLEARANCE = 4.0;
    private static final double MOON_ROUTE_CLEARANCE = 11.0;
    private static final double LUNAR_ORBIT_VIEW_TILT_DEGREES = 18.0;
    private static final int MAX_DISPLAY_POINTS = 680;

    private final double[] axisX;
    private final double[] axisY;
    private final double[] axisZ;
    private final double[] moonAxisY;
    private final double[] moonAxisZ;
    private final double spatialScale;
    private final List<ProjectedPoint> displayPoints;
    private final double encounterElapsedSeconds;

    private MissionTrajectoryProjector(
            double[] axisX,
            double[] axisY,
            double[] axisZ,
            double[] moonAxisY,
            double[] moonAxisZ,
            double spatialScale,
            List<ProjectedPoint> displayPoints,
            double encounterElapsedSeconds) {
        this.axisX = axisX;
        this.axisY = axisY;
        this.axisZ = axisZ;
        this.moonAxisY = moonAxisY;
        this.moonAxisZ = moonAxisZ;
        this.spatialScale = spatialScale;
        this.displayPoints = List.copyOf(displayPoints);
        this.encounterElapsedSeconds = encounterElapsedSeconds;
    }

    /** Construye una base estable a partir de la trayectoria completa. */
    static MissionTrajectoryProjector from(MissionTrajectory trajectory) {
        List<MissionState> states = trajectory.getStates();
        if (states.size() < 2) {
            throw new IllegalArgumentException("Se necesitan al menos dos estados para proyectar la trayectoria.");
        }

        int encounter = findLunarEncounter(states);
        MissionState encounterState = states.get(encounter);
        double[] moonVector = encounterState.hasMoonPosition()
                ? vector(encounterState.getMoonX(), encounterState.getMoonY(), encounterState.getMoonZ())
                : vector(encounterState.getX(), encounterState.getY(), encounterState.getZ());
        double[] axisX = normalize(moonVector, vector(1.0, 0.0, 0.0));

        MissionPhaseTimeline timeline = MissionPhaseTimeline.from(trajectory);
        int departure = findNearestElapsedIndex(states, timeline.getTliCenterSeconds());

        // La vista por defecto muestra de lado tanto el perigeo terrestre como
        // el paso por la Luna. Solo rota la base: no altera ningun vector OEM.
        double[] earthPerigeeOffset = removeComponent(position(states.get(departure)), axisX);
        double[] lunarFlybyOffset = removeComponent(
                subtract(position(encounterState), moonVector),
                axisX);

        int before = Math.max(0, encounter - 3);
        int after = Math.min(states.size() - 1, encounter + 3);
        double[] encounterTangent = subtract(position(states.get(after)), position(states.get(before)));
        double[] tangentPerpendicular = removeComponent(encounterTangent, axisX);
        double[] routeViewAxis = balancedViewAxis(
                axisX,
                earthPerigeeOffset,
                lunarFlybyOffset,
                tangentPerpendicular);

        MissionState outboundReference = states.get(Math.max(1, encounter / 2));
        double[] axisY = routeViewAxis;
        if (dot(position(outboundReference), axisY) > 0.0) {
            axisY = multiply(axisY, -1.0);
        }
        double[] axisZ = normalize(cross(axisX, axisY), vector(0.0, 0.0, 1.0));

        // La vista lunar independiente proyecta su plano como una elipse. Asi
        // la Tierra permanece centrada y la ruta de Orion conserva su vista limpia.
        double[] moonBeforePosition = moonPosition(states.get(before));
        double[] moonAfterPosition = moonPosition(states.get(after));
        double[] lunarOrbitTangent = normalize(
                removeComponent(subtract(moonAfterPosition, moonBeforePosition), axisX),
                routeViewAxis);
        if (dot(lunarOrbitTangent, axisY) < 0.0) {
            lunarOrbitTangent = multiply(lunarOrbitTangent, -1.0);
        }
        double[] lunarOrbitNormal = normalize(cross(axisX, lunarOrbitTangent), axisY);
        if (dot(lunarOrbitNormal, axisY) < 0.0) {
            lunarOrbitNormal = multiply(lunarOrbitNormal, -1.0);
        }
        double viewTilt = Math.toRadians(LUNAR_ORBIT_VIEW_TILT_DEGREES);
        double[] moonAxisY = normalize(add(
                multiply(lunarOrbitNormal, Math.cos(viewTilt)),
                multiply(lunarOrbitTangent, Math.sin(viewTilt))), axisY);
        double[] moonAxisZ = normalize(
                cross(axisX, moonAxisY), vector(0.0, 0.0, 1.0));

        double moonDistance = Math.max(1.0, norm(moonVector));
        // Una sola escala conserva distancias y angulos entre los tres ejes.
        // La camara de MissionMap3D se ocupa de encuadrar la ruta completa.
        double spatialScale = (MOON_X - EARTH_X) / moonDistance;

        List<Integer> indexes = displayIndexes(states, encounter);
        List<ProjectedPoint> points = new ArrayList<>(indexes.size());
        for (int index : indexes) {
            MissionState state = states.get(index);
            double[] physicalProjection = project(state, axisX, axisY, axisZ, spatialScale);
            double[] displayProjection = applyDisplayLens(physicalProjection, spatialScale);
            points.add(new ProjectedPoint(
                    state.getElapsedTime(),
                    displayProjection));
        }
        points.sort(Comparator.comparingDouble(ProjectedPoint::elapsedSeconds));
        return new MissionTrajectoryProjector(
                axisX,
                axisY,
                axisZ,
                moonAxisY,
                moonAxisZ,
                spatialScale,
                points,
                states.get(encounter).getElapsedTime());
    }

    /** Proyecta un estado interpolado con la misma base usada por la ruta completa. */
    double[] project(MissionState state) {
        return project(state, axisX, axisY, axisZ, spatialScale);
    }

    /**
     * Proyecta un estado para el render y aplica la misma lente visual usada por la guia.
     * Quien llama: MissionMap3D para mover la nave sobre la misma linea que se dibuja.
     * La lente no modifica MissionState, telemetria, tiempos ni resultados de Orekit.
     */
    double[] projectForDisplay(MissionState state) {
        return applyDisplayLens(project(state), spatialScale);
    }

    /**
     * Proyecta la Luna con el plano orbital inclinado usado solo por el render.
     * Quien llama: MissionMap3D.updateMoonPosition() en cada cuadro. El tiempo,
     * radio, sentido y fase proceden del vector lunar real del MissionState.
     */
    double[] projectMoonForDisplay(MissionState state) {
        if (state == null || !state.hasMoonPosition()) {
            return new double[]{MOON_X, 0.0, 0.0};
        }
        return projectPosition(
                vector(state.getMoonX(), state.getMoonY(), state.getMoonZ()),
                axisX,
                moonAxisY,
                moonAxisZ,
                spatialScale);
    }

    /** Proyeccion rigida lunar usada para validar separaciones fisicas. */
    double[] projectMoonPhysical(MissionState state) {
        if (state == null || !state.hasMoonPosition()) {
            return new double[]{MOON_X, 0.0, 0.0};
        }
        return projectPosition(
                vector(state.getMoonX(), state.getMoonY(), state.getMoonZ()),
                axisX,
                axisY,
                axisZ,
                spatialScale);
    }

    /**
     * Convierte una distancia fisica en kilometros a unidades del mapa.
     * Quien llama: MissionMap3D para que los radios no invadan la trayectoria real.
     */
    double scaleDistance(double kilometers) {
        if (!Double.isFinite(kilometers) || kilometers < 0.0) {
            throw new IllegalArgumentException("La distancia a escalar debe ser finita y no negativa.");
        }
        return kilometers * spatialScale;
    }

    List<ProjectedPoint> getDisplayPoints() {
        return displayPoints;
    }

    double getEncounterElapsedSeconds() {
        return encounterElapsedSeconds;
    }

    private static double[] project(
            MissionState state,
            double[] axisX,
            double[] axisY,
            double[] axisZ,
            double spatialScale) {
        return projectPosition(position(state), axisX, axisY, axisZ, spatialScale);
    }

    private static double[] projectPosition(
            double[] position,
            double[] axisX,
            double[] axisY,
            double[] axisZ,
            double spatialScale) {
        return new double[]{
                EARTH_X + dot(position, axisX) * spatialScale,
                dot(position, axisY) * spatialScale,
                dot(position, axisZ) * spatialScale
        };
    }

    /**
     * Amplia solo el detalle cercano a Tierra y Luna para la vista 3D.
     * Los puntos de espacio profundo quedan identicos a la proyeccion fisica,
     * mientras los encuentros cercanos reciben una transicion suave y continua.
     */
    private static double[] applyDisplayLens(double[] point, double spatialScale) {
        double[] earthAdjusted = expandNearBody(
                point,
                EARTH_X,
                EARTH_MEAN_RADIUS_KM * spatialScale,
                DISPLAY_EARTH_RADIUS + EARTH_ROUTE_CLEARANCE,
                EARTH_DISPLAY_INFLUENCE_RADIUS);
        return expandNearBody(
                earthAdjusted,
                MOON_X,
                MOON_MEAN_RADIUS_KM * spatialScale,
                DISPLAY_MOON_RADIUS + MOON_ROUTE_CLEARANCE,
                MOON_DISPLAY_INFLUENCE_RADIUS);
    }

    /**
     * Reescala radialmente un vecindario sin saltos ni cambios bruscos de
     * pendiente: el borde fisico pasa al borde visual y la salida recupera
     * derivada 1 antes de enlazar con la proyeccion sin modificar. Se escala
     * tambien Z para conservar la direccion 3D local.
     */
    private static double[] expandNearBody(
            double[] point,
            double centerX,
            double physicalRadius,
            double visualClearanceRadius,
            double influenceRadius) {
        double dx = point[0] - centerX;
        double dy = point[1];
        double planarDistance = Math.hypot(dx, dy);
        if (!Double.isFinite(planarDistance) || planarDistance >= influenceRadius) {
            return point;
        }
        if (planarDistance < 1.0e-9) {
            return new double[]{centerX, -visualClearanceRadius, point[2]};
        }

        double range = Math.max(1.0e-9, influenceRadius - physicalRadius);
        double progress = clamp((planarDistance - physicalRadius) / range, 0.0, 1.0);
        double progress2 = progress * progress;
        double progress3 = progress2 * progress;
        double startWeight = 2.0 * progress3 - 3.0 * progress2 + 1.0;
        double endWeight = -2.0 * progress3 + 3.0 * progress2;
        double endSlopeWeight = progress3 - progress2;
        double displayDistance = startWeight * visualClearanceRadius
                + endWeight * influenceRadius
                + endSlopeWeight * range;
        double factor = displayDistance / planarDistance;
        return new double[]{
                centerX + dx * factor,
                dy * factor,
                point[2] * factor
        };
    }

    private static int findLunarEncounter(List<MissionState> states) {
        int result = 0;
        for (int i = 1; i < states.size(); i++) {
            if (states.get(i).getDistanceMoon() < states.get(result).getDistanceMoon()) {
                result = i;
            }
        }
        return result;
    }

    private static List<Integer> displayIndexes(List<MissionState> states, int encounter) {
        int size = states.size();
        int stride = Math.max(1, (int) Math.ceil(size / (double) MAX_DISPLAY_POINTS));
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < size; index++) {
            MissionState state = states.get(index);
            boolean closeEarth = state.getDistanceEarth() < 45_000.0;
            boolean closeMoon = state.getDistanceMoon() < 30_000.0;
            boolean nearEarth = state.getDistanceEarth() < 100_000.0;
            boolean nearMoon = state.getDistanceMoon() < 80_000.0;
            boolean encounterDetail = Math.abs(index - encounter) <= 24;
            int localStride = closeEarth || closeMoon
                    ? 1
                    : nearEarth || nearMoon ? Math.max(1, stride / 3) : stride;
            if (index % localStride == 0 || encounterDetail) {
                indexes.add(index);
            }
        }
        indexes.add(encounter);
        indexes.add(size - 1);
        return indexes.stream().distinct().sorted().toList();
    }

    private static double[] position(MissionState state) {
        return vector(state.getX(), state.getY(), state.getZ());
    }

    private static double[] moonPosition(MissionState state) {
        return state.hasMoonPosition()
                ? vector(state.getMoonX(), state.getMoonY(), state.getMoonZ())
                : position(state);
    }

    /** Busca el estado fuente mas cercano a un hito calculado por MissionPhaseTimeline. */
    private static int findNearestElapsedIndex(List<MissionState> states, double elapsedSeconds) {
        int result = 0;
        double minimumDifference = Double.POSITIVE_INFINITY;
        for (int index = 0; index < states.size(); index++) {
            double difference = Math.abs(states.get(index).getElapsedTime() - elapsedSeconds);
            if (difference < minimumDifference) {
                result = index;
                minimumDifference = difference;
            }
        }
        return result;
    }

    /**
     * Elige una orientacion que mantenga visibles los dos encuentros cercanos.
     * Alinea los vectores antes de promediarlos para no cancelar lados opuestos.
     */
    private static double[] balancedViewAxis(
            double[] axisX,
            double[] earthOffset,
            double[] moonOffset,
            double[] fallback) {
        double[] safeFallback = normalize(fallback, perpendicularTo(axisX));
        double[] earthAxis = normalize(earthOffset, safeFallback);
        double[] moonAxis = normalize(moonOffset, safeFallback);
        if (dot(earthAxis, moonAxis) < 0.0) {
            moonAxis = multiply(moonAxis, -1.0);
        }
        return normalize(add(earthAxis, moonAxis), safeFallback);
    }

    private static double[] removeComponent(double[] vector, double[] axis) {
        return subtract(vector, multiply(axis, dot(vector, axis)));
    }

    private static double[] perpendicularTo(double[] vector) {
        double[] reference = Math.abs(vector[2]) < 0.8
                ? vector(0.0, 0.0, 1.0)
                : vector(0.0, 1.0, 0.0);
        return cross(reference, vector);
    }

    private static double[] vector(double x, double y, double z) {
        return new double[]{x, y, z};
    }

    private static double[] subtract(double[] first, double[] second) {
        return vector(first[0] - second[0], first[1] - second[1], first[2] - second[2]);
    }

    private static double[] add(double[] first, double[] second) {
        return vector(first[0] + second[0], first[1] + second[1], first[2] + second[2]);
    }

    private static double[] multiply(double[] vector, double scalar) {
        return new double[]{vector[0] * scalar, vector[1] * scalar, vector[2] * scalar};
    }

    private static double dot(double[] first, double[] second) {
        return first[0] * second[0] + first[1] * second[1] + first[2] * second[2];
    }

    private static double[] cross(double[] first, double[] second) {
        return vector(
                first[1] * second[2] - first[2] * second[1],
                first[2] * second[0] - first[0] * second[2],
                first[0] * second[1] - first[1] * second[0]
        );
    }

    private static double norm(double[] vector) {
        return Math.sqrt(dot(vector, vector));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double[] normalize(double[] vector, double[] fallback) {
        double magnitude = norm(vector);
        return magnitude < 1.0e-9 ? fallback : multiply(vector, 1.0 / magnitude);
    }

    /** Punto visual asociado a su tiempo de mision para revelar la ruta correcta. */
    record ProjectedPoint(double elapsedSeconds, double[] coordinates) {
    }
}
