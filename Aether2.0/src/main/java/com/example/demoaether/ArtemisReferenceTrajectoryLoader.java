package com.example.demoaether;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.odm.oem.Oem;
import org.orekit.files.ccsds.ndm.odm.oem.OemSegment;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapta la efemeride oficial Artemis II al modelo interno de AETHER.
 *
 * <p>Quien la llama: MissionSimulator cuando MissionConfig conserva el perfil
 * Artemis II de referencia. A quien llama: OemParser de Orekit para leer el
 * archivo CCSDS OEM y CelestialBodyFactory para calcular la posicion lunar.
 * El resultado contiene los mismos puntos para el mapa, la telemetria, la
 * reproduccion normal y la demo rapida.</p>
 */
public final class ArtemisReferenceTrajectoryLoader {

    static final String RESOURCE_PATH = "/com/example/demoaether/data/artemis-ii-flight-oem.asc";
    private static final double EARTH_RADIUS_M = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
    private static volatile MissionTrajectory cachedTrajectory;

    private ArtemisReferenceTrajectoryLoader() {
    }

    /**
     * Decide si la configuracion representa el perfil nominal incluido.
     * La escala de tiempo no participa: cambiar de 1x a 1000x nunca cambia la fisica.
     */
    public static boolean supports(MissionConfig config) {
        return MissionPresets.isArtemisIIReference(config);
    }

    /**
     * Lee una sola vez el OEM incluido y devuelve mas de 3,000 estados reales.
     *
     * @return trayectoria oficial post-separacion ICPS hasta interfaz de entrada
     */
    public static MissionTrajectory load() {
        MissionTrajectory result = cachedTrajectory;
        if (result != null) {
            return result;
        }

        synchronized (ArtemisReferenceTrajectoryLoader.class) {
            if (cachedTrajectory == null) {
                cachedTrajectory = parseResource();
            }
            return cachedTrajectory;
        }
    }

    private static MissionTrajectory parseResource() {
        OrekitInitializer.initialize();
        DataSource source = new DataSource("artemis-ii-flight-oem.asc", () -> openResource());
        Oem oem = new ParserBuilder()
                .withMu(Constants.WGS84_EARTH_MU)
                .buildOemParser()
                .parse(source);

        List<MissionState> states = new ArrayList<>();
        AbsoluteDate start = null;
        for (OemSegment segment : oem.getSegments()) {
            Frame frame = segment.getFrame();
            for (TimeStampedPVCoordinates coordinates : segment.getCoordinates()) {
                if (start == null) {
                    start = coordinates.getDate();
                }
                if (!states.isEmpty()
                        && coordinates.getDate().durationFrom(start)
                        <= states.get(states.size() - 1).getElapsedTime()) {
                    continue;
                }
                states.add(toMissionState(coordinates, frame, start));
            }
        }

        if (states.size() < 500) {
            throw new IllegalStateException("La efemeride Artemis II no contiene suficientes estados.");
        }

        List<String> events = describeReferenceEvents(states);
        return new MissionTrajectory(states, events, true);
    }

    private static InputStream openResource() throws IOException {
        InputStream stream = ArtemisReferenceTrajectoryLoader.class.getResourceAsStream(RESOURCE_PATH);
        if (stream == null) {
            throw new IOException("No se encontro el recurso " + RESOURCE_PATH);
        }
        return stream;
    }

    private static MissionState toMissionState(
            TimeStampedPVCoordinates spacecraft,
            Frame frame,
            AbsoluteDate start) {
        CelestialBody moon = CelestialBodyFactory.getMoon();
        Vector3D moonPosition = moon.getPVCoordinates(spacecraft.getDate(), frame).getPosition();
        Vector3D position = spacecraft.getPosition();
        double distanceEarthKm = position.getNorm() / 1000.0;
        double distanceMoonKm = Vector3D.distance(position, moonPosition) / 1000.0;

        return new MissionState(
                spacecraft.getDate().durationFrom(start),
                position.getX() / 1000.0,
                position.getY() / 1000.0,
                position.getZ() / 1000.0,
                spacecraft.getVelocity().getNorm() / 1000.0,
                distanceEarthKm,
                distanceMoonKm,
                distanceEarthKm - EARTH_RADIUS_M / 1000.0,
                moonPosition.getX() / 1000.0,
                moonPosition.getY() / 1000.0,
                moonPosition.getZ() / 1000.0
        );
    }

    private static List<String> describeReferenceEvents(List<MissionState> states) {
        MissionState closestMoon = states.get(0);
        MissionState farthestEarth = states.get(0);
        for (MissionState state : states) {
            if (state.getDistanceMoon() < closestMoon.getDistanceMoon()) {
                closestMoon = state;
            }
            if (state.getDistanceEarth() > farthestEarth.getDistanceEarth()) {
                farthestEarth = state;
            }
        }

        MissionState finalState = states.get(states.size() - 1);
        List<String> events = new ArrayList<>();
        events.add("Fuente: NASA/JSC/FOD/FDO - OEM post-ICPS hasta entrada");
        events.add(String.format(Locale.US,
                "Maxima distancia terrestre: %.0f km a T+%.0f s",
                farthestEarth.getDistanceEarth(), farthestEarth.getElapsedTime()));
        events.add(String.format(Locale.US,
                "Periapsis lunar: %.0f km del centro lunar a T+%.0f s",
                closestMoon.getDistanceMoon(), closestMoon.getElapsedTime()));
        events.add(String.format(Locale.US,
                "Interfaz de entrada: %.0f km de altitud a T+%.0f s",
                finalState.getAltitude(), finalState.getElapsedTime()));
        return events;
    }
}
