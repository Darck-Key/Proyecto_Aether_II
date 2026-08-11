package com.example.demoaether;

import javafx.beans.binding.Bindings;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

/**
 * Vista 3D del mapa de mision.
 *
 * Quien llama:
 * - HelloController.initializeMissionMap() muestra el placeholder.
 * - HelloController configura la ruta una vez y llama updateState() en cada frame.
 * - Los botones de zoom llaman zoomIn(), zoomOut() y resetCamera().
 *
 * Que hace:
 * - Dibuja Tierra centrada, Luna movil, estrellas, una guia limpia y la nave triangular.
 * - No calcula fisica orbital; proyecta los MissionState recibidos sin inventar puntos.
 */
public class MissionMap3D {

    private static final double WIDTH = 900;
    private static final double HEIGHT = 370;
    private static final double EARTH_X = MissionTrajectoryProjector.EARTH_X;
    private static final double EARTH_Y = 0;
    private static final double EARTH_Z = 0;
    private static final double MOON_X = MissionTrajectoryProjector.MOON_X;
    private static final double MOON_Y = 0;
    private static final double MOON_Z = 0;
    private static final double EARTH_RADIUS = MissionTrajectoryProjector.DISPLAY_EARTH_RADIUS;
    private static final double MOON_RADIUS = MissionTrajectoryProjector.DISPLAY_MOON_RADIUS;
    private static final double CAMERA_FIELD_OF_VIEW = 11;
    private static final double DEFAULT_CAMERA_DISTANCE = -1550;
    private static final double MIN_CAMERA_DISTANCE = -3400;
    private static final double MAX_CAMERA_DISTANCE = -900;
    private static final double CAMERA_ZOOM_STEP = 180;
    private static final double DEFAULT_WORLD_PITCH = 0;
    private static final double DEFAULT_WORLD_YAW = 0;
    private static final double VISUAL_LAUNCH_DURATION_SECONDS = 15.0 * 60.0;
    private static final double LAUNCH_CENTER_RADIUS = EARTH_RADIUS * 0.68;
    private static final double LAUNCH_DEPTH_BEHIND_EARTH = 4.0;
    private static final String EARTH_TEXTURE =
            "/com/example/demoaether/imagenes/earth-texture.png";
    private static final String MOON_TEXTURE =
            "/com/example/demoaether/imagenes/moon-texture.jpg";
    private static final double WORLD_CENTER_X = EARTH_X;
    private static final int MAX_TRAIL_POINTS = 96;
    private static final double ROUTE_SAMPLE_SPACING = 0.8;
    private static final double TRAIL_SAMPLE_SPACING = 1.4;
    private static final List<double[]> TELEMETRY_TRAIL = new ArrayList<>();
    private static double initialCameraDistance = DEFAULT_CAMERA_DISTANCE;
    private static double cameraDistance = DEFAULT_CAMERA_DISTANCE;
    private static double cameraCenterX = EARTH_X;
    private static double worldPitch = DEFAULT_WORLD_PITCH;
    private static double worldYaw = DEFAULT_WORLD_YAW;
    private static MissionTrajectoryProjector projector;
    private static MissionTrajectory activeTrajectory;
    private static MissionPhaseTimeline activePhaseTimeline;
    private static SubScene activeScene;
    private static PerspectiveCamera activeCamera;
    private static Group activeWorld;
    private static Group trailGroup;
    private static Group moonGroup;
    private static Group spacecraftGroup;
    private static Rotate worldPitchRotation;
    private static Rotate worldYawRotation;
    private static double[] lastSpacecraftPosition;

    private MissionMap3D() {
    }

    /**
     * Crea una escena inicial antes de que exista telemetria real.
     *
     * @return nodo JavaFX listo para insertarse en missionMapContainer
     */
    public static Node createPlaceholder() {
        // Estado inicial del mapa antes de tener telemetria calculada.
        projector = null;
        activeTrajectory = null;
        activePhaseTimeline = null;
        activeScene = null;
        initialCameraDistance = DEFAULT_CAMERA_DISTANCE;
        cameraCenterX = EARTH_X;
        resetViewState();
        resetTrail();
        MissionState initial = new MissionState(0, 6671, 0, 0, 7.8, 6671, 377729, 300);
        return createScene(initial);
    }

    /**
     * Prepara una unica proyeccion para todos los puntos orbitales.
     * Quien llama: HelloController antes de iniciar TrajectoryPlayback.
     */
    public static Node configureTrajectory(MissionTrajectory trajectory) {
        activeTrajectory = trajectory;
        projector = MissionTrajectoryProjector.from(trajectory);
        activePhaseTimeline = MissionPhaseTimeline.from(trajectory);
        activeScene = null;
        CameraFraming framing = calculateCameraFraming();
        initialCameraDistance = framing.distance();
        cameraCenterX = framing.centerX();
        resetViewState();
        resetTrail();
        return createScene(trajectory.getStates().get(0), trajectory.getDurationSeconds());
    }

