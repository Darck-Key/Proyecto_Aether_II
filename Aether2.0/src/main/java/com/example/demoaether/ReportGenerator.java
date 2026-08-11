package com.example.demoaether;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generador de reportes PDF de AETHER.
 *
 * Quien llama:
 * - HelloController.generateReport() envia MissionConfig, MissionState, historial y usuario activo.
 *
 * Que hace:
 * - Crea la carpeta reportes si no existe.
 * - Arma las lineas del reporte con datos orbitales, usuario e historial.
 * - Escribe un PDF simple sin librerias externas para facilitar la entrega del proyecto.
 */
public class ReportGenerator {

    private ReportGenerator() {
    }

    public static File generatePdf(MissionConfig config, MissionState state, LocalDateTime generatedAt) throws IOException {
        return generatePdf(config, state, generatedAt, List.of());
    }

    public static File generatePdf(MissionConfig config, MissionState state, LocalDateTime generatedAt,
                                   List<CalculationHistoryEntry> history) throws IOException {
        return generatePdf(config, state, generatedAt, history, "operador");
    }

    public static File generatePdf(MissionConfig config, MissionState state, LocalDateTime generatedAt,
                                   List<CalculationHistoryEntry> history, String userName) throws IOException {
        // Punto principal de generacion: recibe datos de la UI/controlador y devuelve el archivo PDF creado.
        File dir = new File("reportes");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta de reportes.");
        }

        String stamp = generatedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        File file = new File(dir, "aether-reporte-" + stamp + ".pdf");
        List<String> lines = reportLines(config, state, generatedAt, history, userName);
        writeSimplePdf(file, lines);
        return file;
    }

    private static List<String> reportLines(MissionConfig config, MissionState state, LocalDateTime generatedAt,
                                            List<CalculationHistoryEntry> history, String userName) {
        // Construye el contenido textual del PDF: cabecera, usuario, parametros, resultado e historial.
        List<String> lines = new ArrayList<>();
        lines.add("PROYECTO AETHER - REPORTE ORBITAL");
        lines.add("Fecha y hora: " + generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        lines.add("Usuario operador: " + sanitizeUserName(userName));
        lines.add("Mision: " + config.getMissionName());
        lines.add("Nave: " + config.getSpacecraftName());
        lines.add("Altitud inicial: " + format(config.getInitialAltitude()) + " km");
        lines.add("Velocidad inicial: " + format(config.getInitialVelocity()) + " km/s");
        lines.add("Inclinacion: " + format(config.getInclination()) + " grados");
        lines.add("Excentricidad: " + format(config.getEccentricity()));
        lines.add("Argumento del perigeo: " + format(config.getArgumentOfPerigee()) + " grados");
        lines.add("Tiempo simulado: " + format(state.getElapsedTime()) + " s");
        lines.add("Velocidad calculada: " + format(state.getVelocity()) + " km/s");
        lines.add("Altitud calculada: " + format(state.getAltitude()) + " km");
        lines.add("Distancia a Tierra: " + format(state.getDistanceEarth()) + " km");
        lines.add("Distancia a Luna: " + format(state.getDistanceMoon()) + " km");
        lines.add("Posicion X/Y/Z: " + format(state.getX()) + " / " + format(state.getY()) + " / " + format(state.getZ()) + " km");
        lines.add("");
        lines.add("HISTORIAL RECIENTE DE CALCULOS");
        if (history == null || history.isEmpty()) {
            lines.add("No hay calculos anteriores guardados en MySQL.");
        } else {
            int count = Math.min(history.size(), 8);
            for (int i = 0; i < count; i++) {
                CalculationHistoryEntry entry = history.get(i);
                lines.add(historyLine(i + 1, entry));
            }
        }
        return lines;
    }

    private static String sanitizeUserName(String userName) {
        // Evita que el reporte salga sin operador cuando el login no envio un nombre valido.
        if (userName == null || userName.isBlank()) {
            return "operador";
        }
        return userName.trim();
    }

    private static String historyLine(int index, CalculationHistoryEntry entry) {
        // Formatea una fila corta del historial recuperado desde AetherRepository.
        String date = entry.getExecutedAt() == null
                ? "sin fecha"
                : entry.getExecutedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return index + ". " + date +
                " | " + entry.getMissionName() +
                " | v=" + format(entry.getVelocityKms()) + " km/s" +
                " | alt=" + format(entry.getAltitudeKm()) + " km" +
                " | luna=" + format(entry.getDistanceMoonKm()) + " km";
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static void writeSimplePdf(File file, List<String> lines) throws IOException {
        // Escribe manualmente la estructura PDF basica: catalogo, pagina, fuente y stream de texto.
        StringBuilder text = new StringBuilder();
        text.append("BT\n/F1 16 Tf\n50 760 Td\n");
        for (int i = 0; i < lines.size(); i++) {
            String size = i == 0 ? "/F1 16 Tf" : "/F1 11 Tf";
            text.append(size).append("\n(").append(escape(lines.get(i))).append(") Tj\n0 -24 Td\n");
        }
        text.append("ET");

        byte[] stream = text.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objects = new ArrayList<>();
        objects.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        objects.add(("5 0 obj\n<< /Length " + stream.length + " >>\nstream\n" + text + "\nendstream\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));

        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            int position = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1).length;
            for (byte[] object : objects) {
                offsets.add(position);
                out.write(object);
                position += object.length;
            }
            int xref = position;
            StringBuilder trailer = new StringBuilder();
            trailer.append("xref\n0 6\n0000000000 65535 f \n");
            for (int i = 1; i < offsets.size(); i++) {
                trailer.append(String.format(Locale.US, "%010d 00000 n \n", offsets.get(i)));
            }
            trailer.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
            out.write(trailer.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    private static String escape(String value) {
        String normalized = value
                .replace("ó", "o")
                .replace("Ó", "O")
                .replace("í", "i")
                .replace("Í", "I")
                .replace("á", "a")
                .replace("Á", "A")
                .replace("é", "e")
                .replace("É", "E")
                .replace("ú", "u")
                .replace("Ú", "U")
                .replace("ñ", "n")
                .replace("Ñ", "N");
        return normalized.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
