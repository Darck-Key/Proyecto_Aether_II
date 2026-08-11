package com.example.demoaether;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TrajectoryPlaybackTest {

    @Test
    void sampleAtInterpolatesAllTelemetryWithoutChangingEndpoints() {
        MissionState start = state(0.0, 0.0);
        MissionState end = state(10.0, 100.0);
        List<MissionState> states = List.of(start, end);

        MissionState middle = TrajectoryPlayback.sampleAt(states, 5.0);

        assertEquals(50.0, middle.getX(), 0.0001);
        assertEquals(55.0, middle.getDistanceMoon(), 0.0001);
        assertEquals(50.0, middle.getMoonX(), 0.0001);
        assertSame(start, TrajectoryPlayback.sampleAt(states, -1.0));
        assertSame(end, TrajectoryPlayback.sampleAt(states, 12.0));
    }

    private static MissionState state(double elapsed, double value) {
        return new MissionState(
                elapsed,
                value,
                value + 1.0,
                value + 2.0,
                value + 3.0,
                value + 4.0,
                value + 5.0,
                value + 6.0,
                value,
                value + 7.0,
                value + 8.0
        );
    }
}
