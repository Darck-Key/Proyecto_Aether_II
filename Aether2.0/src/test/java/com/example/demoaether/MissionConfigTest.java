package com.example.demoaether;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MissionConfigTest {

    @Test
    void defaultConfigIsValid() {
        MissionConfig config = new MissionConfig();

        assertDoesNotThrow(config::validate);
    }

    @Test
    void defaultParkingOrbitMatchesE4Requirement() {
        MissionConfig config = new MissionConfig();

        assertEquals(185.0, config.getInitialAltitude(), 0.001);
        assertEquals(0.0, config.getEccentricity(), 0.001);
    }

    @Test
    void missingStoredConfigUsesArtemisPreset() {
        // Simula una PC nueva o MySQL desactivado, donde aun no existe una configuracion.
        MissionConfig config = assertDoesNotThrow(
                () -> MissionPresets.migrateLegacyArtemisII(null)
        );

        assertEquals("Artemis II", config.getMissionName());
        assertEquals("Orion", config.getSpacecraftName());
        assertEquals(240, config.getSimulationHours());
    }

    @Test
    void invalidAltitudeIsRejected() {
        MissionConfig config = new MissionConfig();
        config.setInitialAltitude(50);

        assertThrows(IllegalArgumentException.class, config::validate);
    }
}
