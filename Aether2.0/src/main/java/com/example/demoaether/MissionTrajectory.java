package com.example.demoaether;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Resultado precalculado de una simulacion orbital.
 *
 * Quien lo crea:
 * - OrekitTrajectoryPlanner calcula la lista de puntos usando Orekit.
 *
 * Quien lo consume:
 * - TrajectoryPlayback interpola estos estados en el hilo JavaFX.
 * - HelloController recibe cada MissionState y actualiza la interfaz.
 */
public class MissionTrajectory {

    private final List<MissionState> states;
    private final List<String> events;
    private final boolean orekitBacked;

    public MissionTrajectory(List<MissionState> states, List<String> events, boolean orekitBacked) {
        // Copias inmutables: MissionSimulator puede leer la trayectoria sin que otra clase la modifique.
        this.states = Collections.unmodifiableList(new ArrayList<>(states));
        this.events = Collections.unmodifiableList(new ArrayList<>(events));
        this.orekitBacked = orekitBacked;
    }

    public List<MissionState> getStates() {
        return states;
    }

    public List<String> getEvents() {
        return events;
    }

    public boolean isOrekitBacked() {
        return orekitBacked;
    }

    public MissionState lastState() {
        // Acceso rapido al resultado final usado por reportes o guardado de cierre.
        return states.isEmpty() ? null : states.get(states.size() - 1);
    }

    /** Devuelve el tiempo de mision del ultimo estado, no la duracion de pared. */
    public double getDurationSeconds() {
        MissionState last = lastState();
        return last == null ? 0.0 : last.getElapsedTime();
    }
}