    /**
     * Limpia la trayectoria acumulada antes de una nueva corrida.
     */
    public static void resetTrail() {
        // Lo llama HelloController al iniciar/reiniciar para que cada simulacion dibuje su propia ruta.
        TELEMETRY_TRAIL.clear();
        lastSpacecraftPosition = null;
        if (trailGroup != null) {
            trailGroup.getChildren().clear();
        }
    }

    /**
     * Crea la escena 3D a partir del estado actual de la mision.
     *
     * @param state telemetria actual usada para inferir progreso visual
     * @return subescena JavaFX con Tierra, Luna, trayectoria, nave y estrellas
     */
    public static Node createScene(MissionState state) {
        return createScene(state, 10.0 * 3600.0);
    }

    /**
     * Crea la escena 3D usando la duracion configurada para sincronizar mapa, fases y reloj.
     *
     * @param state telemetria actual usada para inferir progreso visual
     * @param missionDurationSeconds duracion total de la simulacion en segundos
     * @return subescena JavaFX con progreso proporcional a la duracion real
     */
    public static Node createScene(MissionState state, double missionDurationSeconds) {
        // La SubScene se crea una sola vez; cada frame posterior llama updateState().
        if (activeScene != null) {
            updateState(state);
            return activeScene;
        }
        double[] craft = spacecraftPositionFromTelemetry(state);

        trailGroup = new Group();
        moonGroup = createMoon();
        updateMoonPosition(state);
        spacecraftGroup = createSpacecraft(craft);
        applyDepthSizeCompensation(spacecraftGroup, craft[2]);
        activeWorld = new Group(
                trailGroup,
                createEarth(),
                moonGroup,
                spacecraftGroup
        );
        configureWorldRotation();

        Group root = new Group(createStars(), activeWorld);
        root.setDepthTest(DepthTest.ENABLE);

        // La luz ambiental evita caras negras; la luz puntual revela el volumen de cada objeto.
        AmbientLight ambientLight = new AmbientLight(Color.web("#70657F"));
        PointLight light = new PointLight(Color.web("#E7D8F0"));
        light.setTranslateX(-120);
        light.setTranslateY(-220);
        light.setTranslateZ(-320);
        root.getChildren().addAll(ambientLight, light);

        activeScene = new SubScene(root, WIDTH, HEIGHT, true, SceneAntialiasing.BALANCED);
        activeScene.setFill(Color.web("#05030D"));

        activeCamera = new PerspectiveCamera(true);
        activeCamera.setTranslateX(cameraCenterX);
        activeCamera.setTranslateZ(cameraDistance);
        activeCamera.setFieldOfView(CAMERA_FIELD_OF_VIEW);
        activeCamera.setVerticalFieldOfView(true);
        activeCamera.setNearClip(0.1);
        activeCamera.setFarClip(7000);
        activeScene.setCamera(activeCamera);
        bindSceneToContainer();
        configureCameraControls();

        appendTrailPoint(craft, state.getElapsedTime());
        orientSpacecraftAtStart();
        return activeScene;
    }

    /**
     * Mueve la nave usando el estado interpolado y revela la ruta ya calculada.
     * No reconstruye planetas, estrellas ni camara durante la animacion.
     */
    public static void updateState(MissionState state) {
        if (state == null) {
            return;
        }
        if (activeScene == null) {
            createScene(state);
            return;
        }
        updateMoonPosition(state);
        double[] position = spacecraftPositionFromTelemetry(state);
        if (spacecraftGroup != null) {
            spacecraftGroup.setTranslateX(position[0]);
            spacecraftGroup.setTranslateY(position[1]);
            spacecraftGroup.setTranslateZ(position[2]);
            applyDepthSizeCompensation(spacecraftGroup, position[2]);
        }
        if (lastSpacecraftPosition != null) {
            orientSpacecraft(lastSpacecraftPosition, position);
        }
        appendTrailPoint(position, state.getElapsedTime());
    }

    /**
     * Acerca la camara del mapa.
     */
    public static void zoomIn() {
        // Llamado por el boton Zoom + del FXML.
        cameraDistance = Math.min(
                MAX_CAMERA_DISTANCE, cameraDistance + CAMERA_ZOOM_STEP);
        if (activeCamera != null) {
            activeCamera.setTranslateX(cameraCenterX);
            activeCamera.setTranslateZ(cameraDistance);
        }
    }

    /**
     * Aleja la camara del mapa.
     */
    public static void zoomOut() {
        // Llamado por el boton Zoom - del FXML.
        cameraDistance = Math.max(
                MIN_CAMERA_DISTANCE, cameraDistance - CAMERA_ZOOM_STEP);
        if (activeCamera != null) {
            activeCamera.setTranslateZ(cameraDistance);
        }
    }

    /**
     * Devuelve la camara a la distancia por defecto.
     */
    public static void resetCamera() {
        // Restaura distancia y angulos para volver al encuadre tridimensional inicial.
        resetViewState();
        if (activeCamera != null) {
            activeCamera.setTranslateZ(cameraDistance);
        }
        applyWorldRotation();
    }

