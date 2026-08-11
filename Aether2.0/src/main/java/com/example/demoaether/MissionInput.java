package com.example.demoaether;

import java.util.Scanner;


public class MissionInput {


    public static MissionConfig createCustomMission() {


        try (Scanner scanner = new Scanner(System.in)) {
                MissionConfig config =
                        new MissionConfig();



                System.out.println();


                System.out.println("================================");
                System.out.println(" MISION PERSONALIZADA ");
                System.out.println("================================");



                System.out.print(
                        "Nombre de la mision: "
                );


                config.setMissionName(
                        scanner.nextLine()
                );



                System.out.print(
                        "Nombre de la nave: "
                );


                config.setSpacecraftName(
                        scanner.nextLine()
                );



                System.out.print(
                        "Masa de la nave (kg): "
                );


                config.setSpacecraftMass(
                        scanner.nextDouble()
                );



                System.out.print(
                        "Altitud inicial (km): "
                );


                config.setInitialAltitude(
                        scanner.nextDouble()
                );



                System.out.print(
                        "Velocidad inicial (km/s): "
                );


                config.setInitialVelocity(
                        scanner.nextDouble()
                );



                System.out.print(
                        "Horas de simulacion: "
                );


                config.setSimulationHours(
                        scanner.nextInt()
                );



                System.out.print(
                        "Paso de tiempo (segundos): "
                );


                config.setSimulationStepSeconds(
                        scanner.nextInt()
                );



                config.setSimulationSpeed(1);


                config.setSaveReports(true);



                System.out.println();


                System.out.println(
                        "Configuracion creada correctamente."
                );



                return config;
        }


    }


}