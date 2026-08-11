package com.example.demoaether;

/**
 * Interfaz para notificar cada actualización de la simulación.
 */
public interface SimulationListener {

    /**
     * Avisa que MissionSimulator inicio el precalculo de la trayectoria.
     */
    void onSimulationStarted();

    /**
     * Entrega cada estado calculado para pintar telemetria, mapa y progreso.
     *
     * @param state estado orbital actual
     */
    void onStateUpdated(MissionState state);

    /**
     * Avisa que la trayectoria termino y puede generarse un reporte final.
     */
    void onSimulationFinished();

    /**
     * Entrega errores de simulacion para mostrarlos y registrarlos.
     *
     * @param exception excepcion producida por el motor orbital o el logger
     */
    void onSimulationError(Exception exception);

}
