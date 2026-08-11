package com.example.demoaether;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controlador principal de la interfaz JavaFX AETHER.
 *
 * <p>Recibe eventos del FXML, coordina MissionSimulator, ReportGenerator,
 * AetherRepository, MissionMap3D y NetworkQualityService.</p>
 */
public class HelloController implements SimulationListener {

    @FXML private Label lblMision;
    @FXML private Label lblTiempo;
    @FXML private Label lblEstado;
    @FXML private Label lblConectividad;
    @FXML private Label lblVelocidad;
    @FXML private Label lblAltitud;
    @FXML private Label lblDistanciaLuna;
    @FXML private Label lblCombustible;
    @FXML private Label lblTiempoLanzamiento;
    @FXML private Label lblEstadoSistema;
    @FXML private Label lblOrekitStatus;
    @FXML private Label lblLastCalculation;
    @FXML private Label lblLastReport;
    @FXML private Label lblPersistenceStatus;
    @FXML private Label lblRecentEvent1;
    @FXML private Label lblRecentEvent2;
    @FXML private Label lblRecentEvent3;
    @FXML private Label lblCurrentPhase;
    @FXML private Label lblNextEvent;
    @FXML private ProgressBar pbCombustible;
    @FXML private Button btnOpciones;
    @FXML private Button btnReportes;
    @FXML private Pane missionMapContainer;

    private MissionConfig config;
    private MissionSimulator simulator;
    private MissionState lastState;
    private Thread simulationThread;
    private TrajectoryPlayback trajectoryPlayback;
    private boolean orbitCalculated;
    private boolean reportGenerated;
    private boolean simulationSaved;
    private double lastMapRenderSecond = -1;
    private double activeMissionDurationSeconds = 1.0;
    private int activeTrajectoryPointCount = 1;
    private MissionPhaseTimeline activeMissionTimeline;
    private ScheduledExecutorService networkMonitor;
    private final AetherRepository repository = RepositoryFactory.createRepository();

    // Clave comun de acceso solicitada para todos los operadores del simulador.
    private static final String DEFAULT_ACCESS_PASSWORD = "27201";

    // Usuario activo de la sesion; se registra en eventos y se envia al PDF de reporte.
    private String currentUser = "operador";

    /**
     * Inicializa la pantalla principal, carga configuracion, activa mapa, red y login.
     */
    @FXML
    public void initialize() {
        // Punto de entrada de la interfaz: carga configuracion, prepara telemetria, mapa, MySQL y login.
        // Llama a RepositoryFactory por medio de repository, inicializa MissionMap3D y activa NetworkQualityService.
        Locale.setDefault(Locale.US);
        config = MissionPresets.migrateLegacyArtemisII(repository.loadLastMissionConfig());
        if (config.getSimulationHours() <= 0) {
            config.setSimulationHours(10);
        }
        if (config.getSimulationStepSeconds() <= 0) {
            config.setSimulationStepSeconds(60);
        }
        if (config.getSimulationSpeed() <= 0) {
            config.setSimulationSpeed(60);
        }

        lblMision.setText("ARTEMIS II");
        lblEstado.setText("LISTO");
        lblConectividad.setText("VERIFICANDO");
        lblTiempo.setText("00:00:00");
        lblVelocidad.setText("--- km/s");
        lblAltitud.setText("--- km");
        lblDistanciaLuna.setText("--- km");
        lblCombustible.setText("100 %");

        if (lblTiempoLanzamiento != null) {
            lblTiempoLanzamiento.setText("00:00:00");
        }
        if (lblEstadoSistema != null) {
            lblEstadoSistema.setText("LISTO");
        }
        if (lblOrekitStatus != null) {
            lblOrekitStatus.setText("Orekit: pendiente");
        }
        if (lblLastCalculation != null) {
            lblLastCalculation.setText("Ultimo calculo: pendiente");
        }
        if (lblLastReport != null) {
            lblLastReport.setText("Ultimo reporte: pendiente");
        }
        if (lblPersistenceStatus != null) {
            lblPersistenceStatus.setText(repository.describeStatus());
        }
        if (pbCombustible != null) {
            pbCombustible.setProgress(1.0);
        }
        updateMissionPhase(null);
        if (btnOpciones != null) {
            btnOpciones.setText("Opciones");
        }
        if (btnReportes != null) {
            btnReportes.setText("Reportes");
        }

        initializeMissionMap();
        refreshRecentEvents();
        startNetworkMonitor();
        Platform.runLater(this::requestLogin);
    }

