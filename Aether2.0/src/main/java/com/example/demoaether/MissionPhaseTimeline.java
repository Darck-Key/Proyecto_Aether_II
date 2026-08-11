package com.example.demoaether;

import java.util.ArrayList;
import java.util.List;

/**
 * Identifica los hitos visibles de una trayectoria a partir de su geometria.
 *
 * <p>Quien la llama: HelloController para nombrar la fase activa y MissionMap3D
 * para colorear la ruta. A quien llama: solo consulta los MissionState ya
 * calculados por Orekit; no modifica la efemeride ni inventa posiciones.</p>
 */
final class MissionPhaseTimeline {

    private static final double TLI_LEAD_SECONDS = 60.0 * 60.0;
    private static final double TLI_FOLLOW_SECONDS = 2.0 * 60.0 * 60.0;
    private static final double LUNAR_INFLUENCE_DISTANCE_KM = 70_000.0;
    private static final double REENTRY_APPROACH_DISTANCE_KM = 100_000.0;

    private final double startSeconds;
    private final double tliCenterSeconds;
    private final double tliStartSeconds;
    private final double tliEndSeconds;
    private final double lunarEntrySeconds;
    private final double lunarEncounterSeconds;
    private final double lunarExitSeconds;
    private final double reentryStartSeconds;
    private final double endSeconds;

    private MissionPhaseTimeline(
            double startSeconds,
            double tliCenterSeconds,
            double tliStartSeconds,
            double tliEndSeconds,
            double lunarEntrySeconds,
            double lunarEncounterSeconds,
            double lunarExitSeconds,
            double reentryStartSeconds,
            double endSeconds) {
        this.startSeconds = startSeconds;
        this.tliCenterSeconds = tliCenterSeconds;
        this.tliStartSeconds = tliStartSeconds;
        this.tliEndSeconds = tliEndSeconds;
        this.lunarEntrySeconds = lunarEntrySeconds;
        this.lunarEncounterSeconds = lunarEncounterSeconds;
        this.lunarExitSeconds = lunarExitSeconds;
        this.reentryStartSeconds = reentryStartSeconds;
        this.endSeconds = endSeconds;
    }

    /** Construye la secuencia buscando perigeo de salida, periapsis lunar y retorno terrestre. */
    static MissionPhaseTimeline from(MissionTrajectory trajectory) {
        if (trajectory == null || trajectory.getStates().size() < 3) {
            throw new IllegalArgumentException("La trayectoria requiere al menos tres estados.");
        }

        List<MissionState> states = trajectory.getStates();
        int encounterIndex = indexOfMinimumMoonDistance(states);
        int tliIndex = indexOfDeparturePerigee(states, encounterIndex);
        int lunarEntryIndex = firstWithinMoonDistance(states, 0, encounterIndex);
        int lunarExitIndex = firstOutsideMoonDistance(states, encounterIndex);
        int reentryIndex = firstReturnWithinEarthDistance(states, lunarExitIndex);

        double start = states.get(0).getElapsedTime();
        double tliCenter = states.get(tliIndex).getElapsedTime();
        double lunarEntry = Math.max(tliCenter, states.get(lunarEntryIndex).getElapsedTime());
        double encounter = Math.max(lunarEntry, states.get(encounterIndex).getElapsedTime());
        double lunarExit = Math.max(encounter, states.get(lunarExitIndex).getElapsedTime());
        double reentry = Math.max(lunarExit, states.get(reentryIndex).getElapsedTime());
        double end = Math.max(reentry, states.get(states.size() - 1).getElapsedTime());

        double tliStart = Math.max(start, tliCenter - TLI_LEAD_SECONDS);
        double tliEnd = Math.min(lunarEntry, tliCenter + TLI_FOLLOW_SECONDS);
        return new MissionPhaseTimeline(
                start,
                tliCenter,
                tliStart,
                tliEnd,
                lunarEntry,
                encounter,
                lunarExit,
                reentry,
                end);
    }

    /** Devuelve la fase que corresponde al instante real de la efemeride. */
    Phase phaseAt(double elapsedSeconds) {
        double elapsed = Math.max(startSeconds, Math.min(endSeconds, elapsedSeconds));
        if (elapsed < tliStartSeconds) {
            return Phase.ORBITA_TERRESTRE;
        }
        if (elapsed < tliEndSeconds) {
            return Phase.INYECCION_TRANSLUNAR;
        }
        if (elapsed < lunarEntrySeconds) {
            return Phase.COSTA_TRANSLUNAR;
        }
        if (elapsed <= lunarExitSeconds) {
            return Phase.SOBREVUELO_LUNAR;
        }
        if (elapsed < reentryStartSeconds) {
            return Phase.RETORNO_LIBRE;
        }
        return Phase.REINGRESO;
    }

