package com.example.demoaether;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servicio de conectividad para la barra superior.
 *
 * Quien llama:
 * - HelloController.startNetworkMonitor() consulta readQualityLabel() periodicamente.
 *
 * Que hace:
 * - En Windows intenta leer porcentaje WiFi con netsh.
 * - Si no hay dato WiFi, verifica si existe una interfaz de red activa.
 */
public class NetworkQualityService {

    private NetworkQualityService() {
    }

    public static String readQualityLabel() {
        // Devuelve texto listo para la UI: EXCELENTE/BUENA/MEDIA/BAJA, RED ACTIVA o SIN RED.
        Integer wifiSignal = readWindowsWifiSignal();
        if (wifiSignal != null) {
            return qualityName(wifiSignal) + " " + wifiSignal + " %";
        }

        if (hasActiveNetworkInterface()) {
            return "RED ACTIVA";
        }

        return "SIN RED";
    }

    private static Integer readWindowsWifiSignal() {
        // Llama al comando de Windows netsh y extrae el porcentaje de senal WiFi.
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return null;
        }

        try {
            Process process = new ProcessBuilder("netsh", "wlan", "show", "interfaces")
                    .redirectErrorStream(true)
                    .start();

            Pattern signalPattern = Pattern.compile("(?i)(signal|se.al)\\s*:\\s*(\\d+)\\s*%");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = signalPattern.matcher(line);
                    if (matcher.find()) {
                        return Integer.parseInt(matcher.group(2));
                    }
                }
            }
        } catch (IOException | NumberFormatException ignored) {
            return null;
        }

        return null;
    }

    private static boolean hasActiveNetworkInterface() {
        // Fallback multiplataforma cuando no se puede leer senal WiFi.
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!networkInterface.isLoopback() && networkInterface.isUp() && !networkInterface.isVirtual()) {
                    return true;
                }
            }
        } catch (IOException ignored) {
            return false;
        }
        return false;
    }

    private static String qualityName(int signal) {
        // Convierte porcentaje numerico en etiqueta comprensible para la barra superior.
        if (signal >= 80) {
            return "EXCELENTE";
        }
        if (signal >= 60) {
            return "BUENA";
        }
        if (signal >= 40) {
            return "MEDIA";
        }
        return "BAJA";
    }
}