    @FXML
    private void onOpcionesClicked() {
        // Ventana de control de mision.
        // Lee parametros orbitales, guarda MissionConfig en el repositorio y llama a calculo/simulacion segun el boton.
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Opciones de simulacion");
        dialog.setHeaderText(null);

        TextField altitude = field(config.getInitialAltitude());
        TextField velocity = field(config.getInitialVelocity());
        TextField inclination = field(config.getInclination());
        TextField eccentricity = field(config.getEccentricity());
        TextField argument = field(config.getArgumentOfPerigee());
        TextField tliDeltaV = field(config.getTliDeltaVKms());
        TextField tliOffset = field(config.getTliBurnOffsetHours());
        TextField hours = field(config.getSimulationHours());
        Slider timeScale = new Slider(1, 1000, config.getSimulationSpeed());
        timeScale.setShowTickMarks(true);
        timeScale.setShowTickLabels(true);
        timeScale.setMajorTickUnit(250);
        timeScale.setBlockIncrement(25);
        TextField missionName = new TextField(config.getMissionName());
        TextField spacecraftName = new TextField(config.getSpacecraftName());
        TextField mass = field(config.getSpacecraftMass());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-background-color: #120B25; -fx-padding: 4 0 0 0;");
        grid.addRow(0, new Label("Mision"), missionName);
        grid.addRow(1, new Label("Nave"), spacecraftName);
        grid.addRow(2, new Label("Masa nave (kg)"), mass);
        grid.addRow(3, new Label("Altitud inicial (km)"), altitude);
        grid.addRow(4, new Label("Velocidad inicial (km/s)"), velocity);
        grid.addRow(5, new Label("Inclinacion (grados)"), inclination);
        grid.addRow(6, new Label("Excentricidad"), eccentricity);
        grid.addRow(7, new Label("Argumento del perigeo (grados)"), argument);
        grid.addRow(8, new Label("Delta-v TLI (km/s)"), tliDeltaV);
        grid.addRow(9, new Label("Encendido TLI (horas)"), tliOffset);
        grid.addRow(10, new Label("Duracion simulada (horas)"), hours);
        grid.addRow(11, new Label("Escala de tiempo (1x-1000x)"), timeScale);
        dialog.getDialogPane().setContent(createDialogContent("PARAMETROS ORBITALES Y CONTROLES DE OREKIT", grid));

        ButtonType calculate = new ButtonType("Calcular/Recalcular", ButtonBar.ButtonData.APPLY);
        ButtonType start = new ButtonType("Iniciar", ButtonBar.ButtonData.OK_DONE);
        ButtonType pause = new ButtonType("Pausar/Reanudar", ButtonBar.ButtonData.LEFT);
        ButtonType stop = new ButtonType("Detener", ButtonBar.ButtonData.NO);
        ButtonType reset = new ButtonType("Reiniciar", ButtonBar.ButtonData.OTHER);
        ButtonType demo = new ButtonType("Demo rapida", ButtonBar.ButtonData.OTHER);
        ButtonType close = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(calculate, start, demo, pause, stop, reset, close);
        styleDialog(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == close) {
            return;
        }

        try {
            if (result.get() == pause) {
                togglePause();
                return;
            }
            if (result.get() == stop) {
                stopSimulation();
                return;
            }
            if (result.get() == reset) {
                resetSimulation();
                return;
            }
            if (result.get() == demo) {
                // La demo conserva todos los parametros fisicos; solo comprime el reloj visual.
                config = readConfig(missionName, spacecraftName, mass, altitude, velocity,
                        inclination, eccentricity, argument, tliDeltaV, tliOffset, hours, timeScale);
                repository.saveMissionConfig(config, LocalDateTime.now());
                startSimulation(true);
                return;
            }

            MissionConfig updated = readConfig(missionName, spacecraftName, mass, altitude, velocity,
                    inclination, eccentricity, argument, tliDeltaV, tliOffset, hours, timeScale);
            config = updated;
            repository.saveMissionConfig(config, LocalDateTime.now());

            if (result.get() == start) {
                startSimulation(false);
                return;
            }
            calculateOrbit(true);
        } catch (IllegalArgumentException exception) {
            showError("Datos invalidos", safeExceptionMessage(exception));
        }
    }

    @FXML
    private void onReportesClicked() {
        // Menu central de reportes: desde aqui se genera PDF, se consulta historial o se abren reportes guardados.
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Reportes");
        dialog.setHeaderText(null);

        TextArea summary = new TextArea(reportSummary());
        summary.setEditable(false);
        summary.setWrapText(true);
        summary.setPrefColumnCount(54);
        summary.setPrefRowCount(7);
        styleReportTextArea(summary);
        dialog.getDialogPane().setContent(createDialogContent("GESTION DE REPORTES E HISTORIAL DE MISION", summary));

        ButtonType generate = new ButtonType("Generar PDF", ButtonBar.ButtonData.APPLY);
        ButtonType history = new ButtonType("Historial", ButtonBar.ButtonData.LEFT);
        ButtonType savedReports = new ButtonType("Reportes guardados", ButtonBar.ButtonData.LEFT);
        ButtonType openLast = new ButtonType("Abrir ultimo", ButtonBar.ButtonData.OK_DONE);
        ButtonType close = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(generate, history, savedReports, openLast, close);
        styleDialog(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == close) {
            return;
        }

        if (result.get() == history) {
            // Si el subdialogo devuelve true, el usuario presiono Atras y se reabre este menu.
            if (showCalculationHistory()) {
                onReportesClicked();
            }
            return;
        }
        if (result.get() == savedReports) {
            // Mantiene la navegacion dentro de Reportes sin obligar a cerrar y entrar otra vez.
            if (showSavedReports()) {
                onReportesClicked();
            }
            return;
        }
        if (result.get() == openLast) {
            openLastReport();
            return;
        }
        generateReport();
    }