    /** Hitos usados por MissionMap3D para marcar cambios de fase sin numeros ni texto. */
    List<Transition> transitions() {
        List<Transition> result = new ArrayList<>();
        result.add(new Transition(tliStartSeconds, Phase.INYECCION_TRANSLUNAR));
        result.add(new Transition(tliEndSeconds, Phase.COSTA_TRANSLUNAR));
        result.add(new Transition(lunarEntrySeconds, Phase.SOBREVUELO_LUNAR));
        result.add(new Transition(lunarExitSeconds, Phase.RETORNO_LIBRE));
        result.add(new Transition(reentryStartSeconds, Phase.REINGRESO));
        return List.copyOf(result);
    }

    double getTliCenterSeconds() {
        return tliCenterSeconds;
    }

    double getLunarEncounterSeconds() {
        return lunarEncounterSeconds;
    }

    double getEndSeconds() {
        return endSeconds;
    }

    private static int indexOfMinimumMoonDistance(List<MissionState> states) {
        int result = 0;
        for (int index = 1; index < states.size(); index++) {
            if (states.get(index).getDistanceMoon() < states.get(result).getDistanceMoon()) {
                result = index;
            }
        }
        return result;
    }

    /**
     * El ultimo perigeo terrestre antes de la salida sostenida coincide con la
     * maniobra TLI en la efemeride de referencia.
     */
    private static int indexOfDeparturePerigee(List<MissionState> states, int encounterIndex) {
        double encounterTime = states.get(encounterIndex).getElapsedTime();
        double searchEnd = Math.min(encounterTime * 0.45, 48.0 * 60.0 * 60.0);
        int result = 0;
        for (int index = 1; index < encounterIndex - 1; index++) {
            MissionState current = states.get(index);
            if (current.getElapsedTime() > searchEnd) {
                break;
            }
            double previousDistance = states.get(index - 1).getDistanceEarth();
            double currentDistance = current.getDistanceEarth();
            double nextDistance = states.get(index + 1).getDistanceEarth();
            if (currentDistance <= previousDistance
                    && currentDistance <= nextDistance
                    && currentDistance < 100_000.0) {
                result = index;
            }
        }
        if (result > 0) {
            return result;
        }

        int fallbackEnd = Math.max(1, Math.min(encounterIndex, states.size() / 3));
        for (int index = 1; index <= fallbackEnd; index++) {
            if (states.get(index).getDistanceEarth() < states.get(result).getDistanceEarth()) {
                result = index;
            }
        }
        return result;
    }

    private static int firstWithinMoonDistance(
            List<MissionState> states,
            int startIndex,
            int encounterIndex) {
        for (int index = Math.max(0, startIndex); index <= encounterIndex; index++) {
            if (states.get(index).getDistanceMoon() <= LUNAR_INFLUENCE_DISTANCE_KM) {
                return index;
            }
        }
        return encounterIndex;
    }

    private static int firstOutsideMoonDistance(List<MissionState> states, int encounterIndex) {
        for (int index = encounterIndex; index < states.size(); index++) {
            if (states.get(index).getDistanceMoon() > LUNAR_INFLUENCE_DISTANCE_KM) {
                return index;
            }
        }
        return states.size() - 1;
    }

    private static int firstReturnWithinEarthDistance(List<MissionState> states, int startIndex) {
        for (int index = Math.max(0, startIndex); index < states.size(); index++) {
            if (states.get(index).getDistanceEarth() <= REENTRY_APPROACH_DISTANCE_KM) {
                return index;
            }
        }
        return states.size() - 1;
    }

    /** Nombre, siguiente evento y color compartidos por controlador y mapa. */
    enum Phase {
        ORBITA_TERRESTRE("Orbita terrestre", "Inyeccion translunar", "#6DA8FF"),
        INYECCION_TRANSLUNAR("Inyeccion translunar", "Costa translunar", "#A46C93"),
        COSTA_TRANSLUNAR("Costa translunar", "Sobrevuelo lunar", "#A46CFF"),
        SOBREVUELO_LUNAR("Sobrevuelo lunar", "Retorno libre", "#C79CFF"),
        RETORNO_LIBRE("Retorno libre", "Reingreso", "#6DA8FF"),
        REINGRESO("Reingreso", "Completar mision", "#A46C93");

        private final String displayName;
        private final String nextEvent;
        private final String color;

        Phase(String displayName, String nextEvent, String color) {
            this.displayName = displayName;
            this.nextEvent = nextEvent;
            this.color = color;
        }

        String displayName() {
            return displayName;
        }

        String nextEvent() {
            return nextEvent;
        }

        String color() {
            return color;
        }
    }

    record Transition(double elapsedSeconds, Phase phase) {
    }
}
