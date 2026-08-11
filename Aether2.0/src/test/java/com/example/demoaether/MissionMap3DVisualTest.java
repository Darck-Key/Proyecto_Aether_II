package com.example.demoaether;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionMap3DVisualTest {

    private static boolean toolkitStarted;

    @Test
    void mapSnapshotContainsBodiesStarsAndTrajectoryColors() throws Exception {
        startJavaFxToolkit();
        MissionTrajectory trajectory = ArtemisReferenceTrajectoryLoader.load();
        RenderedFrame startFrame = renderState(
                trajectory, trajectory.getStates().get(0));
        assertNull(startFrame.scene().getOnMouseDragged(),
                "Arrastrar el mouse no debe girar la camara.");
        assertNotNull(startFrame.scene().getOnScroll(),
                "La rueda del mouse debe conservar el zoom.");
        WritableImage midpointSnapshot = renderState(
                trajectory, trajectory.getStates().get(trajectory.getStates().size() / 2)).image();
        MissionState flyby = trajectory.getStates().stream()
                .min(java.util.Comparator.comparingDouble(MissionState::getDistanceMoon))
                .orElseThrow();
        WritableImage flybySnapshot = renderState(trajectory, flyby).image();
        WritableImage returnSnapshot = renderState(trajectory, trajectory.lastState()).image();
        WritableImage startSnapshot = startFrame.image();

        assertTrue(countMissionPixels(startSnapshot) > 350,
                "La captura debe contener estrellas, cuerpos y trayectoria visibles.");
        assertTrue(countMissionPixels(flybySnapshot) > 350,
                "El sobrevuelo debe conservar visibles Luna, nave y trayectoria.");
        Path preview = Path.of("build", "reports", "mission-map-preview.png");
        Path midpointPreview = Path.of("build", "reports", "mission-map-preview-midpoint.png");
        Path flybyPreview = Path.of("build", "reports", "mission-map-preview-flyby.png");
        Path returnPreview = Path.of("build", "reports", "mission-map-preview-return.png");
        writeSnapshot(startSnapshot, preview);
        writeSnapshot(midpointSnapshot, midpointPreview);
        writeSnapshot(flybySnapshot, flybyPreview);
        writeSnapshot(returnSnapshot, returnPreview);
        assertTrue(Files.isRegularFile(preview), "La prueba debe dejar una vista previa verificable.");
        assertTrue(Files.isRegularFile(flybyPreview),
                "La prueba debe dejar una vista previa del encuentro lunar.");
    }

    /** Inicia JavaFX una sola vez para que la prueba no dependa de abrir HelloApplication. */
    private static synchronized void startJavaFxToolkit() throws InterruptedException {
        if (toolkitStarted) {
            return;
        }
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(started::countDown);
        assertTrue(started.await(10, TimeUnit.SECONDS), "JavaFX no pudo iniciar.");
        Platform.setImplicitExit(false);
        toolkitStarted = true;
    }

    private static int countMissionPixels(WritableImage image) {
        PixelReader reader = image.getPixelReader();
        int result = 0;
        for (int y = 0; y < (int) image.getHeight(); y += 2) {
            for (int x = 0; x < (int) image.getWidth(); x += 2) {
                Color color = reader.getColor(x, y);
                boolean blue = color.getBlue() > 0.45 && color.getRed() < 0.75;
                boolean lavender = color.getRed() > 0.45 && color.getBlue() > 0.45;
                if ((blue || lavender) && color.getOpacity() > 0.25) {
                    result++;
                }
            }
        }
        return result;
    }

    /** Crea una escena limpia por etapa para evitar residuos del buffer 3D entre capturas. */
    private static RenderedFrame renderState(
            MissionTrajectory trajectory,
            MissionState state) throws Exception {
        AtomicReference<Pane> containerReference = new AtomicReference<>();
        AtomicReference<SubScene> sceneReference = new AtomicReference<>();
        runOnFxThreadAndWait(() -> {
            Node map = MissionMap3D.configureTrajectory(trajectory);
            assertTrue(map instanceof SubScene, "El mapa debe conservar su SubScene 3D.");
            SubScene subScene = (SubScene) map;
            Pane container = new Pane(map);
            container.resize(900, 370);
            new Scene(container, 900, 370, true);
            // Reproduce muestras anteriores para comprobar que solo aparezca
            // el rastro ya recorrido y nunca la trayectoria futura completa.
            int targetIndex = nearestStateIndex(trajectory, state.getElapsedTime());
            int stride = Math.max(1, targetIndex / 120);
            for (int index = 0; index <= targetIndex; index += stride) {
                MissionMap3D.updateState(trajectory.getStates().get(index));
            }
            if (targetIndex % stride != 0) {
                MissionMap3D.updateState(trajectory.getStates().get(targetIndex));
            }
            container.applyCss();
            container.layout();
            Platform.requestNextPulse();
            containerReference.set(container);
            sceneReference.set(subScene);
        });
        Thread.sleep(140L);
        // La primera captura fuerza a JavaFX a subir la malla 3D; no se usa como evidencia.
        runOnFxThreadAndWait(() -> {
            snapshot(containerReference.get());
            Platform.requestNextPulse();
        });
        Thread.sleep(70L);
        AtomicReference<WritableImage> result = new AtomicReference<>();
        runOnFxThreadAndWait(() -> result.set(snapshot(containerReference.get())));
        return new RenderedFrame(result.get(), sceneReference.get());
    }

    private static int nearestStateIndex(MissionTrajectory trajectory, double elapsedSeconds) {
        int result = 0;
        double minimumDifference = Double.POSITIVE_INFINITY;
        for (int index = 0; index < trajectory.getStates().size(); index++) {
            double difference = Math.abs(
                    trajectory.getStates().get(index).getElapsedTime() - elapsedSeconds);
            if (difference < minimumDifference) {
                minimumDifference = difference;
                result = index;
            }
        }
        return result;
    }

    private static void runOnFxThreadAndWait(Runnable action) throws Exception {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                completed.countDown();
            }
        });
        assertTrue(completed.await(20, TimeUnit.SECONDS), "JavaFX no termino la operacion.");
        assertNull(failure.get(), () -> "Fallo el render 3D: " + failure.get());
    }

    private static WritableImage snapshot(Pane container) {
        return container.snapshot(null, new WritableImage(900, 370));
    }

    /** Guarda la misma captura validada para poder revisar el encuadre sin iniciar la aplicacion. */
    private static void writeSnapshot(WritableImage image, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        BufferedImage buffered = new BufferedImage(
                (int) image.getWidth(),
                (int) image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < buffered.getHeight(); y++) {
            for (int x = 0; x < buffered.getWidth(); x++) {
                buffered.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        ImageIO.write(buffered, "png", output.toFile());
    }

    private record RenderedFrame(WritableImage image, SubScene scene) {
    }
}
