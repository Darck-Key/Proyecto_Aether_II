package com.example.demoaether;

import javafx.application.Application;

/**
 * Entrada compatible con Gradle e IntelliJ.
 *
 * Quien llama:
 * - La tarea run de Gradle o la configuracion de ejecucion de IntelliJ.
 *
 * A quien llama:
 * - HelloApplication, que carga el FXML y muestra JavaFX.
 */
public class Launcher {
    public static void main(String[] args) {
        // Permite validar el paquete sin abrir ventanas; lo usan las pruebas de despliegue.
        if (args.length == 1 && "--verificar-despliegue".equals(args[0])) {
            DeploymentVerifier.verify();
            return;
        }

        // Mantiene main() separado de Application para evitar problemas de lanzamiento modular.
        Application.launch(HelloApplication.class, args);
    }
}
