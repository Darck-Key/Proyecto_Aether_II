package com.example.demoaether;

import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitIllegalArgumentException;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrekitTrajectoryPlannerTest {

    @Test
    void oam3PropagatorIncludesRequiredForceModels() {
        List<String> forceModels = OrekitTrajectoryPlanner.describeForceModels(new MissionConfig());

        assertTrue(forceModels.contains("HolmesFeatherstoneAttractionModel"));
        assertTrue(forceModels.stream().filter("ThirdBodyAttraction"::equals).count() >= 2);
    }

    @Test
    void oam5TrajectoryHasAtLeast500ChronologicalSamples() {
        MissionConfig config = new MissionConfig();
        config.setSimulationHours(2);
        config.setTliBurnOffsetHours(0.5);

        MissionTrajectory trajectory = OrekitTrajectoryPlanner.precompute(config);

        assertTrue(trajectory.isOrekitBacked());
        assertTrue(trajectory.getStates().size() >= 500);
        assertChronological(trajectory.getStates());
    }

    @Test
    void oam6LunarPeriapsisEventIsRegistered() {
        MissionConfig config = new MissionConfig();
        config.setSimulationHours(2);
        config.setTliBurnOffsetHours(0.5);

        MissionTrajectory trajectory = OrekitTrajectoryPlanner.precompute(config);

        assertTrue(trajectory.getEvents().stream().anyMatch(event -> event.contains("Periapsis lunar")));
    }

    @Test
    void oam4ChangingTliDeltaVChangesTrajectory() {
        MissionConfig lowDeltaV = new MissionConfig();
        lowDeltaV.setSimulationHours(2);
        lowDeltaV.setTliBurnOffsetHours(0.5);
        lowDeltaV.setTliDeltaVKms(0.5);

        MissionConfig highDeltaV = new MissionConfig();
        highDeltaV.setSimulationHours(2);
        highDeltaV.setTliBurnOffsetHours(0.5);
        highDeltaV.setTliDeltaVKms(3.2);

        MissionState lowFinal = OrekitTrajectoryPlanner.precompute(lowDeltaV).lastState();
        MissionState highFinal = OrekitTrajectoryPlanner.precompute(highDeltaV).lastState();

        assertNotEquals(lowFinal.getX(), highFinal.getX(), 1.0);
        assertNotEquals(lowFinal.getVelocity(), highFinal.getVelocity(), 0.001);
    }

    @Test
    void oam7ReentryDetectorUses120KmInterface() {
        assertEquals(120.0, OrekitTrajectoryPlanner.reentryInterfaceAltitudeKm(), 0.001);
    }

    @Test
    void ui4ChangingInputParametersProducesDifferentTrajectory() {
        try {
            MissionConfig lowParkingOrbit = new MissionConfig();
            lowParkingOrbit.setSimulationHours(2);
            lowParkingOrbit.setTliBurnOffsetHours(0.5);
            lowParkingOrbit.setInitialAltitude(185.0);

            MissionConfig highParkingOrbit = new MissionConfig();
            highParkingOrbit.setSimulationHours(2);
            highParkingOrbit.setTliBurnOffsetHours(0.5);
            highParkingOrbit.setInitialAltitude(400.0);

            MissionState lowFinal = OrekitTrajectoryPlanner.precompute(lowParkingOrbit).lastState();
            MissionState highFinal = OrekitTrajectoryPlanner.precompute(highParkingOrbit).lastState();

            assertNotEquals(lowFinal.getX(), highFinal.getX(), 1.0);
            assertNotEquals(lowFinal.getAltitude(), highFinal.getAltitude(), 0.1);
        } catch (OrekitIllegalArgumentException exception) {
            throw new AssertionError("Orekit rechazo parametros: "
                    + exception.getSpecifier() + " " + Arrays.toString(exception.getParts()));
        }
    }

    private static void assertChronological(List<MissionState> states) {
        double previous = -1.0;
        for (MissionState state : states) {
            assertTrue(state.getElapsedTime() > previous);
            previous = state.getElapsedTime();
        }
    }
}
