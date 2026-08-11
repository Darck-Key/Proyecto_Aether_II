package com.example.demoaether;

import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;

/**
 * Fabrica de orbitas iniciales para Orekit.
 *
 * Quien llama:
 * - OrekitTrajectoryPlanner.precompute().
 *
 * Que hace:
 * - Valida MissionConfig.
 * - Convierte kilometros/grados de la UI a metros/radianes para Orekit.
 * - Crea una KeplerianOrbit en el frame EME2000.
 */
public class OrbitFactory {

    /**
     * Crea la orbita inicial de estacionamiento en EME2000.
     *
     * @param config parametros capturados en Opciones
     * @return orbita Kepleriana inicial para Orekit
     */
    public static Orbit createInitialOrbit(MissionConfig config) {
        // Punto unico donde los parametros de la interfaz se transforman en orbita inicial Orekit.
        config.validate();

        Frame frame = FramesFactory.getEME2000();
        AbsoluteDate date = new AbsoluteDate(2026, 7, 20, 12, 0, 0, TimeScalesFactory.getUTC());
        double earthRadius = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double perigee = earthRadius + (config.getInitialAltitude() * 1000);
        double eccentricity = config.getEccentricity();
        double semiMajorAxis = perigee / (1.0 - eccentricity);
        double inclination = Math.toRadians(config.getInclination());
        double argumentOfPerigee = Math.toRadians(config.getArgumentOfPerigee());
        double rightAscension = 0.0;
        double trueAnomaly = 0.0;

        return new KeplerianOrbit(
                semiMajorAxis,
                eccentricity,
                inclination,
                argumentOfPerigee,
                rightAscension,
                trueAnomaly,
                PositionAngleType.TRUE,
                frame,
                date,
                Constants.WGS84_EARTH_MU
        );
    }
}
