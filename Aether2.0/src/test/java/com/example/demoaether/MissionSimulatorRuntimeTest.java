package com.example.demoaether;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionSimulatorRuntimeTest {

    @Test
    void simulationEmitsMultipleStatesQuickly() throws Exception {
        MissionConfig config = new MissionConfig();
        config.setMissionName("Prueba personalizada");
        config.setSimulationHours(1);
        config.setTliBurnOffsetHours(0.5);
        config.setSimulationStepSeconds(60);

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger errors = new AtomicInteger();
        MissionSimulator simulator = new MissionSimulator(config);
        simulator.setSimulationListener(new SimulationListener() {
            @Override
            public void onSimulationStarted() {
            }

            @Override
            public void onStateUpdated(MissionState state) {
                latch.countDown();
                if (latch.getCount() == 0) {
                    simulator.stopSimulation();
                }
            }

            @Override
            public void onSimulationFinished() {
            }

            @Override
            public void onSimulationError(Exception exception) {
                errors.incrementAndGet();
            }
        });

        Thread thread = new Thread(simulator::startSimulation);
        thread.setDaemon(true);
        thread.start();

        assertTrue(latch.await(8, TimeUnit.SECONDS), "La simulacion no emitio estados a tiempo.");
        assertTrue(errors.get() == 0, "La simulacion reporto errores.");
    }

    @Test
    void integrationPipelineProducesStatesConsumedByListener() throws Exception {
        MissionConfig config = new MissionConfig();
        config.setMissionName("Prueba personalizada");
        config.setSimulationHours(1);
        config.setTliBurnOffsetHours(0.5);
        config.setSimulationStepSeconds(60);

        CountDownLatch latch = new CountDownLatch(3);
        AtomicInteger validTelemetryStates = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        MissionSimulator simulator = new MissionSimulator(config);
        simulator.setSimulationListener(new SimulationListener() {
            @Override
            public void onSimulationStarted() {
            }

            @Override
            public void onStateUpdated(MissionState state) {
                TelemetryViewModel telemetry = TelemetryViewModel.fromState(state);
                if (!telemetry.getVelocityText().isBlank()
                        && state.getDistanceMoon() > 0
                        && state.getAltitude() > -500) {
                    validTelemetryStates.incrementAndGet();
                }
                latch.countDown();
                if (latch.getCount() == 0) {
                    simulator.stopSimulation();
                }
            }

            @Override
            public void onSimulationFinished() {
            }

            @Override
            public void onSimulationError(Exception exception) {
                errors.incrementAndGet();
            }
        });

        Thread thread = new Thread(simulator::startSimulation);
        thread.setDaemon(true);
        thread.start();

        assertTrue(latch.await(8, TimeUnit.SECONDS), "La integracion no entrego estados al listener.");
        assertTrue(validTelemetryStates.get() >= 3, "La UI no pudo consumir la telemetria del propagador.");
        assertTrue(errors.get() == 0, "La integracion reporto errores.");
    }
}
