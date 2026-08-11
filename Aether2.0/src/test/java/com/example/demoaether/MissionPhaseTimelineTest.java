package com.example.demoaether;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionPhaseTimelineTest {

    @Test
    void officialEphemerisProducesTheRealMissionSequence() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        MissionPhaseTimeline timeline = MissionPhaseTimeline.from(trajectory);

        assertEquals(
                MissionPhaseTimeline.Phase.ORBITA_TERRESTRE,
                timeline.phaseAt(trajectory.getStates().get(0).getElapsedTime()));
        assertEquals(
                MissionPhaseTimeline.Phase.INYECCION_TRANSLUNAR,
                timeline.phaseAt(timeline.getTliCenterSeconds()));
        assertEquals(
                MissionPhaseTimeline.Phase.SOBREVUELO_LUNAR,
                timeline.phaseAt(timeline.getLunarEncounterSeconds()));
        assertEquals(
                MissionPhaseTimeline.Phase.REINGRESO,
                timeline.phaseAt(timeline.getEndSeconds()));

        assertTrue(timeline.getTliCenterSeconds() > 20.0 * 3600.0);
        assertTrue(timeline.getTliCenterSeconds() < 24.0 * 3600.0);
        assertTrue(timeline.getLunarEncounterSeconds() > 115.0 * 3600.0);
        assertTrue(timeline.getLunarEncounterSeconds() < 120.0 * 3600.0);
        assertTrue(timeline.getEndSeconds() > 8.0 * 24.0 * 3600.0);
    }

    @Test
    void transitionsRemainChronologicalAndInsideTheTrajectory() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        MissionPhaseTimeline timeline = MissionPhaseTimeline.from(trajectory);
        List<MissionPhaseTimeline.Transition> transitions = timeline.transitions();

        assertEquals(5, transitions.size());
        for (int index = 1; index < transitions.size(); index++) {
            assertTrue(transitions.get(index).elapsedSeconds()
                    >= transitions.get(index - 1).elapsedSeconds());
        }
        assertTrue(transitions.get(0).elapsedSeconds() >= 0.0);
        assertTrue(transitions.get(transitions.size() - 1).elapsedSeconds()
                <= trajectory.getDurationSeconds());
    }
}
