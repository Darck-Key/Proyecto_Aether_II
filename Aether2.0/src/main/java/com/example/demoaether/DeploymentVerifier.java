package com.example.demoaether;

import java.net.URL;

/**
 * Verificacion automatica del paquete de despliegue.
 *
 * Quien llama:
 * - Launcher.main() cuando recibe --verificar-despliegue.
 *
 * A quien llama:
 * - OrekitInitializer para comprobar el ZIP de datos.
 * - ClassLoader para comprobar JavaFX, MySQL, FXML y CSS dentro del JAR.
 *
 * Que hace:
 * - Falla con un mensaje claro si falta una pieza indispensable del paquete.
 * - Termina con codigo 0 e imprime DESPLIEGUE AETHER: OK cuando todo esta disponible.
 */
public final class DeploymentVerifier {

    private DeploymentVerifier() {
        // Clase utilitaria: no necesita instancias.
    }

    /** Ejecuta las comprobaciones que no necesitan mostrar la interfaz grafica. */
    public static void verify() {
        try {
            // Confirma que las clases de las dependencias fueron incluidas en el JAR ejecutable.
            Class.forName("javafx.application.Application");
            Class.forName("com.mysql.cj.jdbc.Driver");

            requireResource("/com/example/demoaether/hello-view.fxml");
            requireResource("/com/example/demoaether/aether.css");

            // Reproduce una primera instalacion, donde MySQL aun no tiene configuracion guardada.
            MissionConfig initialConfig = MissionPresets.migrateLegacyArtemisII(null);
            initialConfig.validate();

            // Inicializa Orekit para comprobar que orekit-data.zip es legible y contiene UTC.
            OrekitInitializer.initialize();

            System.out.println("DESPLIEGUE AETHER: OK");
            System.out.println("JAR, JavaFX, MySQL, configuracion inicial, FXML, CSS y datos Orekit disponibles.");
        } catch (Exception error) {
            throw new IllegalStateException("DESPLIEGUE AETHER: ERROR - " + error.getMessage(), error);
        }
    }

    private static void requireResource(String resourcePath) {
        URL resource = DeploymentVerifier.class.getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Falta el recurso " + resourcePath + " dentro del JAR.");
        }
    }
}
