package com.example.demoaether;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MissionConfigValidationTest {

    @ParameterizedTest
    @ValueSource(doubles = {160.0, 185.0, 2000.0})
    void ui3AcceptsValidParkingAltitudes(double altitude) {
        MissionConfig config = new MissionConfig();
        config.setInitialAltitude(altitude);

        assertDoesNotThrow(config::validate);
    }

    @ParameterizedTest
    @ValueSource(doubles = {159.9, 2000.1})
    void ui3RejectsInvalidParkingAltitudes(double altitude) {
        MissionConfig config = new MissionConfig();
        config.setInitialAltitude(altitude);

        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 1.5, 5.0})
    void ui3AcceptsValidTliDeltaV(double deltaV) {
        MissionConfig config = new MissionConfig();
        config.setTliDeltaVKms(deltaV);

        assertDoesNotThrow(config::validate);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 5.1})
    void ui3RejectsInvalidTliDeltaV(double deltaV) {
        MissionConfig config = new MissionConfig();
        config.setTliDeltaVKms(deltaV);

        assertThrows(IllegalArgumentException.class, config::validate);
    }
}
