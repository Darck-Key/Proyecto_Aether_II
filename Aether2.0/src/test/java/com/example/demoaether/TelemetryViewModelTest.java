package com.example.demoaether;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryViewModelTest {

    @Test
    void ui2FormatsTelemetryFromPhysicsStateUsingMockito() {
        MissionState state = mock(MissionState.class);
        when(state.getElapsedTime()).thenReturn(3723.0);
        when(state.getVelocity()).thenReturn(10.8123);
        when(state.getAltitude()).thenReturn(185.4);
        when(state.getDistanceMoon()).thenReturn(377_729.2);

        TelemetryViewModel telemetry = TelemetryViewModel.fromState(state);

        assertEquals("01:02:03", telemetry.getElapsedTimeText());
        assertEquals("10.81 km/s", telemetry.getVelocityText());
        assertEquals("185 km", telemetry.getAltitudeText());
        assertEquals("377,729 km", telemetry.getMoonDistanceText());
    }

    @Test
    void ui2FormatsTimeBoundaries() {
        assertEquals("00:00:00", TelemetryViewModel.formatTime(0));
        assertEquals("10:00:00", TelemetryViewModel.formatTime(36_000));
    }
}