    /**
     * Calcula una distancia inicial que mantiene toda la trayectoria dentro del panel.
     * Quien llama: configureTrajectory() despues de crear el proyector 3D.
     */
    private static CameraFraming calculateCameraFraming() {
        if (projector == null || projector.getDisplayPoints().isEmpty()) {
            return new CameraFraming(EARTH_X, DEFAULT_CAMERA_DISTANCE);
        }

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        for (MissionTrajectoryProjector.ProjectedPoint point : projector.getDisplayPoints()) {
            double[] coordinates = point.coordinates();
            minX = Math.min(minX, coordinates[0]);
            maxX = Math.max(maxX, coordinates[0]);
            minY = Math.min(minY, coordinates[1]);
            maxY = Math.max(maxY, coordinates[1]);
            minZ = Math.min(minZ, coordinates[2]);
            maxZ = Math.max(maxZ, coordinates[2]);
        }

        double halfFov = Math.toRadians(CAMERA_FIELD_OF_VIEW / 2.0);
        double aspectRatio = WIDTH / HEIGHT;
        // Centra el conjunto completo, no solo la Tierra. Esto aprovecha el
        // espacio vacio de la izquierda y permite ver cuerpos y rastro mas grandes.
        double centerX = (minX + maxX) / 2.0;
        double horizontalSpan = maxX - minX + 90.0;
        double verticalSpan = maxY - minY + 70.0;
        double horizontalDistance = horizontalSpan
                / (2.0 * Math.tan(halfFov) * aspectRatio);
        double verticalDistance = verticalSpan
                / (2.0 * Math.tan(halfFov));
        // La profundidad solo necesita una reserva moderada: sumarla casi completa
        // alejaba la camara y desperdiciaba mas de la mitad del panel.
        double depthMargin = Math.max(35.0, (maxZ - minZ) * 0.12 + 24.0);
        double requiredDistance = Math.max(horizontalDistance, verticalDistance) + depthMargin;
        double distance = -clamp(requiredDistance, 1050.0, Math.abs(MIN_CAMERA_DISTANCE));
        return new CameraFraming(centerX, distance);
    }

    /**
     * Ajusta el render al interior del Pane existente para que no quede corrido ni recortado.
     * Quien llama: createScene(); el listener se activa cuando HelloController inserta la SubScene.
     */
    private static void bindSceneToContainer() {
        SubScene scene = activeScene;
        scene.setLayoutX(1.0);
        scene.setLayoutY(1.0);
        scene.parentProperty().addListener((observable, previousParent, currentParent) -> {
            scene.widthProperty().unbind();
            scene.heightProperty().unbind();
            if (currentParent instanceof Region region) {
                scene.widthProperty().bind(Bindings.max(1.0, region.widthProperty().subtract(2.0)));
                scene.heightProperty().bind(Bindings.max(1.0, region.heightProperty().subtract(2.0)));
            }
        });
    }

    /** Configura las rotaciones del mundo alrededor del punto medio Tierra-Luna. */
    private static void configureWorldRotation() {
        worldPitchRotation = new Rotate(worldPitch, WORLD_CENTER_X, 0, 0, Rotate.X_AXIS);
        worldYawRotation = new Rotate(worldYaw, WORLD_CENTER_X, 0, 0, Rotate.Y_AXIS);
        activeWorld.getTransforms().setAll(worldPitchRotation, worldYawRotation);
    }

    /**
     * Conecta exclusivamente la rueda con el zoom de la camara.
     * Quien llama: createScene(); el arrastre queda libre para que la vista no
     * pierda su orientacion. Los botones del FXML siguen llamando zoomIn(),
     * zoomOut() y resetCamera().
     */
    private static void configureCameraControls() {
        activeScene.setOnScroll(event -> {
            cameraDistance = clamp(
                    cameraDistance + event.getDeltaY() * 1.4,
                    MIN_CAMERA_DISTANCE,
                    MAX_CAMERA_DISTANCE);
            activeCamera.setTranslateZ(cameraDistance);
            event.consume();
        });
    }

    /** Aplica los angulos actuales sin reconstruir objetos ni trayectoria. */
    private static void applyWorldRotation() {
        if (worldPitchRotation != null) {
            worldPitchRotation.setAngle(worldPitch);
        }
        if (worldYawRotation != null) {
            worldYawRotation.setAngle(worldYaw);
        }
    }

    /** Restablece el encuadre conservando la distancia calculada para la ruta activa. */
    private static void resetViewState() {
        cameraDistance = initialCameraDistance;
        worldPitch = DEFAULT_WORLD_PITCH;
        worldYaw = DEFAULT_WORLD_YAW;
    }

