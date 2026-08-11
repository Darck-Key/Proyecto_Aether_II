package com.example.demoaether;

import org.junit.jupiter.api.Test;
import org.orekit.orbits.Orbit;
import org.orekit.utils.Constants;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrbitFactoryTest {

    @Test
    void oam2CreatesCircularParkingOrbitNear185Km() {
        OrekitInitializer.initialize();
        MissionConfig config = new MissionConfig();

        Orbit orbit = OrbitFactory.createInitialOrbit(config);
        double altitudeKm = (orbit.getA() - Constants.WGS84_EARTH_EQUATORIAL_RADIUS) / 1000.0;

        assertEquals(185.0, altitudeKm, 1.0);
        assertEquals(0.0, orbit.getE(), 1.0e-9);
    }
}
