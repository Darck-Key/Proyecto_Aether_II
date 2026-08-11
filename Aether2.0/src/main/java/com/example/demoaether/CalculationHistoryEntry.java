package com.example.demoaether;

import java.time.LocalDateTime;

/**
 * Fila de historial orbital.
 *
 * Quien la crea:
 * - MySqlAetherRepository al leer orbital_calculations.
 * - PendingDatabaseRepository cuando MySQL no esta disponible.
 *
 * Quien la usa:
 * - HelloController.createHistoryTable().
 * - ReportGenerator para incluir historial reciente en PDF.
 */
public class CalculationHistoryEntry {

    private final long id;
    private final String missionName;
    private final String spacecraftName;
    private final double elapsedSeconds;
    private final double velocityKms;
    private final double altitudeKm;
    private final double distanceMoonKm;
    private final LocalDateTime executedAt;

    public CalculationHistoryEntry(long id, String missionName, String spacecraftName, double elapsedSeconds,
                                   double velocityKms, double altitudeKm, double distanceMoonKm,
                                   LocalDateTime executedAt) {
        // Representa una fila ya lista para tablas y reportes.
        this.id = id;
        this.missionName = missionName;
        this.spacecraftName = spacecraftName;
        this.elapsedSeconds = elapsedSeconds;
        this.velocityKms = velocityKms;
        this.altitudeKm = altitudeKm;
        this.distanceMoonKm = distanceMoonKm;
        this.executedAt = executedAt;
    }

    public long getId() {
        return id;
    }

    public String getMissionName() {
        return missionName;
    }

    public String getSpacecraftName() {
        return spacecraftName;
    }

    public double getElapsedSeconds() {
        return elapsedSeconds;
    }

    public double getVelocityKms() {
        return velocityKms;
    }

    public double getAltitudeKm() {
        return altitudeKm;
    }

    public double getDistanceMoonKm() {
        return distanceMoonKm;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
}