    private static Group createEarth() {
        // Blue Marble envuelve toda la esfera; no usa continentes superpuestos ni manchas sueltas.
        Group group = new Group();
        Sphere earth = sphere(EARTH_RADIUS, "#6DA8FF");
        earth.setMaterial(texturedCelestialMaterial(
                EARTH_TEXTURE,
                "#F4F7FF",
                "#6DA8FF"));
        earth.setRotationAxis(Rotate.Y_AXIS);
        earth.setRotate(-72.0);
        group.getChildren().add(earth);
        group.setTranslateX(EARTH_X);
        group.setTranslateY(EARTH_Y);
        group.setTranslateZ(EARTH_Z);
        return group;
    }

    private static Group createMoon() {
        // El mosaico LROC aporta crateres reales y recibe un tinte lavanda muy tenue.
        Group group = new Group();
        Sphere moon = sphere(MOON_RADIUS, "#D8CAD5");
        moon.setMaterial(texturedCelestialMaterial(
                MOON_TEXTURE,
                "#E9DDE7",
                "#B8AABB"));
        moon.setRotationAxis(Rotate.Y_AXIS);
        moon.setRotate(24.0);
        group.getChildren().add(moon);
        group.setTranslateX(MOON_X);
        group.setTranslateY(MOON_Y);
        group.setTranslateZ(MOON_Z);
        return group;
    }

    private static Group createPlannedTrajectory() {
        // Una unica banda continua evita caras redondas cuyo sombreado parecia entrecortado.
        Group group = new Group();
        if (projector == null || activeTrajectory == null) {
            return group;
        }
        List<MissionTrajectoryProjector.ProjectedPoint> points = smoothTrajectory(
                projector.getDisplayPoints());
        if (points.size() < 2) {
            return group;
        }
        MeshView route = createTrajectoryRibbon(points, "#9B82C4", 0.82);
        route.setOpacity(0.88);
        group.getChildren().add(route);
        return group;
    }

    /**
     * Construye una banda 3D continua orientada hacia la camara fija. Quien
     * llama: createPlannedTrajectory() y rebuildTrailGeometry(). Las uniones
     * usan una normal bisectriz, por lo que no quedan huecos ni vertices en pico.
     */
    private static MeshView createTrajectoryRibbon(
            List<MissionTrajectoryProjector.ProjectedPoint> points,
            String color,
            double halfWidth) {
        List<double[]> path = new ArrayList<>();
        for (MissionTrajectoryProjector.ProjectedPoint point : points) {
            double[] coordinates = point.coordinates();
            if (path.isEmpty() || distance(path.get(path.size() - 1), coordinates) > 1.0e-5) {
                path.add(coordinates);
            }
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);
        if (path.size() < 2) {
            MeshView empty = new MeshView(mesh);
            empty.setMaterial(material(color));
            return empty;
        }

        for (int index = 0; index < path.size(); index++) {
            double[] previous = path.get(Math.max(0, index - 1));
            double[] current = path.get(index);
            double[] next = path.get(Math.min(path.size() - 1, index + 1));
            double[] incomingNormal = screenNormal(previous, current, current, next);
            double[] outgoingNormal = screenNormal(current, next, previous, current);
            double normalX = incomingNormal[0] + outgoingNormal[0];
            double normalY = incomingNormal[1] + outgoingNormal[1];
            double normalLength = Math.hypot(normalX, normalY);
            if (normalLength < 1.0e-8) {
                normalX = outgoingNormal[0];
                normalY = outgoingNormal[1];
                normalLength = Math.max(1.0e-8, Math.hypot(normalX, normalY));
            }
            normalX /= normalLength;
            normalY /= normalLength;

            double alignment = Math.abs(normalX * outgoingNormal[0] + normalY * outgoingNormal[1]);
            double joinWidth = Math.min(halfWidth * 1.6, halfWidth / Math.max(0.62, alignment));
            mesh.getPoints().addAll(
                    (float) (current[0] + normalX * joinWidth),
                    (float) (current[1] + normalY * joinWidth),
                    (float) current[2],
                    (float) (current[0] - normalX * joinWidth),
                    (float) (current[1] - normalY * joinWidth),
                    (float) current[2]);
        }

        int faceCount = 0;
        for (int segment = 0; segment < path.size() - 1; segment++) {
            int left = segment * 2;
            int right = left + 1;
            int nextLeft = left + 2;
            int nextRight = left + 3;
            mesh.getFaces().addAll(
                    left, 0, right, 0, nextLeft, 0,
                    right, 0, nextRight, 0, nextLeft, 0);
            faceCount += 2;
        }
        int[] smoothingGroups = new int[faceCount];
        java.util.Arrays.fill(smoothingGroups, 1);
        mesh.getFaceSmoothingGroups().addAll(smoothingGroups);

        MeshView result = new MeshView(mesh);
        result.setCullFace(CullFace.NONE);
        result.setDrawMode(DrawMode.FILL);
        result.setMaterial(trajectoryMaterial(color));
        return result;
    }

