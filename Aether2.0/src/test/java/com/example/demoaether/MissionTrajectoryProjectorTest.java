package com.example.demoaether;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionTrajectoryProjectorTest {

    @Test
    void officialRouteLeavesEarthPassesMoonAndReturnsToEarth() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        MissionTrajectoryProjector projector = MissionTrajectoryProjector.from(trajectory);
        List<MissionTrajectoryProjector.ProjectedPoint> points = projector.getDisplayPoints();

        double[] first = projector.project(trajectory.getStates().get(0));
        double[] last = projector.project(trajectory.lastState());
        double closestMoon = points.stream()
                .mapToDouble(point -> distance(
                        point.coordinates(),
                        new double[]{MissionTrajectoryProjector.MOON_X, 0.0, 0.0}))
                .min()
                .orElseThrow();

        assertTrue(distance(first, new double[]{MissionTrajectoryProjector.EARTH_X, 0.0, 0.0}) < 60.0);
        assertTrue(closestMoon < MissionTrajectoryProjector.MOON_DISPLAY_INFLUENCE_RADIUS,
                "La ruta debe entrar en la zona del sobrevuelo lunar.");
        assertTrue(distance(last, new double[]{MissionTrajectoryProjector.EARTH_X, 0.0, 0.0}) < 20.0);
        assertTrue(points.stream().flatMapToDouble(point -> java.util.Arrays.stream(point.coordinates()))
                .allMatch(Double::isFinite));
    }

    @Test
    void projectionUsesOneScaleForAllThreeAxes() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        MissionTrajectoryProjector projector = MissionTrajectoryProjector.from(trajectory);
        List<MissionState> states = trajectory.getStates();

        MissionState first = states.get(0);
        MissionState middle = states.get(states.size() / 2);
        MissionState last = states.get(states.size() - 1);
        double outboundScale = distance(projector.project(first), projector.project(middle))
                / distance(position(first), position(middle));
        double returnScale = distance(projector.project(middle), projector.project(last))
                / distance(position(middle), position(last));

        // Una rotacion 3D seguida de escala uniforme conserva la misma razon en cualquier tramo.
        assertEquals(outboundScale, returnScale, outboundScale * 1.0e-9);
    }

    @Test
    void displayedSegmentsStayOutsideEarthAndMoon() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        MissionTrajectoryProjector projector = MissionTrajectoryProjector.from(trajectory);
        List<MissionTrajectoryProjector.ProjectedPoint> points = projector.getDisplayPoints();

        double earthRadius = MissionTrajectoryProjector.DISPLAY_EARTH_RADIUS;
        double moonRadius = MissionTrajectoryProjector.DISPLAY_MOON_RADIUS;
        double minimumEarthDistance = minimumSegmentDistance(
                points, new double[]{MissionTrajectoryProjector.EARTH_X, 0.0, 0.0});
        double minimumMoonDistance = minimumSegmentDistance(
                points, new double[]{MissionTrajectoryProjector.MOON_X, 0.0, 0.0});
        double minimumEarthViewDistance = minimumSegmentDistance2d(
                points, new double[]{MissionTrajectoryProjector.EARTH_X, 0.0});
        double minimumMoonViewDistance = minimumSegmentDistance2d(
                points, new double[]{MissionTrajectoryProjector.MOON_X, 0.0});

        // La lente grafica mantiene cada tramo fuera de las siluetas ampliadas.
        assertTrue(minimumEarthDistance >= earthRadius - 0.05,
                () -> "La ruta visual entra en la Tierra: " + minimumEarthDistance);
        assertTrue(minimumMoonDistance >= moonRadius - 0.05,
                () -> "La ruta visual entra en la Luna: " + minimumMoonDistance);
        assertTrue(minimumEarthViewDistance >= earthRadius - 0.05,
                () -> "La ruta cruza la silueta terrestre: " + minimumEarthViewDistance);
        assertTrue(minimumMoonViewDistance >= moonRadius - 0.05,
                () -> "La ruta cruza la silueta lunar: " + minimumMoonViewDistance);
    }

    @Test
    void displayProjectionKeepsDeepSpaceExactAndEnlargesCloseEncounters() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        MissionTrajectoryProjector projector = MissionTrajectoryProjector.from(trajectory);
        List<MissionState> states = trajectory.getStates();

        MissionState nearEarth = states.stream()
                .min(java.util.Comparator.comparingDouble(MissionState::getDistanceEarth))
                .orElseThrow();
        assertTrue(distance2d(
                        projector.projectForDisplay(nearEarth),
                        MissionTrajectoryProjector.EARTH_X)
                        > distance2d(projector.project(nearEarth), MissionTrajectoryProjector.EARTH_X),
                "La vista debe ampliar el encuentro terrestre sin cambiar el estado fisico.");

        MissionState deepSpace = states.stream()
                .filter(state -> distance2d(
                        projector.project(state), MissionTrajectoryProjector.EARTH_X)
                        >= MissionTrajectoryProjector.EARTH_DISPLAY_INFLUENCE_RADIUS)
                .filter(state -> distance2d(
                        projector.project(state), MissionTrajectoryProjector.MOON_X)
                        >= MissionTrajectoryProjector.MOON_DISPLAY_INFLUENCE_RADIUS)
                .findFirst()
                .orElseThrow();
        assertArrayEquals(projector.project(deepSpace), projector.projectForDisplay(deepSpace), 1.0e-12,
                "La lente no debe mover ningun punto de espacio profundo.");
    }

    @Test
    void defaultViewShowsEarthAndLunarPassesOutsideTheBodySilhouettes() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        MissionTrajectoryProjector projector = MissionTrajectoryProjector.from(trajectory);
        MissionPhaseTimeline timeline = MissionPhaseTimeline.from(trajectory);

        MissionState departure = nearestState(trajectory.getStates(), timeline.getTliCenterSeconds());
        MissionState lunarEncounter = nearestState(
                trajectory.getStates(), timeline.getLunarEncounterSeconds());
        double[] departurePoint = projector.projectForDisplay(departure);
        double[] lunarPoint = projector.projectForDisplay(lunarEncounter);

        assertTrue(distance2d(departurePoint, MissionTrajectoryProjector.EARTH_X)
                        > MissionTrajectoryProjector.DISPLAY_EARTH_RADIUS,
                "El perigeo terrestre debe verse fuera de la silueta de la Tierra.");
        assertTrue(distance2d(lunarPoint, MissionTrajectoryProjector.MOON_X)
                        > MissionTrajectoryProjector.DISPLAY_MOON_RADIUS,
                "El periapsis lunar debe verse fuera de la silueta de la Luna.");
    }

    @Test
    void moonUsesItsEphemerisAndMeetsTheSpacecraftAtTheFlyby() {
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        MissionTrajectoryProjector projector = MissionTrajectoryProjector.from(trajectory);
        List<MissionState> states = trajectory.getStates();
        MissionState first = states.get(0);
        MissionState last = states.get(states.size() - 1);
        MissionState flyby = states.stream()
                .min(java.util.Comparator.comparingDouble(MissionState::getDistanceMoon))
                .orElseThrow();

        double[] firstMoon = projector.projectMoonForDisplay(first);
        double[] lastMoon = projector.projectMoonForDisplay(last);
        assertEquals(0.0, MissionTrajectoryProjector.EARTH_X, 1.0e-12,
                "La Tierra debe ocupar el centro horizontal del mapa.");
        assertTrue(distance(firstMoon, lastMoon) > 20.0,
                "La Luna debe cambiar de posicion durante los dias de mision.");
        assertTrue(firstMoon[1] * lastMoon[1] < 0.0,
                "La Luna debe recorrer lados opuestos de la elipse visible.");
        assertTrue(Math.abs(firstMoon[1]) < 140.0 && Math.abs(lastMoon[1]) < 140.0,
                "El arco lunar completo debe permanecer dentro del alto util del mapa.");
        assertEquals(
                projector.scaleDistance(flyby.getDistanceMoon()),
                distance(projector.project(flyby), projector.projectMoonPhysical(flyby)),
                1.0e-6,
                "El centro lunar y Orion deben conservar su separacion fisica en el sobrevuelo.");
        assertTrue(distance(
                        projector.projectForDisplay(flyby), projector.projectMoonForDisplay(flyby))
                        > MissionTrajectoryProjector.DISPLAY_MOON_RADIUS,
                "La ampliacion grafica debe dejar a Orion fuera de la Luna.");
    }

    private static double[] position(MissionState state) {
        return new double[]{state.getX(), state.getY(), state.getZ()};
    }

    private static double distance(double[] first, double[] second) {
        double dx = first[0] - second[0];
        double dy = first[1] - second[1];
        double dz = first[2] - second[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double distance2d(double[] point, double centerX) {
        double dx = point[0] - centerX;
        return Math.sqrt(dx * dx + point[1] * point[1]);
    }

    private static MissionState nearestState(List<MissionState> states, double elapsedSeconds) {
        return states.stream()
                .min(java.util.Comparator.comparingDouble(
                        state -> Math.abs(state.getElapsedTime() - elapsedSeconds)))
                .orElseThrow();
    }

    private static double minimumSegmentDistance(
            List<MissionTrajectoryProjector.ProjectedPoint> points,
            double[] center) {
        double minimum = Double.POSITIVE_INFINITY;
        for (int index = 1; index < points.size(); index++) {
            minimum = Math.min(minimum, pointToSegmentDistance(
                    center,
                    points.get(index - 1).coordinates(),
                    points.get(index).coordinates()));
        }
        return minimum;
    }

    private static double minimumSegmentDistance2d(
            List<MissionTrajectoryProjector.ProjectedPoint> points,
            double[] center) {
        double minimum = Double.POSITIVE_INFINITY;
        for (int index = 1; index < points.size(); index++) {
            double[] start3d = points.get(index - 1).coordinates();
            double[] end3d = points.get(index).coordinates();
            minimum = Math.min(minimum, pointToSegmentDistance(
                    new double[]{center[0], center[1], 0.0},
                    new double[]{start3d[0], start3d[1], 0.0},
                    new double[]{end3d[0], end3d[1], 0.0}));
        }
        return minimum;
    }

    private static double pointToSegmentDistance(double[] point, double[] start, double[] end) {
        double[] segment = {
                end[0] - start[0],
                end[1] - start[1],
                end[2] - start[2]
        };
        double[] offset = {
                point[0] - start[0],
                point[1] - start[1],
                point[2] - start[2]
        };
        double lengthSquared = segment[0] * segment[0]
                + segment[1] * segment[1]
                + segment[2] * segment[2];
        double ratio = lengthSquared < 1.0e-12
                ? 0.0
                : Math.max(0.0, Math.min(1.0,
                (offset[0] * segment[0] + offset[1] * segment[1] + offset[2] * segment[2])
                        / lengthSquared));
        double[] closest = {
                start[0] + segment[0] * ratio,
                start[1] + segment[1] * ratio,
                start[2] + segment[2] * ratio
        };
        return distance(point, closest);
    }
}