    private void generateReport() {
        // Genera un PDF del ultimo estado orbital calculado.
        // Llama a ReportGenerator para crear el archivo y a AetherRepository para guardar el registro del reporte.
        if (!orbitCalculated || lastState == null) {
            showInfo("Reportes", "Primero calcula o ejecuta una simulacion para generar un reporte.");
            return;
        }
        try {
            LocalDateTime generatedAt = LocalDateTime.now();
            // Lee el historial desde el repositorio activo: MySQL si esta conectado, memoria local si esta pendiente.
            List<CalculationHistoryEntry> history = repository.findRecentCalculations(8);

            // ReportGenerator recibe currentUser para que el PDF indique quien ejecuto la simulacion.
            File pdf = ReportGenerator.generatePdf(config, lastState, generatedAt, history, currentUser);
            repository.saveReport(pdf, config, lastState, generatedAt);
            reportGenerated = true;
            if (lblLastReport != null) {
                lblLastReport.setText("Ultimo reporte: " + generatedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                        " (" + history.size() + " historicos)");
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdf);
            } else {
                showInfo("Reporte generado", pdf.getAbsolutePath() + "\nHistorial incluido: " + history.size() + " calculos.");
            }
        } catch (IOException exception) {
            showError("No se pudo generar el reporte", safeExceptionMessage(exception));
        }
    }

    @FXML
    private void onMapZoomInClicked() {
        MissionMap3D.zoomIn();
        refreshMissionMap();
    }

    @FXML
    private void onMapZoomOutClicked() {
        MissionMap3D.zoomOut();
        refreshMissionMap();
    }

    @FXML
    private void onMapResetCameraClicked() {
        MissionMap3D.resetCamera();
        refreshMissionMap();
    }

    private void calculateOrbit(boolean showSuccessMessage) {
        // Calculo puntual de orbita.
        // Llama a OrekitInitializer, MissionSimulator.calculateInitialState y luego persiste en AetherRepository.
        try {
            OrekitInitializer.initialize();
            lastState = MissionSimulator.calculateInitialState(config);
            LocalDateTime executedAt = LocalDateTime.now();
            repository.saveCalculation(config, lastState, executedAt);
            repository.saveMissionEvent("CALCULO_ORBITAL", "Orbita calculada con Orekit", executedAt);
            refreshRecentEvents();
            applyMissionState(lastState);
            orbitCalculated = true;
            reportGenerated = false;
            lblEstado.setText("ORBITA CALCULADA");
            setSystemState("ORBITA CALCULADA");
            if (lblOrekitStatus != null) {
                lblOrekitStatus.setText("Orekit: datos cargados");
            }
            if (lblLastCalculation != null) {
                lblLastCalculation.setText("Ultimo calculo: " + executedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
            if (showSuccessMessage) {
                showInfo("Calculo orbital", "Orbita calculada correctamente con Orekit.");
            }
        } catch (Exception exception) {
            orbitCalculated = false;
            lblEstado.setText("ERROR");
            setSystemState("ERROR");
            if (lblOrekitStatus != null) {
                lblOrekitStatus.setText("Orekit: error");
            }
            showError("Error de calculo", safeExceptionMessage(exception));
        }
    }

    private void startSimulation(boolean quickDemo) {
        // Precalcula fuera de JavaFX y reproduce en AnimationTimer dentro de JavaFX.
        // La bandera quickDemo solo cambia la duracion de pared, nunca MissionTrajectory.
        if (simulator != null && simulator.isRunning()) {
            showInfo("Simulacion", "La simulacion ya esta en ejecucion.");
            return;
        }

        lblEstado.setText("PREPARANDO");
        setSystemState("PREPARANDO");
        simulationSaved = false;
        if (lblOrekitStatus != null) {
            lblOrekitStatus.setText("Simulacion: iniciando");
        }
        if (lblCurrentPhase != null) {
            lblCurrentPhase.setText("Fase actual: Preparando simulacion");
        }
        if (lblNextEvent != null) {
            lblNextEvent.setText("Siguiente: primer estado orbital");
        }

        MissionMap3D.resetTrail();
        lastMapRenderSecond = -1;
        MissionSimulator preparedSimulator = new MissionSimulator(config);
        preparedSimulator.setSimulationListener(this);
        simulator = preparedSimulator;
        simulationThread = new Thread(() -> {
            try {
                MissionTrajectory trajectory = preparedSimulator.prepareTrajectory();
                Platform.runLater(() -> beginTrajectoryPlayback(preparedSimulator, trajectory, quickDemo));
            } catch (Exception exception) {
                preparedSimulator.failSimulation(exception);
            }
        }, "aether-orekit-precalculation");
        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    /**
     * Conecta una trayectoria ya calculada al mapa persistente y al reloj JavaFX.
     * Lo llama el hilo de precalculo mediante Platform.runLater().
     */
    private void beginTrajectoryPlayback(
            MissionSimulator preparedSimulator,
            MissionTrajectory trajectory,
            boolean quickDemo) {
        if (simulator != preparedSimulator || !preparedSimulator.isRunning()) {
            return;
        }
        try {
            activeMissionDurationSeconds = Math.max(1.0, trajectory.getDurationSeconds());
            activeTrajectoryPointCount = Math.max(1, trajectory.getStates().size());
            activeMissionTimeline = MissionPhaseTimeline.from(trajectory);
            if (missionMapContainer != null) {
                missionMapContainer.getChildren().setAll(MissionMap3D.configureTrajectory(trajectory));
            }
        } catch (RuntimeException exception) {
            preparedSimulator.failSimulation(exception);
            return;
        }
        lblEstado.setText("SIMULANDO");
        setSystemState("SIMULANDO");
        if (lblOrekitStatus != null) {
            lblOrekitStatus.setText(ArtemisReferenceTrajectoryLoader.supports(config)
                    ? "Orekit: efemeride NASA"
                    : "Orekit: trayectoria propagada");
        }

        trajectoryPlayback = new TrajectoryPlayback(
                trajectory,
                config.getSimulationSpeed(),
                quickDemo,
                preparedSimulator::publishState,
                preparedSimulator::completeSimulation
        );
        recordEventAsync("SIMULACION_INICIADA",
                quickDemo ? "Demo rapida con trayectoria orbital completa" : "Simulacion orbital iniciada");
        trajectoryPlayback.start();
    }

    private void togglePause() {
        // Pausa o reanuda MissionSimulator y actualiza las etiquetas de estado de la interfaz.
        if (simulator == null || !simulator.isRunning()) {
            showInfo("Simulacion", "No hay una simulacion activa para pausar o reanudar.");
            return;
        }
        boolean paused = trajectoryPlayback != null
                ? trajectoryPlayback.togglePaused()
                : !simulator.isPaused();
        simulator.setPaused(paused);
        if (paused) {
            lblEstado.setText("PAUSADA");
            setSystemState("PAUSADA");
        } else {
            lblEstado.setText("SIMULANDO");
            setSystemState("SIMULANDO");
        }
    }

    private void stopSimulation() {
        // Detiene MissionSimulator y guarda una captura final por medio del repositorio activo.
        if (simulator != null) {
            simulator.stopSimulation();
        }
        if (trajectoryPlayback != null) {
            trajectoryPlayback.cancel();
            trajectoryPlayback = null;
        }
        lblEstado.setText("DETENIDA");
        setSystemState("DETENIDA");
        if (lblCurrentPhase != null) {
            lblCurrentPhase.setText("Fase actual: Detenida");
        }
        if (lblNextEvent != null) {
            lblNextEvent.setText("Siguiente: Reiniciar o generar reporte");
        }
        saveSimulationSnapshotAsync("SIMULACION_DETENIDA");
        recordEventAsync("SIMULACION_DETENIDA", "Simulacion detenida por el usuario");
    }

    private void resetSimulation() {
        // Limpia telemetria, flags y mapa para regresar la interfaz a estado LISTO.
        stopSimulation();
        lastState = null;
        activeMissionDurationSeconds = 1.0;
        activeTrajectoryPointCount = 1;
        activeMissionTimeline = null;
        orbitCalculated = false;
        reportGenerated = false;
        simulationSaved = false;
        lblTiempo.setText("00:00:00");
        lblVelocidad.setText("--- km/s");
        lblAltitud.setText("--- km");
        lblDistanciaLuna.setText("--- km");
        lblCombustible.setText("100 %");
        if (lblTiempoLanzamiento != null) {
            lblTiempoLanzamiento.setText("00:00:00");
        }
        if (pbCombustible != null) {
            pbCombustible.setProgress(1.0);
        }
        if (lblLastCalculation != null) {
            lblLastCalculation.setText("Ultimo calculo: pendiente");
        }
        if (lblLastReport != null) {
            lblLastReport.setText("Ultimo reporte: pendiente");
        }
        lblEstado.setText("LISTO");
        setSystemState("LISTO");
        lastMapRenderSecond = -1;
        initializeMissionMap();
    }

    /**
     * Callback llamado por MissionSimulator cuando inicia la simulacion.
     */
    @Override
    public void onSimulationStarted() {
        // Callback de precalculo: todavia no mueve la nave hasta recibir la trayectoria completa.
        Platform.runLater(() -> {
            lblEstado.setText("PREPARANDO");
            setSystemState("PREPARANDO");
            if (lblOrekitStatus != null) {
                lblOrekitStatus.setText("Orekit: precalculando");
            }
            recordEventAsync("PRECALCULO_INICIADO", "Orekit prepara la trayectoria orbital");
        });
    }

    /**
     * Callback llamado por MissionSimulator por cada estado orbital calculado.
     *
     * @param state telemetria actual de la nave
     */
    @Override
    public void onStateUpdated(MissionState state) {
        // AnimationTimer ya llama desde JavaFX; las pruebas heredadas pueden llamar desde otro hilo.
        Runnable update = () -> {
            applyMissionState(state);
            if (lblLastCalculation != null) {
                lblLastCalculation.setText("Simulando: " + formatTime((int) state.getElapsedTime()));
            }
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            Platform.runLater(update);
        }
    }

    /**
     * Callback llamado por MissionSimulator cuando termina la trayectoria.
     */
    @Override
    public void onSimulationFinished() {
        // Conserva el ultimo estado visible; Reiniciar es la unica accion que vuelve a T+0.
        Runnable finish = () -> {
            lblEstado.setText("COMPLETADA");
            setSystemState("COMPLETADA");
            if (lblCurrentPhase != null) {
                lblCurrentPhase.setText("Fase actual: Completada");
            }
            if (lblNextEvent != null) {
                lblNextEvent.setText("Siguiente: generar reporte");
            }
            saveSimulationSnapshotAsync("SIMULACION_COMPLETADA");
            recordEventAsync("SIMULACION_COMPLETADA", "Simulacion orbital completada");
            completeSimulationPlayback();
        };
        if (Platform.isFxApplicationThread()) {
            finish.run();
        } else {
            Platform.runLater(finish);
        }
    }

    /**
     * Callback llamado por MissionSimulator si ocurre un error.
     *
     * @param exception error producido durante la simulacion
     */
    @Override
    public void onSimulationError(Exception exception) {
        // Callback de MissionSimulator: registra el error de propagacion y lo muestra al usuario.
        Platform.runLater(() -> {
            if (trajectoryPlayback != null) {
                trajectoryPlayback.cancel();
                trajectoryPlayback = null;
            }
            simulationThread = null;
            lblEstado.setText("ERROR");
            setSystemState("ERROR");
            if (lblOrekitStatus != null) {
                lblOrekitStatus.setText("Orekit: error");
            }
            String message = safeExceptionMessage(exception);
            recordEventAsync("SIMULACION_ERROR", message);
            showError("Error de simulacion", message);
        });
    }

    /**
     * Aplica un estado de mision a la telemetria visible y al mapa orbital.
     *
     * @param state estado calculado por Orekit o por la trayectoria de respaldo
     */
    public void applyMissionState(MissionState state) {
        // Traduce MissionState a etiquetas de telemetria y refresca MissionMap3D.
        lastState = state;
        orbitCalculated = true;
        TelemetryViewModel telemetry = TelemetryViewModel.fromState(state);
        lblTiempo.setText(telemetry.getElapsedTimeText());
        lblVelocidad.setText(telemetry.getVelocityText());
        lblAltitud.setText(telemetry.getAltitudeText());
        lblDistanciaLuna.setText(telemetry.getMoonDistanceText());

        if (lblTiempoLanzamiento != null) {
            lblTiempoLanzamiento.setText(telemetry.getElapsedTimeText());
        }
        updateMissionPhase(state);

        double fuel = calculateFuelLevel(state);
        lblCombustible.setText(String.format(Locale.US, "%.0f %%", fuel * 100));
        if (pbCombustible != null) {
            pbCombustible.setProgress(fuel);
        }

        updateMissionMapIfNeeded(state);
    }

    private double calculateFuelLevel(MissionState state) {
        // Modelo visual de combustible: no pretende ser masa real de propelente.
        // Lo llama applyMissionState() para que la telemetria muestre consumo visible por fases.
        double totalSeconds = Math.max(1.0, activeMissionDurationSeconds);
        double progress = state == null ? 0.0 : Math.min(1.0, Math.max(0.0, state.getElapsedTime() / totalSeconds));
        double cruiseConsumption = progress * 0.10;
        double launchBurn = progress >= 0.03 ? 0.08 : progress / 0.03 * 0.08;
        double tliBurn = progress >= 0.20 ? 0.10 : Math.max(0.0, (progress - 0.10) / 0.10) * 0.10;
        double correctionBurns = progress >= 0.72 ? 0.08 : Math.max(0.0, (progress - 0.30) / 0.42) * 0.08;
        double reentryReserveUse = progress >= 0.94 ? Math.max(0.0, (progress - 0.94) / 0.06) * 0.04 : 0.0;
        return Math.max(0.55, 1.0 - launchBurn - tliBurn - correctionBurns - cruiseConsumption - reentryReserveUse);
    }

    private void updateMissionMapIfNeeded(MissionState state) {
        if (state == null) {
            return;
        }
        try {
            updateMissionMap(state);
            lastMapRenderSecond = state.getElapsedTime();
        } catch (RuntimeException exception) {
            if (lblOrekitStatus != null) {
                lblOrekitStatus.setText("Mapa 3D: pendiente");
            }
        }
    }

    private double mapRefreshIntervalSeconds() {
        // Ajusta cada cuanto se redibuja el mapa 3D segun la escala seleccionada.
        // Lo llama updateMissionMapIfNeeded(); busca suficientes frames para movimiento fluido sin saturar JavaFX.
        int speed = Math.max(1, config.getSimulationSpeed());
        double missionSeconds = Math.max(1.0, config.getSimulationHours() * 3600.0);
        if (speed >= 900) {
            return Math.max(12.0, missionSeconds / 140.0);
        }
        if (speed >= 500) {
            return Math.max(18.0, missionSeconds / 120.0);
        }
        if (speed >= 200) {
            return Math.max(24.0, missionSeconds / 100.0);
        }
        return Math.max(30.0, missionSeconds / 80.0);
    }

    private void updateMissionPhase(MissionState state) {
        double elapsed = state == null ? 0 : state.getElapsedTime();
        double totalSeconds = Math.max(1.0, activeMissionDurationSeconds);
        double progress = Math.min(1.0, Math.max(0.0, elapsed / totalSeconds));
        String phase;
        String next;
        if (state == null) {
            phase = "Espera";
            next = "Lanzamiento";
        } else if (progress >= 1.0) {
            phase = "Completada";
            next = "generar reporte";
        } else {
            // MissionPhaseTimeline detecta los hitos en la efemeride; no usa porcentajes arbitrarios.
            MissionPhaseTimeline.Phase currentPhase = activeMissionTimeline == null
                    ? MissionPhaseTimeline.Phase.ORBITA_TERRESTRE
                    : activeMissionTimeline.phaseAt(elapsed);
            phase = currentPhase.displayName();
            next = currentPhase.nextEvent();
        }
        if (lblCurrentPhase != null) {
            lblCurrentPhase.setText("Paso " + formatStep(state) + " - " + phase);
        }
        if (lblNextEvent != null) {
            lblNextEvent.setText("Siguiente: " + next);
        }
    }

    private String formatStep(MissionState state) {
        if (state == null) {
            return "0/0";
        }
        int totalSteps = Math.max(1, activeTrajectoryPointCount - 1);
        double progress = Math.min(1.0,
                Math.max(0.0, state.getElapsedTime() / Math.max(1.0, activeMissionDurationSeconds)));
        int currentStep = Math.min(totalSteps, Math.max(0, (int) Math.round(progress * totalSteps)));
        return currentStep + "/" + totalSteps;
    }

    private MissionConfig readConfig(TextField missionName, TextField spacecraftName, TextField mass,
                                     TextField altitude, TextField velocity, TextField inclination,
                                     TextField eccentricity, TextField argument, TextField tliDeltaV,
                                     TextField tliOffset, TextField hours, Slider timeScale) {
        MissionConfig updated = new MissionConfig();
        updated.setMissionName(readRequiredText(missionName, "Mision"));
        updated.setSpacecraftName(readRequiredText(spacecraftName, "Nave"));
        updated.setSpacecraftMass(parseRequired(mass, "Masa nave"));
        updated.setInitialAltitude(parseRequired(altitude, "Altitud inicial"));
        updated.setInitialVelocity(parseRequired(velocity, "Velocidad inicial"));
        updated.setInclination(parseRequired(inclination, "Inclinacion"));
        updated.setEccentricity(parseRequired(eccentricity, "Excentricidad"));
        updated.setArgumentOfPerigee(parseRequired(argument, "Argumento del perigeo"));
        updated.setTliDeltaVKms(parseRequired(tliDeltaV, "Delta-v TLI"));
        updated.setTliBurnOffsetHours(parseRequired(tliOffset, "Encendido TLI"));
        updated.setSimulationHours((int) parseRequired(hours, "Duracion simulada"));
        updated.setSimulationStepSeconds(60);
        updated.setSimulationSpeed((int) Math.round(timeScale.getValue()));
        updated.validate();
        return updated;
    }

    private MissionConfig createDemoConfig() {
        // LEGACY, no invocado: Demo rapida ya no modifica parametros orbitales ni duracion.
        MissionConfig demo = new MissionConfig();
        demo.setMissionName(config.getMissionName());
        demo.setSpacecraftName(config.getSpacecraftName());
        demo.setSpacecraftMass(config.getSpacecraftMass());
        demo.setInitialAltitude(config.getInitialAltitude());
        demo.setInitialVelocity(config.getInitialVelocity());
        demo.setInclination(config.getInclination());
        demo.setEccentricity(config.getEccentricity());
        demo.setArgumentOfPerigee(config.getArgumentOfPerigee());
        demo.setTliDeltaVKms(config.getTliDeltaVKms());
        demo.setTliBurnOffsetHours(config.getTliBurnOffsetHours());
        demo.setSimulationHours(config.getSimulationHours());
        demo.setSimulationStepSeconds(config.getSimulationStepSeconds());
        demo.setSimulationSpeed(1000);
        demo.setSaveReports(config.isSaveReports());
        demo.validate();
        return demo;
    }

    private void requestLogin() {
        // Control de acceso inicial.
        // Valida usuario + clave comun DEFAULT_ACCESS_PASSWORD y guarda currentUser para eventos y reportes.
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Acceso AETHER");
        dialog.setHeaderText(null);

        TextField user = new TextField(currentUser);
        user.setPromptText("Usuario");
        PasswordField password = new PasswordField();
        password.setPromptText("Contrasena");
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setStyle("-fx-background-color: #0B0616; -fx-padding: 18 18 12 18;");

        Label title = new Label("ACCESO AETHER");
        title.setStyle("-fx-text-fill: #6FD6FF; -fx-font-size: 19px; -fx-font-weight: bold;");
        Label subtitle = new Label("Identificacion del operador");
        subtitle.setStyle("-fx-text-fill: #D8CAD5; -fx-font-size: 12px;");

        Label userLabel = new Label("Usuario");
        Label passwordLabel = new Label("Contrasena");
        userLabel.setStyle("-fx-text-fill: #E7D8F0; -fx-font-size: 14px;");
        passwordLabel.setStyle("-fx-text-fill: #E7D8F0; -fx-font-size: 14px;");
        user.setStyle(loginFieldStyle());
        password.setStyle(loginFieldStyle());

        grid.add(title, 0, 0, 2, 1);
        grid.add(subtitle, 0, 1, 2, 1);
        grid.addRow(2, userLabel, user);
        grid.addRow(3, passwordLabel, password);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        styleDialog(dialog);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            lblEstado.setText("BLOQUEADO");
            setSystemState("BLOQUEADO");
            return;
        }
        String userName = user.getText() == null ? "" : user.getText().trim();
        if (userName.isBlank() || password.getText() == null || password.getText().isBlank()) {
            showError("Acceso invalido", "Debes indicar usuario y contrasena.");
            requestLogin();
            return;
        }
        // Clave unica de acceso definida para todos los operadores del simulador.
        if (!DEFAULT_ACCESS_PASSWORD.equals(password.getText())) {
            showError("Acceso invalido", "La contrasena predeterminada no coincide.");
            requestLogin();
            return;
        }
        currentUser = userName;
        recordEventAsync("LOGIN", "Sesion iniciada por " + currentUser);
    }

    private String loginFieldStyle() {
        return "-fx-background-color: #120B25;" +
                "-fx-border-color: #7A4FA3;" +
                "-fx-border-width: 1.3;" +
                "-fx-background-radius: 7;" +
                "-fx-border-radius: 7;" +
                "-fx-text-fill: #FFFFFF;" +
                "-fx-prompt-text-fill: #7A4FA3;" +
                "-fx-highlight-fill: #6FD6FF;" +
                "-fx-highlight-text-fill: #05030D;" +
                "-fx-font-size: 14px;" +
                "-fx-padding: 8 10 8 10;";
    }

    private String readRequiredText(TextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " es obligatorio.");
        }
        return value;
    }

    private double parseRequired(TextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(label + " es obligatorio.");
        }
        try {
            return Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + " debe ser numerico.");
        }
    }

    private TextField field(double value) {
        return new TextField(String.format(Locale.US, "%.4f", value));
    }

    private TextField field(int value) {
        return new TextField(String.valueOf(value));
    }

    private void startNetworkMonitor() {
        if (networkMonitor != null) {
            return;
        }

        networkMonitor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "aether-network-monitor");
            thread.setDaemon(true);
            return thread;
        });

        networkMonitor.scheduleAtFixedRate(() -> {
            String quality = NetworkQualityService.readQualityLabel();
            Platform.runLater(() -> lblConectividad.setText(quality));
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void initializeMissionMap() {
        if (missionMapContainer == null) {
            return;
        }
        MissionMap3D.resetTrail();
        missionMapContainer.getChildren().setAll(MissionMap3D.createPlaceholder());
    }

    private void updateMissionMap(MissionState state) {
        if (missionMapContainer == null || state == null) {
            return;
        }
        // MissionMap3D conserva la misma SubScene y mueve solo la nave/trayectoria revelada.
        MissionMap3D.updateState(state);
    }

    private void refreshMissionMap() {
        if (lastState == null) {
            initializeMissionMap();
            return;
        }
        updateMissionMap(lastState);
    }

    private void styleDialog(Dialog<?> dialog) {
        // Estilo base para que los Dialog de JavaFX se vean como paneles AETHER.
        dialog.getDialogPane().setStyle(
                "-fx-background-color: #120B25;" +
                "-fx-border-color: #7A4FA3;" +
                "-fx-border-width: 1.5;" +
                "-fx-background-radius: 10;" +
                "-fx-border-radius: 10;"
        );
        applyDialogNodeStyles(dialog);

        // JavaFX crea algunos botones tarde; al mostrarse el dialogo se reaplican estilos.
        dialog.setOnShown(event -> applyDialogNodeStyles(dialog));
    }

    private void applyDialogNodeStyles(Dialog<?> dialog) {
        // Aplica colores AETHER a controles internos del Dialog sin depender del CSS principal.
        dialog.getDialogPane().lookupAll(".label").forEach(node -> node.setStyle("-fx-text-fill: #E7D8F0; -fx-font-size: 13px;"));
        dialog.getDialogPane().lookupAll(".button").forEach(node -> node.setStyle(
                "-fx-background-color: #1F1736;" +
                "-fx-border-color: #6FD6FF;" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 7;" +
                "-fx-border-radius: 7;" +
                "-fx-text-fill: #E7D8F0;" +
                "-fx-font-weight: bold;" +
                "-fx-padding: 8 18 8 18;" +
                "-fx-cursor: hand;"
        ));
        dialog.getDialogPane().lookupAll(".text-field").forEach(node -> node.setStyle(loginFieldStyle()));
        dialog.getDialogPane().lookupAll(".password-field").forEach(node -> node.setStyle(loginFieldStyle()));
        dialog.getDialogPane().lookupAll(".slider").forEach(node -> node.setStyle(
                "-fx-control-inner-background: #1F1736;" +
                "-fx-accent: #6FD6FF;"
        ));
        dialog.getDialogPane().lookupAll(".track").forEach(node -> node.setStyle("-fx-background-color: #2A1E43; -fx-background-radius: 4;"));
        dialog.getDialogPane().lookupAll(".thumb").forEach(node -> node.setStyle("-fx-background-color: #6FD6FF; -fx-background-radius: 10;"));
        dialog.getDialogPane().lookupAll(".text-area").forEach(node -> node.setStyle(
                reportTextAreaStyle()
        ));
        dialog.getDialogPane().lookupAll(".table-view").forEach(node -> node.setStyle(
                "-fx-background-color: #0B0616;" +
                "-fx-control-inner-background: #0B0616;" +
                "-fx-table-cell-border-color: #1F1736;" +
                "-fx-selection-bar: #6FD6FF;" +
                "-fx-selection-bar-non-focused: #7A4FA3;"
        ));
    }

    private VBox createDialogContent(String title, Node content) {
        // Sustituye el header blanco nativo del Dialog por un titulo integrado al tema oscuro.
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-text-fill: #6FD6FF;" +
                "-fx-font-size: 17px;" +
                "-fx-font-weight: bold;"
        );
        VBox box = new VBox(12, titleLabel, content);
        box.setStyle("-fx-background-color: #120B25; -fx-padding: 14 14 10 14;");
        return box;
    }

    private void styleReportTextArea(TextArea area) {
        // Resumen textual usado en Reportes e historial, con apariencia de consola AETHER.
        area.setStyle(reportTextAreaStyle());
    }

    private String reportTextAreaStyle() {
        return "-fx-control-inner-background: #0B0616;" +
                "-fx-background-color: #0B0616;" +
                "-fx-border-color: #7A4FA3;" +
                "-fx-border-width: 1;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-text-fill: #F6EDFF;" +
                "-fx-highlight-fill: #6FD6FF;" +
                "-fx-highlight-text-fill: #120B25;" +
                "-fx-font-family: 'Consolas';" +
                "-fx-font-size: 13px;";
    }

    private <T> void styleAetherTable(TableView<T> table) {
        // Tabla reutilizable para historial y reportes guardados.
        table.setFixedCellSize(30);
        table.setPlaceholder(new Label("Sin datos disponibles"));
        table.setStyle(
                "-fx-background-color: #0B0616;" +
                "-fx-control-inner-background: #0B0616;" +
                "-fx-table-cell-border-color: #24183C;" +
                "-fx-selection-bar: #253B64;" +
                "-fx-selection-bar-non-focused: #1F1736;" +
                "-fx-font-size: 12px;"
        );
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setStyle("-fx-background-color: " + (empty ? "#0B0616" : "#100A1D") + ";");
            }
        });
    }

    private String reportSummary() {
        int historyCount = repository.findRecentCalculations(20).size();
        int reportCount = repository.findRecentReports(20).size();
        return "Mision: " + config.getMissionName() + "\n" +
                "Nave: " + config.getSpacecraftName() + "\n" +
                "Estado actual: " + lblEstado.getText() + "\n" +
                "Calculos recientes disponibles: " + historyCount + "\n" +
                "Reportes guardados disponibles: " + reportCount + "\n\n" +
                "Generar PDF crea un nuevo reporte del estado actual e incluye el historial reciente.";
    }

    private void refreshRecentEvents() {
        List<MissionEventEntry> events = repository.findRecentEvents(3);
        Label[] labels = {lblRecentEvent1, lblRecentEvent2, lblRecentEvent3};
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] == null) {
                continue;
            }
            if (i < events.size()) {
                labels[i].setText(formatEvent(events.get(i)));
            } else if (i == 0) {
                labels[i].setText("Eventos: pendiente");
            } else {
                labels[i].setText("");
            }
        }
    }

    private void recordEventAsync(String type, String description) {
        Thread eventThread = new Thread(() -> {
            repository.saveMissionEvent(type, description, LocalDateTime.now());
            Platform.runLater(this::refreshRecentEvents);
        }, "aether-event-recorder");
        eventThread.setDaemon(true);
        eventThread.start();
    }

    private void saveSimulationSnapshotAsync(String eventType) {
        MissionState snapshot = lastState;
        if (snapshot == null || simulationSaved) {
            return;
        }
        simulationSaved = true;
        Thread snapshotThread = new Thread(() -> {
            LocalDateTime executedAt = LocalDateTime.now();
            repository.saveCalculation(config, snapshot, executedAt);
            repository.saveMissionEvent(eventType + "_HISTORIAL",
                    "Estado final guardado para historial de reportes", executedAt);
            Platform.runLater(() -> {
                refreshRecentEvents();
                if (lblLastCalculation != null) {
                    lblLastCalculation.setText("Ultimo calculo: " +
                            executedAt.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                }
                if (lblPersistenceStatus != null) {
                    lblPersistenceStatus.setText(repository.describeStatus());
                }
            });
        }, "aether-simulation-snapshot");
        snapshotThread.setDaemon(true);
        snapshotThread.start();
    }

    private void completeSimulationPlayback() {
        // Libera el reloj y el hilo, pero deja reloj, combustible y mapa en el estado final.
        trajectoryPlayback = null;
        simulationThread = null;
        orbitCalculated = lastState != null;
        if (lblCurrentPhase != null) {
            lblCurrentPhase.setText("Fase actual: Completada");
        }
        if (lblNextEvent != null) {
            lblNextEvent.setText("Siguiente: generar reporte");
        }
        lastMapRenderSecond = lastState == null ? -1 : lastState.getElapsedTime();
    }

    private String formatEvent(MissionEventEntry event) {
        String time = event.getExecutedAt() == null
                ? "--:--:--"
                : event.getExecutedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String description = event.getDescription() == null || event.getDescription().isBlank()
                ? event.getEventType()
                : event.getDescription();
        if (description.length() > 34) {
            description = description.substring(0, 31) + "...";
        }
        return time + " - " + description;
    }

    private boolean showCalculationHistory() {
        // Devuelve true cuando el usuario presiona Atras para volver al menu de Reportes.
        List<CalculationHistoryEntry> history = repository.findRecentCalculations(20);
        if (history.isEmpty()) {
            showInfo("Historial", "No hay calculos guardados en MySQL.");
            return true;
        } else {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Historial");
            dialog.setHeaderText(null);
            ButtonType back = new ButtonType("Atras", ButtonBar.ButtonData.BACK_PREVIOUS);
            ButtonType close = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().setContent(createDialogContent("CALCULOS ORBITALES GUARDADOS EN MYSQL", createHistoryTable(history)));
            dialog.getDialogPane().getButtonTypes().addAll(back, close);
            styleDialog(dialog);
            Optional<ButtonType> result = dialog.showAndWait();
            return result.isPresent() && result.get() == back;
        }
    }

    private boolean showSavedReports() {
        // Devuelve true al presionar Atras o despues de abrir un reporte seleccionado.
        List<MissionReportEntry> reports = repository.findRecentReports(20);
        if (reports.isEmpty()) {
            showInfo("Reportes guardados", "No hay reportes guardados en MySQL.");
            return true;
        } else {
            TableView<MissionReportEntry> table = createReportsTable(reports);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Reportes guardados");
            dialog.setHeaderText(null);
            ButtonType open = new ButtonType("Abrir seleccionado", ButtonBar.ButtonData.OK_DONE);
            ButtonType back = new ButtonType("Atras", ButtonBar.ButtonData.BACK_PREVIOUS);
            ButtonType close = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().setContent(createDialogContent("REPORTES PERSISTIDOS EN MYSQL", table));
            dialog.getDialogPane().getButtonTypes().addAll(open, back, close);
            styleDialog(dialog);
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == open && table.getSelectionModel().getSelectedItem() != null) {
                openFile(new File(table.getSelectionModel().getSelectedItem().getFilePath()));
                return true;
            }
            return result.isPresent() && result.get() == back;
        }
    }

    private TableView<CalculationHistoryEntry> createHistoryTable(List<CalculationHistoryEntry> history) {
        TableView<CalculationHistoryEntry> table = new TableView<>();
        table.setPrefSize(720, 330);
        styleAetherTable(table);
        table.getItems().setAll(history);

        TableColumn<CalculationHistoryEntry, String> date = column("Fecha", 145);
        date.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getExecutedAt())));

        TableColumn<CalculationHistoryEntry, String> mission = column("Mision", 110);
        mission.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMissionName()));

        TableColumn<CalculationHistoryEntry, String> ship = column("Nave", 90);
        ship.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSpacecraftName()));

        TableColumn<CalculationHistoryEntry, String> velocity = column("Velocidad", 115);
        velocity.setCellValueFactory(data -> new SimpleStringProperty(String.format(Locale.US, "%.3f km/s", data.getValue().getVelocityKms())));

        TableColumn<CalculationHistoryEntry, String> altitude = column("Altitud", 115);
        altitude.setCellValueFactory(data -> new SimpleStringProperty(String.format(Locale.US, "%.1f km", data.getValue().getAltitudeKm())));

        TableColumn<CalculationHistoryEntry, String> moon = column("Dist. Luna", 130);
        moon.setCellValueFactory(data -> new SimpleStringProperty(String.format(Locale.US, "%.1f km", data.getValue().getDistanceMoonKm())));

        table.getColumns().add(date);
        table.getColumns().add(mission);
        table.getColumns().add(ship);
        table.getColumns().add(velocity);
        table.getColumns().add(altitude);
        table.getColumns().add(moon);
        return table;
    }

    private TableView<MissionReportEntry> createReportsTable(List<MissionReportEntry> reports) {
        TableView<MissionReportEntry> table = new TableView<>();
        table.setPrefSize(820, 330);
        styleAetherTable(table);
        table.getItems().setAll(reports);

        TableColumn<MissionReportEntry, String> date = column("Fecha", 145);
        date.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue().getGeneratedAt())));

        TableColumn<MissionReportEntry, String> mission = column("Mision", 120);
        mission.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMissionName()));

        TableColumn<MissionReportEntry, String> file = column("Archivo", 210);
        file.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFileName()));

        TableColumn<MissionReportEntry, String> path = column("Ruta", 330);
        path.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFilePath()));

        table.getColumns().add(date);
        table.getColumns().add(mission);
        table.getColumns().add(file);
        table.getColumns().add(path);
        return table;
    }

    private <T> TableColumn<T, String> column(String title, double width) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setStyle("-fx-alignment: CENTER_LEFT; -fx-background-color: #120B25; -fx-text-fill: #F6EDFF;");
        column.setCellFactory(data -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setStyle(
                        "-fx-background-color: " + (empty ? "#0B0616" : "#100A1D") + ";" +
                        "-fx-text-fill: #F6EDFF;" +
                        "-fx-border-color: #24183C;" +
                        "-fx-border-width: 0 1 1 0;" +
                        "-fx-padding: 0 8 0 8;"
                );
            }
        });
        return column;
    }

    private void openLastReport() {
        List<MissionReportEntry> reports = repository.findRecentReports(1);
        if (reports.isEmpty()) {
            showInfo("Reportes", "No hay reportes guardados para abrir.");
            return;
        }
        File file = new File(reports.get(0).getFilePath());
        openFile(file);
    }

    private void openFile(File file) {
        try {
            File resolvedFile = resolveReportFile(file);
            if (!resolvedFile.exists()) {
                showError("Reporte no encontrado", file.getAbsolutePath());
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(resolvedFile);
            } else {
                showInfo("Archivo", resolvedFile.getAbsolutePath());
            }
        } catch (IOException exception) {
            showError("No se pudo abrir el archivo", safeExceptionMessage(exception));
        }
    }

    private File resolveReportFile(File storedFile) {
        // MySQL guarda rutas absolutas. Si la carpeta del proyecto cambia de nombre,
        // se busca el mismo PDF dentro de la carpeta reportes del proyecto actual.
        if (storedFile.exists()) {
            return storedFile;
        }
        File currentProjectReport = new File("reportes", storedFile.getName());
        return currentProjectReport.exists() ? currentProjectReport : storedFile;
    }

    private void showTextDialog(String title, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        TextArea area = new TextArea(message);
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefColumnCount(84);
        area.setPrefRowCount(18);
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        styleDialog(dialog);
        dialog.showAndWait();
    }

    private String formatDate(LocalDateTime date) {
        return date == null ? "sin fecha" : date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void setSystemState(String state) {
        if (lblEstadoSistema != null) {
            lblEstadoSistema.setText(state);
        }
    }

    private String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        styleDialog(alert);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        styleDialog(alert);
        alert.setContentText(message == null ? "Error desconocido" : message);
        alert.showAndWait();
    }

    private String safeExceptionMessage(Exception exception) {
        // Evita llamar directamente mensajes localizados que pueden fallar con Orekit en modo modular.
        // Lo usan los dialogos y eventos para mostrar un texto estable sin romper JavaFX.
        if (exception == null) {
            return "Error desconocido";
        }
        try {
            String message = exception.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (RuntimeException messageFailure) {
            return exception.getClass().getSimpleName() + " (" + messageFailure.getClass().getSimpleName() + ")";
        }
        Throwable cause = exception.getCause();
        if (cause == null || cause == exception) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + " causado por " + cause.getClass().getSimpleName();
    }
}
