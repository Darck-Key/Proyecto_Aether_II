package com.example.demoaether;

import java.time.LocalDateTime;

/**
 * Fila de reporte guardado.
 *
 * Quien la crea:
 * - MySqlAetherRepository al leer mission_reports.
 * - PendingDatabaseRepository cuando MySQL no esta disponible.
 *
 * Quien la usa:
 * - HelloController.showSavedReports() para listar y abrir PDFs generados.
 */
public class MissionReportEntry {

    private final long id;
    private final String fileName;
    private final String filePath;
    private final String missionName;
    private final LocalDateTime generatedAt;

    public MissionReportEntry(long id, String fileName, String filePath, String missionName, LocalDateTime generatedAt) {
        // Guarda referencia al PDF fisico para poder abrirlo desde la interfaz.
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.missionName = missionName;
        this.generatedAt = generatedAt;
    }

    public long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMissionName() {
        return missionName;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
}
