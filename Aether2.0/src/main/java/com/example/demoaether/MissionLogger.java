package com.example.demoaether;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Exportador CSV de telemetria.
 *
 * Quien llama:
 * - MissionSimulator.prepareTrajectory() crea MissionLogger y llama logState() por cada estado fuente.
 *
 * Que hace:
 * - Escribe mission-data.csv con tiempo, posicion, velocidad, distancia a Tierra/Luna y altitud.
 */
public class MissionLogger {

    private PrintWriter writer;

    public MissionLogger(String fileName) {
        // Abre o crea el CSV y escribe la fila de encabezados.

        try {

            File file = new File(fileName);

            System.out.println("======================================");
            System.out.println("Creando archivo CSV...");
            System.out.println("Ruta: " + file.getAbsolutePath());
            System.out.println("======================================");

            writer = new PrintWriter(new FileWriter(file));

            writer.println(
                    "Tiempo(s),X(km),Y(km),Z(km),Velocidad(km/s),DistanciaTierra(km),DistanciaLuna(km),Altitud(km)"
            );

        } catch (IOException e) {


            throw new RuntimeException(
                    "Error creando el archivo CSV.",
                    e
            );

        }

    }

    public void logState(MissionState state) {
        // Agrega una fila por cada MissionState recibido desde MissionSimulator.

        if (writer == null) {
            return;
        }

        writer.printf(
                "%.0f,%.3f,%.3f,%.3f,%.4f,%.3f,%.3f,%.3f%n",
                state.getElapsedTime(),
                state.getX(),
                state.getY(),
                state.getZ(),
                state.getVelocity(),
                state.getDistanceEarth(),
                state.getDistanceMoon(),
                state.getAltitude()
        );

    }

    public void close() {
        // Cierra el archivo para asegurar que todos los datos queden escritos en disco.

        if (writer != null) {

            writer.flush();
            writer.close();

            System.out.println("Archivo CSV guardado correctamente.");

        }

    }

}
