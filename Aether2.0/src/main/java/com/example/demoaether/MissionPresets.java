package com.example.demoaether;

import java.util.Locale;

/**
 * Misiones preconfiguradas.
 * Contiene valores reales o aproximados
 * para iniciar simulaciones.
 */
public class MissionPresets {


    public static MissionConfig createArtemisII() {


        MissionConfig config = new MissionConfig();



        // Datos de la misión

        config.setMissionName(
                "Artemis II"
        );


        config.setSpacecraftName(
                "Orion"
        );



        // Masa aproximada cápsula Orion

        config.setSpacecraftMass(
                26000
        );



        // Órbita inicial aproximada

        config.setInitialAltitude(
                185
        );



        config.setInitialVelocity(
                7.8
        );



        // Duración de simulación

        config.setSimulationHours(
                240
        );



        // Paso base para calculo y reportes; la animacion interpola entre puntos.

        config.setSimulationStepSeconds(
                60
        );



        config.setSimulationSpeed(
                1000
        );



        config.setSaveReports(
                true
        );



        return config;

    }

    /**
     * Reconoce el perfil nominal incluido sin mirar paso ni velocidad.
     * Quien llama: ArtemisReferenceTrajectoryLoader para elegir el OEM oficial.
     */
    public static boolean isArtemisIIReference(MissionConfig config) {
        if (config == null) {
            return false;
        }
        String mission = normalize(config.getMissionName());
        String spacecraft = normalize(config.getSpacecraftName());
        return (mission.equals("artemis ii") || mission.equals("artemis 2"))
                && spacecraft.equals("orion")
                && near(config.getSpacecraftMass(), 26000.0, 1500.0)
                && near(config.getInitialAltitude(), 185.0, 5.0)
                && near(config.getInitialVelocity(), 7.8, 0.25)
                && near(config.getInclination(), 28.5, 0.5)
                && near(config.getEccentricity(), 0.0, 0.01)
                && near(config.getArgumentOfPerigee(), 0.0, 0.5)
                && near(config.getTliDeltaVKms(), 3.2, 0.15)
                && near(config.getTliBurnOffsetHours(), 23.5, 1.0)
                && config.getSimulationHours() >= 214;
    }

    /**
     * Migra solo los perfiles antiguos creados por versiones anteriores.
     * Quien llama: HelloController.initialize() despues de leer MySQL.
     */
    public static MissionConfig migrateLegacyArtemisII(MissionConfig loaded) {
        // Una instalacion nueva no tiene configuracion guardada: usa el perfil nominal.
        if (loaded == null) {
            return createArtemisII();
        }
        if (!isLegacyArtemisII(loaded)) {
            return loaded;
        }
        MissionConfig migrated = createArtemisII();
        migrated.setMissionName(loaded.getMissionName());
        migrated.setSpacecraftName(loaded.getSpacecraftName());
        migrated.setSpacecraftMass(loaded.getSpacecraftMass());
        migrated.setSimulationSpeed(Math.max(1, Math.min(1000, loaded.getSimulationSpeed())));
        migrated.setSaveReports(loaded.isSaveReports());
        return migrated;
    }

    private static boolean isLegacyArtemisII(MissionConfig config) {
        if (config == null) {
            return false;
        }
        String mission = normalize(config.getMissionName());
        String spacecraft = normalize(config.getSpacecraftName());
        boolean correctNames = (mission.equals("artemis ii") || mission.equals("artemis 2"))
                && spacecraft.equals("orion");
        boolean oldDuration = config.getSimulationHours() == 1
                || config.getSimulationHours() == 10
                || config.getSimulationHours() == 72;
        boolean oldVelocity = near(config.getInitialVelocity(), 10.8, 0.05);
        boolean oldBurn = near(config.getTliBurnOffsetHours(), 0.15, 0.02)
                || near(config.getTliBurnOffsetHours(), 1.5, 0.05);
        return correctNames && (oldDuration || oldVelocity || oldBurn);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean near(double actual, double expected, double tolerance) {
        return Math.abs(actual - expected) <= tolerance;
    }


}
