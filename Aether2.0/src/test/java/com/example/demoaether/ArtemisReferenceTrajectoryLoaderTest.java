package com.example.demoaether;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtemisReferenceTrajectoryLoaderTest {

    @Test
    void officialOemCoversOutboundFlybyReturnAndEntry() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        List<MissionState> states = trajectory.getStates();

        assertTrue(trajectory.isOrekitBacked());
        assertTrue(states.size() >= 3000);
        assertTrue(states.get(states.size() - 1).getElapsedTime() > 8.0 * 24.0 * 3600.0);
        assertTrue(states.stream().mapToDouble(MissionState::getDistanceEarth).max().orElseThrow() > 400000.0);
        assertTrue(states.stream().mapToDouble(MissionState::getDistanceMoon).min().orElseThrow() < 12000.0);
        assertTrue(states.get(states.size() - 1).getAltitude() < 180.0);
        assertTrue(states.get(states.size() - 1).getVelocity() > 10.0);
    }

    @Test
    void changingPlaybackSpeedDoesNotChangeReferenceTrajectory() {
        MissionConfig slow = MissionPresets.createArtemisII();
        slow.setSimulationSpeed(1);
        MissionConfig fast = MissionPresets.createArtemisII();
        fast.setSimulationSpeed(1000);

        assertTrue(ArtemisReferenceTrajectoryLoader.supports(slow));
        assertTrue(ArtemisReferenceTrajectoryLoader.supports(fast));
        assertSame(ArtemisReferenceTrajectoryLoader.load(), ArtemisReferenceTrajectoryLoader.load());
        assertEquals(
                ArtemisReferenceTrajectoryLoader.load().lastState().getX(),
                ArtemisReferenceTrajectoryLoader.load().lastState().getX(),
                0.0
        );
    }
}
