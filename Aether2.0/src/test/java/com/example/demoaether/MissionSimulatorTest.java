package com.example.demoaether;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionSimulatorTest {

    @Test
    void calculatesInitialStateWithOrekit() {
        MissionConfig config = new MissionConfig();

        MissionState state = MissionSimulator.calculateInitialState(config);

        assertTrue(state.getAltitude() > 0);
        assertTrue(state.getVelocity() > 0);
        assertTrue(state.getDistanceMoon() > 0);
    }
}
