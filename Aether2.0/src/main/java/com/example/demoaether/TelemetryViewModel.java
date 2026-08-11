package com.example.demoaether;

import java.util.Locale;

/**
 * Modelo de telemetria listo para la interfaz.
 *
 * Quien llama:
 * - HelloController puede usarlo para transformar MissionState en texto visible.
 * - Las pruebas E5 lo validan sin abrir JavaFX.
 *
 * Que hace:
 * - Convierte valores numericos de MissionState en textos de tiempo, velocidad,
 *   altitud y distancia a la Luna.
 */
public class TelemetryViewModel {

    private final String elapsedTimeText;
    private final String velocityText;
    private final String altitudeText;
    private final String moonDistanceText;

    private TelemetryViewModel(String elapsedTimeText, String velocityText, String altitudeText, String moonDistanceText) {
        this.elapsedTimeText = elapsedTimeText;
        this.velocityText = velocityText;
        this.altitudeText = altitudeText;
        this.moonDistanceText = moonDistanceText;
    }

    /**
     * Crea un modelo de telemetria desde un estado orbital.
     *
     * @param state estado calculado por la fisica
     * @return textos listos para labels JavaFX
     */
    public static TelemetryViewModel fromState(MissionState state) {
        return new TelemetryViewModel(
                formatTime((int) state.getElapsedTime()),
                String.format(Locale.US, "%.2f km/s", state.getVelocity()),
                String.format(Locale.US, "%,.0f km", state.getAltitude()),
                String.format(Locale.US, "%,.0f km", state.getDistanceMoon())
        );
    }

    public String getElapsedTimeText() {
        return elapsedTimeText;
    }

    public String getVelocityText() {
        return velocityText;
    }

    public String getAltitudeText() {
        return altitudeText;
    }

    public String getMoonDistanceText() {
        return moonDistanceText;
    }

    static String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }
}
