package com.example.demoaether;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Contrato unico de persistencia de AETHER.
 *
 * Quien llama:
 * - HelloController llama saveCalculation despues de calcular una orbita con Orekit.
 * - HelloController llama saveMissionEvent al iniciar, completar o fallar una simulacion.
 * - HelloController llama saveReport despues de generar el PDF.
 * - HelloController llama findRecentCalculations para incluir el historial en reportes.
 * - HelloController llama saveMissionConfig/loadLastMissionConfig para recuperar parametros.
 * - HelloController llama findRecentReports para abrir reportes ya guardados.
 *
 * Quien implementa:
 * - MySqlAetherRepository persiste en MySQL.
 * - PendingDatabaseRepository mantiene la app funcional cuando MySQL no esta configurado.
 */
public interface AetherRepository {

    void saveCalculation(MissionConfig config, MissionState state, LocalDateTime executedAt);

    void saveMissionEvent(String eventType, String description, LocalDateTime executedAt);

    void saveReport(File reportFile, MissionConfig config, MissionState state, LocalDateTime generatedAt);

    List<CalculationHistoryEntry> findRecentCalculations(int limit);

    List<MissionEventEntry> findRecentEvents(int limit);

    List<MissionReportEntry> findRecentReports(int limit);

    void saveMissionConfig(MissionConfig config, LocalDateTime savedAt);

    MissionConfig loadLastMissionConfig();

    boolean isAvailable();

    String describeStatus();
}