    /** Devuelve la normal 2D de un tramo y usa el tramo vecino si mide cero. */
    private static double[] screenNormal(
            double[] from,
            double[] to,
            double[] fallbackFrom,
            double[] fallbackTo) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double length = Math.hypot(dx, dy);
        if (length < 1.0e-8) {
            dx = fallbackTo[0] - fallbackFrom[0];
            dy = fallbackTo[1] - fallbackFrom[1];
            length = Math.hypot(dx, dy);
        }
        if (length < 1.0e-8) {
            return new double[]{0.0, 1.0};
        }
        return new double[]{-dy / length, dx / length};
    }

    /**
     * Inserta muestras Catmull-Rom centripetas entre los puntos OEM sin mover
     * las anclas. Quien llama: createPlannedTrajectory() y la estela corta.
     * El parametro por distancia evita picos cuando las muestras no estan
     * igualmente separadas y genera curvas tangentes continuas.
     */
    private static List<MissionTrajectoryProjector.ProjectedPoint> smoothTrajectory(
            List<MissionTrajectoryProjector.ProjectedPoint> source) {
        return smoothTrajectory(source, ROUTE_SAMPLE_SPACING, 32);
    }

    private static List<MissionTrajectoryProjector.ProjectedPoint> smoothTrajectory(
            List<MissionTrajectoryProjector.ProjectedPoint> source,
            double sampleSpacing,
            int maximumSubdivisions) {
        if (source.size() < 3) {
            return source;
        }
        List<MissionTrajectoryProjector.ProjectedPoint> result = new ArrayList<>();
        result.add(source.get(0));
        for (int index = 0; index < source.size() - 1; index++) {
            MissionTrajectoryProjector.ProjectedPoint p1 = source.get(index);
            MissionTrajectoryProjector.ProjectedPoint p2 = source.get(index + 1);
            double[] p0 = index == 0
                    ? extrapolate(p1.coordinates(), p2.coordinates())
                    : source.get(index - 1).coordinates();
            double[] p3 = index + 2 >= source.size()
                    ? extrapolate(p2.coordinates(), p1.coordinates())
                    : source.get(index + 2).coordinates();
            int subdivisions = (int) clamp(
                    Math.ceil(distance(p1.coordinates(), p2.coordinates()) / sampleSpacing),
                    1,
                    maximumSubdivisions);
            for (int step = 1; step <= subdivisions; step++) {
                double ratio = step / (double) subdivisions;
                double[] coordinates = centripetalCatmullRom(
                        p0,
                        p1.coordinates(),
                        p2.coordinates(),
                        p3,
                        ratio);
                result.add(new MissionTrajectoryProjector.ProjectedPoint(
                        lerp(p1.elapsedSeconds(), p2.elapsedSeconds(), ratio),
                        coordinates));
            }
        }
        return result;
    }

    /** Evalua una Catmull-Rom centripeta; alpha=0.5 reduce lazos y esquinas falsas. */
    private static double[] centripetalCatmullRom(
            double[] p0,
            double[] p1,
            double[] p2,
            double[] p3,
            double ratio) {
        double t0 = 0.0;
        double t1 = t0 + centripetalStep(p0, p1);
        double t2 = t1 + centripetalStep(p1, p2);
        double t3 = t2 + centripetalStep(p2, p3);
        double t = lerp(t1, t2, ratio);

        double[] a1 = parameterLerp(p0, p1, t0, t1, t);
        double[] a2 = parameterLerp(p1, p2, t1, t2, t);
        double[] a3 = parameterLerp(p2, p3, t2, t3, t);
        double[] b1 = parameterLerp(a1, a2, t0, t2, t);
        double[] b2 = parameterLerp(a2, a3, t1, t3, t);
        return parameterLerp(b1, b2, t1, t2, t);
    }

    private static double centripetalStep(double[] from, double[] to) {
        return Math.max(1.0e-4, Math.sqrt(distance(from, to)));
    }

    private static double[] parameterLerp(
            double[] from,
            double[] to,
            double fromParameter,
            double toParameter,
            double parameter) {
        double range = Math.max(1.0e-9, toParameter - fromParameter);
        double ratio = (parameter - fromParameter) / range;
        return new double[]{
                lerp(from[0], to[0], ratio),
                lerp(from[1], to[1], ratio),
                lerp(from[2], to[2], ratio)
        };
    }

    private static double[] extrapolate(double[] anchor, double[] neighbor) {
        return new double[]{
                2.0 * anchor[0] - neighbor[0],
                2.0 * anchor[1] - neighbor[1],
                2.0 * anchor[2] - neighbor[2]
        };
    }

    /** Coloca puntos pequenos exactamente donde cambia cada fase calculada. */
    private static Group createPhaseMarkers() {
        Group group = new Group();
        if (projector == null || activePhaseTimeline == null) {
            return group;
        }
        List<MissionTrajectoryProjector.ProjectedPoint> points = projector.getDisplayPoints();
        for (MissionPhaseTimeline.Transition transition : activePhaseTimeline.transitions()) {
            double[] location = nearestPoint(points, transition.elapsedSeconds());
            Sphere marker = sphere(1.8, transition.phase().color());
            marker.setTranslateX(location[0]);
            marker.setTranslateY(location[1]);
            marker.setTranslateZ(location[2]);
            group.getChildren().add(marker);
        }
        return group;
    }

    private static double[] nearestPoint(
            List<MissionTrajectoryProjector.ProjectedPoint> points,
            double elapsedSeconds) {
        MissionTrajectoryProjector.ProjectedPoint result = points.get(0);
        double minimumDifference = Double.POSITIVE_INFINITY;
        for (MissionTrajectoryProjector.ProjectedPoint point : points) {
            double difference = Math.abs(point.elapsedSeconds() - elapsedSeconds);
            if (difference < minimumDifference) {
                result = point;
                minimumDifference = difference;
            }
        }
        return result.coordinates();
    }

    private static Group createSpacecraft(double[] position) {
        // Malla tetraedrica: mantiene la silueta triangular, pero ya posee volumen real.
        Group group = new Group();
        MeshView ship = spacecraftHull("#A46C93");
        Sphere engine = sphere(2.2, "#C79CFF");
        engine.setTranslateY(9.0);
        group.getChildren().addAll(ship, engine);
        group.setTranslateX(position[0]);
        group.setTranslateY(position[1]);
        group.setTranslateZ(position[2]);
        return group;
    }

    /** Orienta la nave con el primer segmento de la ruta antes de iniciar el reloj. */
    private static void orientSpacecraftAtStart() {
        if (projector == null || activeTrajectory == null) {
            return;
        }
        double[] surface = launchSurfacePosition();
        double[] firstOrbit = projector.projectForDisplay(activeTrajectory.getStates().get(0));
        orientSpacecraft(surface, firstOrbit);
    }

    /**
     * Alinea la punta de la nave con su desplazamiento X/Y/Z real.
     * Quien llama: updateState() en cada cuadro y orientSpacecraftAtStart() al cargar.
     */
    private static void orientSpacecraft(double[] from, double[] to) {
        if (spacecraftGroup == null) {
            return;
        }
        Point3D direction = new Point3D(
                to[0] - from[0],
                to[1] - from[1],
                to[2] - from[2]);
        if (direction.magnitude() < 1.0e-6) {
            return;
        }

        Point3D forward = new Point3D(0, -1, 0);
        Point3D normalizedDirection = direction.normalize();
        double cosine = clamp(forward.dotProduct(normalizedDirection), -1.0, 1.0);
        Point3D axis = forward.crossProduct(normalizedDirection);

        spacecraftGroup.getTransforms().clear();
        if (axis.magnitude() < 1.0e-6) {
            if (cosine < 0.0) {
                spacecraftGroup.getTransforms().add(new Rotate(180, Rotate.X_AXIS));
            }
            return;
        }
        spacecraftGroup.getTransforms().add(
                new Rotate(Math.toDegrees(Math.acos(cosine)), axis.normalize()));
    }

    private static Group createStars() {
        // Capas Z distintas producen paralaje cuando el usuario gira la escena.
        Group group = new Group();
        String[] starColors = {"#C79CFF", "#A46CFF", "#6DA8FF"};
        for (int i = 0; i < 190; i++) {
            Sphere star = sphere(i % 13 == 0 ? 1.15 : i % 5 == 0 ? 0.75 : 0.45, starColors[i % starColors.length]);
            star.setTranslateX(-420 + ((i * 113) % 840));
            star.setTranslateY(-158 + ((i * 67) % 300));
            star.setTranslateZ(120 + ((i * 31) % 360));
            star.setOpacity(i % 4 == 0 ? 0.82 : i % 3 == 0 ? 0.58 : 0.38);
            star.setMouseTransparent(true);
            group.getChildren().add(star);
        }
        return group;
    }

    private static double[] spacecraftPositionFromTelemetry(MissionState state) {
        // La ruta activa usa una unica proyeccion EME2000 calculada con todos sus puntos.
        if (projector != null && activeTrajectory != null) {
            double[] orbitalPosition = projector.projectForDisplay(state);
            double startSeconds = activeTrajectory.getStates().get(0).getElapsedTime();
            double launchProgress = clamp(
                    (state.getElapsedTime() - startSeconds) / VISUAL_LAUNCH_DURATION_SECONDS,
                    0.0,
                    1.0);
            if (launchProgress >= 1.0) {
                return orbitalPosition;
            }

            // El OEM oficial comienza con Orion ya en orbita. Esta mezcla solo
            // completa visualmente el despegue; no cambia el MissionState.
            double easedProgress = launchProgress * launchProgress
                    * (3.0 - 2.0 * launchProgress);
            return interpolatePosition(
                    launchSurfacePosition(),
                    orbitalPosition,
                    easedProgress);
        }
        // Antes de cargar Orekit, la punta queda fuera y la base oculta por la Tierra.
        return new double[]{
                EARTH_X,
                EARTH_Y - LAUNCH_CENTER_RADIUS,
                EARTH_Z + LAUNCH_DEPTH_BEHIND_EARTH
        };
    }

    /**
     * Calcula el punto de salida sobre el mismo lado donde comienza la orbita
     * oficial. Quien llama: createScene() y spacecraftPositionFromTelemetry().
     */
    private static double[] launchSurfacePosition() {
        if (projector == null || activeTrajectory == null) {
            return new double[]{
                    EARTH_X,
                    EARTH_Y - LAUNCH_CENTER_RADIUS,
                    EARTH_Z + LAUNCH_DEPTH_BEHIND_EARTH
            };
        }
        double[] firstOrbit = projector.projectForDisplay(activeTrajectory.getStates().get(0));
        double dx = firstOrbit[0] - EARTH_X;
        double dy = firstOrbit[1] - EARTH_Y;
        double dz = firstOrbit[2] - EARTH_Z;
        double magnitude = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (magnitude < 1.0e-9) {
            return new double[]{
                    EARTH_X,
                    EARTH_Y - LAUNCH_CENTER_RADIUS,
                    EARTH_Z + LAUNCH_DEPTH_BEHIND_EARTH
            };
        }
        return new double[]{
                EARTH_X + dx / magnitude * LAUNCH_CENTER_RADIUS,
                EARTH_Y + dy / magnitude * LAUNCH_CENTER_RADIUS,
                EARTH_Z + dz / magnitude * LAUNCH_CENTER_RADIUS
                        + LAUNCH_DEPTH_BEHIND_EARTH
        };
    }

    private static double[] interpolatePosition(double[] from, double[] to, double ratio) {
        return new double[]{
                lerp(from[0], to[0], ratio),
                lerp(from[1], to[1], ratio),
                lerp(from[2], to[2], ratio)
        };
    }

    /** Mueve la esfera lunar con el vector EME2000 interpolado del estado actual. */
    private static void updateMoonPosition(MissionState state) {
        if (moonGroup == null) {
            return;
        }
        double[] position = projector != null && state != null && state.hasMoonPosition()
                ? projector.projectMoonForDisplay(state)
                : new double[]{MOON_X, MOON_Y, MOON_Z};
        moonGroup.setTranslateX(position[0]);
        moonGroup.setTranslateY(position[1]);
        moonGroup.setTranslateZ(position[2]);
        applyDepthSizeCompensation(moonGroup, position[2]);
    }

    /**
     * Conserva legible el tamano grafico cuando un cuerpo se mueve en Z.
     * Usa la camara inicial como referencia, por lo que el zoom solicitado por
     * botones o rueda sigue acercando y alejando toda la escena normalmente.
     */
    private static void applyDepthSizeCompensation(Group group, double objectZ) {
        double referenceDepth = Math.max(1.0, Math.abs(initialCameraDistance));
        double objectDepth = Math.max(1.0, objectZ - initialCameraDistance);
        double scale = clamp(objectDepth / referenceDepth, 0.68, 1.9);
        group.setScaleX(scale);
        group.setScaleY(scale);
        group.setScaleZ(scale);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void appendTrailPoint(double[] point, double elapsedSeconds) {
        // La linea nace detras de la nave: nunca se muestra la ruta futura.
        if (!TELEMETRY_TRAIL.isEmpty()) {
            double[] last = TELEMETRY_TRAIL.get(TELEMETRY_TRAIL.size() - 1);
            double dx = point[0] - last[0];
            double dy = point[1] - last[1];
            double dz = point[2] - last[2];
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) < 1.2) {
                lastSpacecraftPosition = point;
                return;
            }
        }
        TELEMETRY_TRAIL.add(point.clone());
        while (TELEMETRY_TRAIL.size() > MAX_TRAIL_POINTS) {
            TELEMETRY_TRAIL.remove(0);
        }
        rebuildTrailGeometry();
        lastSpacecraftPosition = point;
    }

    /** Redibuja la cola como una curva corta y continua; la fisica no se recalcula. */
    private static void rebuildTrailGeometry() {
        if (trailGroup == null) {
            return;
        }
        trailGroup.getChildren().clear();
        if (TELEMETRY_TRAIL.size() < 2) {
            return;
        }
        List<MissionTrajectoryProjector.ProjectedPoint> source = new ArrayList<>();
        for (int index = 0; index < TELEMETRY_TRAIL.size(); index++) {
            source.add(new MissionTrajectoryProjector.ProjectedPoint(
                    index,
                    TELEMETRY_TRAIL.get(index)));
        }
        List<MissionTrajectoryProjector.ProjectedPoint> relaxed = relaxTrailAnchors(source, 3);
        List<MissionTrajectoryProjector.ProjectedPoint> smooth = smoothTrajectory(
                relaxed,
                TRAIL_SAMPLE_SPACING,
                14);
        MeshView trail = createTrajectoryRibbon(
                smooth,
                "#A46C93",
                0.96);
        trail.setOpacity(0.90);
        trailGroup.getChildren().add(trail);
    }

    /**
     * Elimina pequenas variaciones entre muestras consecutivas antes de crear
     * la curva. Quien llama: rebuildTrailGeometry(). Es un filtro exclusivamente
     * visual: los MissionState originales, Orekit y la telemetria no se alteran.
     */
    private static List<MissionTrajectoryProjector.ProjectedPoint> relaxTrailAnchors(
            List<MissionTrajectoryProjector.ProjectedPoint> source,
            int passes) {
        if (source.size() < 4 || passes <= 0) {
            return source;
        }
        List<MissionTrajectoryProjector.ProjectedPoint> current = source;
        for (int pass = 0; pass < passes; pass++) {
            List<MissionTrajectoryProjector.ProjectedPoint> next = new ArrayList<>(current.size());
            next.add(current.get(0));
            for (int index = 1; index < current.size() - 1; index++) {
                MissionTrajectoryProjector.ProjectedPoint previous = current.get(index - 1);
                MissionTrajectoryProjector.ProjectedPoint anchor = current.get(index);
                MissionTrajectoryProjector.ProjectedPoint following = current.get(index + 1);
                double[] filtered = weightedMidpoint(
                        previous.coordinates(),
                        anchor.coordinates(),
                        following.coordinates());
                next.add(new MissionTrajectoryProjector.ProjectedPoint(
                        anchor.elapsedSeconds(),
                        filtered));
            }
            next.add(current.get(current.size() - 1));
            current = next;
        }
        return current;
    }

    /** Mantiene el punto central con peso doble para suavizar sin deformar la ruta. */
    private static double[] weightedMidpoint(
            double[] previous,
            double[] anchor,
            double[] following) {
        return new double[]{
                (previous[0] + 2.0 * anchor[0] + following[0]) / 4.0,
                (previous[1] + 2.0 * anchor[1] + following[1]) / 4.0,
                (previous[2] + 2.0 * anchor[2] + following[2]) / 4.0
        };
    }

    private static double distance(double[] from, double[] to) {
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double lerp(double from, double to, double ratio) {
        return from + (to - from) * ratio;
    }

    private static MeshView spacecraftHull(String color) {
        // Tetraedro apuntando hacia -Y y sobredimensionado para seguirlo durante la demo.
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(
                0, -16, 0,
                -8, 9, -4,
                8, 9, -4,
                0, 9, 7);
        mesh.getTexCoords().addAll(0, 0);
        mesh.getFaces().addAll(
                0, 0, 1, 0, 2, 0,
                0, 0, 2, 0, 3, 0,
                0, 0, 3, 0, 1, 0,
                1, 0, 3, 0, 2, 0);

        MeshView view = new MeshView(mesh);
        view.setCullFace(CullFace.NONE);
        view.setDrawMode(DrawMode.FILL);
        view.setMaterial(material(color));
        return view;
    }

    private static Text createLabel(String text, double x, double y, double z, String color, int size) {
        Text label = new Text(text);
        label.setFill(Color.web(color));
        label.setFont(Font.font("Consolas", size));
        label.setTranslateX(x);
        label.setTranslateY(y);
        label.setTranslateZ(z);
        return label;
    }

    private static Sphere sphere(double radius, String color) {
        // Helper visual para crear esferas con material uniforme.
        Sphere sphere = new Sphere(radius);
        sphere.setMaterial(material(color));
        return sphere;
    }

    private static PhongMaterial material(String color) {
        // Material comun para mantener brillo y color consistentes en el mapa.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web(color));
        material.setSpecularColor(Color.web("#ffffff"));
        material.setSpecularPower(10);
        return material;
    }

    /** Material mate para que la iluminacion no fragmente visualmente las lineas. */
    private static PhongMaterial trajectoryMaterial(String color) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web(color));
        material.setSpecularColor(Color.web(color));
        material.setSpecularPower(1);
        return material;
    }

    private static PhongMaterial celestialMaterial(String color, String softHighlight) {
        // Material de bajo brillo para dar volumen sin producir manchas blancas duras.
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web(color));
        material.setSpecularColor(Color.web(softHighlight));
        material.setSpecularPower(5);
        return material;
    }

    /**
     * Carga una textura empacada en resources y conserva un material de respaldo.
     * Quien llama: createEarth() y createMoon(). Si el recurso faltara, el mapa
     * seguiria abriendo con el color anterior en vez de lanzar una excepcion.
     */
    private static PhongMaterial texturedCelestialMaterial(
            String resourcePath,
            String tint,
            String softHighlight) {
        var resource = MissionMap3D.class.getResource(resourcePath);
        if (resource == null) {
            return celestialMaterial(tint, softHighlight);
        }
        Image texture = new Image(resource.toExternalForm(), false);
        if (texture.isError()) {
            return celestialMaterial(tint, softHighlight);
        }
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web(tint));
        material.setDiffuseMap(texture);
        material.setSpecularColor(Color.web(softHighlight));
        material.setSpecularPower(4);
        return material;
    }

    /** Centro horizontal y distancia calculados juntos para un encuadre estable. */
    private record CameraFraming(double centerX, double distance) {
    }
}
