package com.example.demoaether;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repositorio de respaldo.
 * Se usa automaticamente cuando MySQL no esta configurado, no tiene driver o no responde.
 */
public class PendingDatabaseRepository implements AetherRepository {

    private final String reason;
    private final List<CalculationHistoryEntry> calculations = new ArrayList<>();
    private final List<MissionEventEntry> events = new ArrayList<>();
    private final List<MissionReportEntry> reports = new ArrayList<>();
    private MissionConfig lastConfig;
    private long nextCalculationId = 1;
    private long nextEventId = 1;
    private long nextReportId = 1;

    public PendingDatabaseRepository() {
        this("MySQL pendiente");
    }

    public PendingDatabaseRepository(String reason) {
        this.reason = reason;
    }

    @Override
    public void saveCalculation(MissionConfig config, MissionState state, LocalDateTime executedAt) {
        calculations.add(0, new CalculationHistoryEntry(
                nextCalculationId++,
                config.getMissionName(),
                config.getSpacecraftName(),
                state.getElapsedTime(),
                state.getVelocity(),
                state.getAltitude(),
                state.getDistanceMoon(),
                executedAt
        ));
        System.out.println("[DB pendiente] Calculo orbital guardado en memoria. Motivo: " + reason);
    }

    @Override
    public void saveMissionEvent(String eventType, String description, LocalDateTime executedAt) {
        events.add(0, new MissionEventEntry(nextEventId++, eventType, description, executedAt));
        System.out.println("[DB pendiente] Evento " + eventType + " guardado en memoria. Motivo: " + reason);
    }

    @Override
    public void saveReport(File reportFile, MissionConfig config, MissionState state, LocalDateTime generatedAt) {
        reports.add(0, new MissionReportEntry(
                nextReportId++,
                reportFile.getName(),
                reportFile.getAbsolutePath(),
                config.getMissionName(),
                generatedAt
        ));
        System.out.println("[DB pendiente] Reporte guardado en memoria: " + reportFile.getAbsolutePath());
    }

    @Override
    public List<CalculationHistoryEntry> findRecentCalculations(int limit) {
        return recent(calculations, limit);
    }

    @Override
    public List<MissionEventEntry> findRecentEvents(int limit) {
        return recent(events, limit);
    }

    @Override
    public List<MissionReportEntry> findRecentReports(int limit) {
        return recent(reports, limit);
    }

    @Override
    public void saveMissionConfig(MissionConfig config, LocalDateTime savedAt) {
        lastConfig = config;
        System.out.println("[DB pendiente] Configuracion de mision guardada en memoria. Motivo: " + reason);
    }

    @Override
    public MissionConfig loadLastMissionConfig() {
        return lastConfig;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String describeStatus() {
        return "MySQL: pendiente / historial local";
    }

    private static <T> List<T> recent(List<T> entries, int limit) {
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(entries.subList(0, Math.min(entries.size(), Math.max(1, limit))));
    }
}
