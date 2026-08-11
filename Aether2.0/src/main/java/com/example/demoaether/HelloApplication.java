package com.example.demoaether;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Aplicacion JavaFX principal.
 *
 * Quien la llama:
 * - Launcher.main() la lanza para Gradle/IntelliJ.
 *
 * Que hace:
 * - Carga hello-view.fxml.
 * - Crea la Scene principal.
 * - Muestra la ventana del simulador.
 */
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // FXMLLoader crea la interfaz y conecta fx:id/eventos con HelloController.
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1400, 800);;
        stage.setTitle("Simulador Artemis II");
        stage.setScene(scene);
        stage.show();
    }
}
