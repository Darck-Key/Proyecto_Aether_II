package com.example.demoaether;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistencia real en MySQL.
 *
 * Flujo de llamadas:
 * 1. RepositoryFactory crea esta clase si AETHER_DB_ENABLED=true.
 * 2. initializeSchema crea las tablas si no existen.
 * 3. HelloController llama saveCalculation/saveMissionEvent/saveReport segun la accion del usuario.
 */
public class MySqlAetherRepository implements AetherRepository {

    private final DatabaseConfig config;

    public MySqlAetherRepository(DatabaseConfig config) {
        // Constructor llamado por RepositoryFactory; prepara driver y esquema antes de usar el repositorio.
        this.config = config;
        loadDriverIfPresent();
        initializeSchema();
    }

    @Override
    public void saveCalculation(MissionConfig missionConfig, MissionState state, LocalDateTime executedAt) {
        // Guarda la telemetria calculada para que Historial y ReportGenerator la puedan consultar.
        String sql = "INSERT INTO orbital_calculations " +
                "(mission_name, spacecraft_name, altitude_km, velocity_kms, inclination_deg, eccentricity, " +
                "argument_of_perigee_deg, elapsed_seconds, x_km, y_km, z_km, distance_earth_km, " +
                "distance_moon_km, calculated_altitude_km, executed_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, missionConfig.getMissionName());
            statement.setString(2, missionConfig.getSpacecraftName());
            statement.setDouble(3, missionConfig.getInitialAltitude());
            statement.setDouble(4, missionConfig.getInitialVelocity());
            statement.setDouble(5, missionConfig.getInclination());
            statement.setDouble(6, missionConfig.getEccentricity());
            statement.setDouble(7, missionConfig.getArgumentOfPerigee());
            statement.setDouble(8, state.getElapsedTime());
            statement.setDouble(9, state.getX());
            statement.setDouble(10, state.getY());
            statement.setDouble(11, state.getZ());
            statement.setDouble(12, state.getDistanceEarth());
            statement.setDouble(13, state.getDistanceMoon());
            statement.setDouble(14, state.getAltitude());
            statement.setTimestamp(15, Timestamp.valueOf(executedAt));
            statement.executeUpdate();
        } catch (SQLException exception) {
            System.err.println("No se pudo guardar el calculo orbital en MySQL: " + exception.getMessage());
        }
    }

    @Override
    public void saveMissionEvent(String eventType, String description, LocalDateTime executedAt) {
        // Guarda eventos operativos: login, inicio, error, completado, calculo orbital, etc.
        String sql = "INSERT INTO mission_events (event_type, description, executed_at) VALUES (?, ?, ?)";

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, eventType);
            statement.setString(2, description == null ? "" : description);
            statement.setTimestamp(3, Timestamp.valueOf(executedAt));
            statement.executeUpdate();
        } catch (SQLException exception) {
            System.err.println("No se pudo guardar el evento en MySQL: " + exception.getMessage());
        }
    }

    @Override
    public void saveReport(File reportFile, MissionConfig missionConfig, MissionState state, LocalDateTime generatedAt) {
        // Registra el PDF creado por ReportGenerator para abrirlo despues desde Reportes guardados.
        String sql = "INSERT INTO mission_reports (file_name, file_path, mission_name, generated_at) VALUES (?, ?, ?, ?)";

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reportFile.getName());
            statement.setString(2, reportFile.getAbsolutePath());
            statement.setString(3, missionConfig.getMissionName());
            statement.setTimestamp(4, Timestamp.valueOf(generatedAt));
            statement.executeUpdate();
        } catch (SQLException exception) {
            System.err.println("No se pudo guardar el reporte en MySQL: " + exception.getMessage());
        }
    }

    @Override
    public List<CalculationHistoryEntry> findRecentCalculations(int limit) {
        // Consulta los ultimos calculos para la tabla Historial y para anexarlos al PDF.
        String sql = "SELECT id, mission_name, spacecraft_name, elapsed_seconds, velocity_kms, " +
                "calculated_altitude_km, distance_moon_km, executed_at " +
                "FROM orbital_calculations ORDER BY executed_at DESC, id DESC LIMIT ?";

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CalculationHistoryEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp executedAt = resultSet.getTimestamp("executed_at");
                    entries.add(new CalculationHistoryEntry(
                            resultSet.getLong("id"),
                            resultSet.getString("mission_name"),
                            resultSet.getString("spacecraft_name"),
                            resultSet.getDouble("elapsed_seconds"),
                            resultSet.getDouble("velocity_kms"),
                            resultSet.getDouble("calculated_altitude_km"),
                            resultSet.getDouble("distance_moon_km"),
                            executedAt == null ? null : executedAt.toLocalDateTime()
                    ));
                }
                return entries;
            }
        } catch (SQLException exception) {
            System.err.println("No se pudo leer el historial orbital en MySQL: " + exception.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<MissionEventEntry> findRecentEvents(int limit) {
        // Consulta eventos recientes para el panel inferior de la interfaz.
        String sql = "SELECT id, event_type, description, executed_at " +
                "FROM mission_events ORDER BY executed_at DESC, id DESC LIMIT ?";

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MissionEventEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp executedAt = resultSet.getTimestamp("executed_at");
                    entries.add(new MissionEventEntry(
                            resultSet.getLong("id"),
                            resultSet.getString("event_type"),
                            resultSet.getString("description"),
                            executedAt == null ? null : executedAt.toLocalDateTime()
                    ));
                }
                return entries;
            }
        } catch (SQLException exception) {
            System.err.println("No se pudieron leer los eventos en MySQL: " + exception.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<MissionReportEntry> findRecentReports(int limit) {
        // Consulta PDFs guardados para la ventana Reportes guardados.
        String sql = "SELECT id, file_name, file_path, mission_name, generated_at " +
                "FROM mission_reports ORDER BY generated_at DESC, id DESC LIMIT ?";

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                List<MissionReportEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    Timestamp generatedAt = resultSet.getTimestamp("generated_at");
                    entries.add(new MissionReportEntry(
                            resultSet.getLong("id"),
                            resultSet.getString("file_name"),
                            resultSet.getString("file_path"),
                            resultSet.getString("mission_name"),
                            generatedAt == null ? null : generatedAt.toLocalDateTime()
                    ));
                }
                return entries;
            }
        } catch (SQLException exception) {
            System.err.println("No se pudieron leer los reportes en MySQL: " + exception.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void saveMissionConfig(MissionConfig missionConfig, LocalDateTime savedAt) {
        // Persiste los parametros de Opciones para restaurarlos en el siguiente arranque.
        String sql = "INSERT INTO mission_configs " +
                "(mission_name, spacecraft_name, spacecraft_mass, initial_altitude_km, initial_velocity_kms, " +
                "inclination_deg, eccentricity, argument_of_perigee_deg, tli_delta_v_kms, tli_burn_offset_hours, simulation_hours, " +
                "simulation_step_seconds, simulation_speed, save_reports, saved_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, missionConfig.getMissionName());
            statement.setString(2, missionConfig.getSpacecraftName());
            statement.setDouble(3, missionConfig.getSpacecraftMass());
            statement.setDouble(4, missionConfig.getInitialAltitude());
            statement.setDouble(5, missionConfig.getInitialVelocity());
            statement.setDouble(6, missionConfig.getInclination());
            statement.setDouble(7, missionConfig.getEccentricity());
            statement.setDouble(8, missionConfig.getArgumentOfPerigee());
            statement.setDouble(9, missionConfig.getTliDeltaVKms());
            statement.setDouble(10, missionConfig.getTliBurnOffsetHours());
            statement.setInt(11, missionConfig.getSimulationHours());
            statement.setInt(12, missionConfig.getSimulationStepSeconds());
            statement.setInt(13, missionConfig.getSimulationSpeed());
            statement.setBoolean(14, missionConfig.isSaveReports());
            statement.setTimestamp(15, Timestamp.valueOf(savedAt));
            statement.executeUpdate();
        } catch (SQLException exception) {
            System.err.println("No se pudo guardar la configuracion en MySQL: " + exception.getMessage());
        }
    }

    @Override
    public MissionConfig loadLastMissionConfig() {
        // Recupera la ultima configuracion usada; si no existe, HelloController crea MissionConfig por defecto.
        String sql = "SELECT mission_name, spacecraft_name, spacecraft_mass, initial_altitude_km, " +
                "initial_velocity_kms, inclination_deg, eccentricity, argument_of_perigee_deg, " +
                "tli_delta_v_kms, tli_burn_offset_hours, simulation_hours, simulation_step_seconds, simulation_speed, save_reports " +
                "FROM mission_configs ORDER BY saved_at DESC, id DESC LIMIT 1";

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return null;
            }

            MissionConfig missionConfig = new MissionConfig();
            missionConfig.setMissionName(resultSet.getString("mission_name"));
            missionConfig.setSpacecraftName(resultSet.getString("spacecraft_name"));
            missionConfig.setSpacecraftMass(resultSet.getDouble("spacecraft_mass"));
            missionConfig.setInitialAltitude(resultSet.getDouble("initial_altitude_km"));
            missionConfig.setInitialVelocity(resultSet.getDouble("initial_velocity_kms"));
            missionConfig.setInclination(resultSet.getDouble("inclination_deg"));
            missionConfig.setEccentricity(resultSet.getDouble("eccentricity"));
            missionConfig.setArgumentOfPerigee(resultSet.getDouble("argument_of_perigee_deg"));
            missionConfig.setTliDeltaVKms(resultSet.getDouble("tli_delta_v_kms"));
            missionConfig.setTliBurnOffsetHours(resultSet.getDouble("tli_burn_offset_hours"));
            missionConfig.setSimulationHours(resultSet.getInt("simulation_hours"));
            missionConfig.setSimulationStepSeconds(resultSet.getInt("simulation_step_seconds"));
            missionConfig.setSimulationSpeed(resultSet.getInt("simulation_speed"));
            missionConfig.setSaveReports(resultSet.getBoolean("save_reports"));
            return missionConfig;
        } catch (SQLException exception) {
            System.err.println("No se pudo cargar la configuracion desde MySQL: " + exception.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        // Prueba rapida de conexion usada por RepositoryFactory para decidir si se mantiene MySQL.
        try (Connection ignored = openConnection()) {
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    @Override
    public String describeStatus() {
        return isAvailable() ? "MySQL: conectado" : "MySQL: sin conexion";
    }

    private void initializeSchema() {
        // Crea tablas y columnas necesarias sin borrar datos existentes.
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS orbital_calculations (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "mission_name VARCHAR(120) NOT NULL," +
                    "spacecraft_name VARCHAR(120) NOT NULL," +
                    "altitude_km DOUBLE NOT NULL," +
                    "velocity_kms DOUBLE NOT NULL," +
                    "inclination_deg DOUBLE NOT NULL," +
                    "eccentricity DOUBLE NOT NULL," +
                    "argument_of_perigee_deg DOUBLE NOT NULL," +
                    "elapsed_seconds DOUBLE NOT NULL," +
                    "x_km DOUBLE NOT NULL," +
                    "y_km DOUBLE NOT NULL," +
                    "z_km DOUBLE NOT NULL," +
                    "distance_earth_km DOUBLE NOT NULL," +
                    "distance_moon_km DOUBLE NOT NULL," +
                    "calculated_altitude_km DOUBLE NOT NULL," +
                    "executed_at DATETIME NOT NULL" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mission_events (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "event_type VARCHAR(80) NOT NULL," +
                    "description TEXT," +
                    "executed_at DATETIME NOT NULL" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mission_reports (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "file_name VARCHAR(255) NOT NULL," +
                    "file_path TEXT NOT NULL," +
                    "mission_name VARCHAR(120) NOT NULL," +
                    "generated_at DATETIME NOT NULL" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS mission_configs (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "mission_name VARCHAR(120) NOT NULL," +
                    "spacecraft_name VARCHAR(120) NOT NULL," +
                    "spacecraft_mass DOUBLE NOT NULL," +
                    "initial_altitude_km DOUBLE NOT NULL," +
                    "initial_velocity_kms DOUBLE NOT NULL," +
                    "inclination_deg DOUBLE NOT NULL," +
                    "eccentricity DOUBLE NOT NULL," +
                    "argument_of_perigee_deg DOUBLE NOT NULL," +
                    "tli_delta_v_kms DOUBLE NOT NULL DEFAULT 3.2," +
                    "tli_burn_offset_hours DOUBLE NOT NULL DEFAULT 1.5," +
                    "simulation_hours INT NOT NULL," +
                    "simulation_step_seconds INT NOT NULL," +
                    "simulation_speed INT NOT NULL," +
                    "save_reports BOOLEAN NOT NULL," +
                    "saved_at DATETIME NOT NULL" +
                    ")");
            addColumnIfMissing(statement, "mission_configs", "tli_delta_v_kms", "DOUBLE NOT NULL DEFAULT 3.2");
            addColumnIfMissing(statement, "mission_configs", "tli_burn_offset_hours", "DOUBLE NOT NULL DEFAULT 1.5");
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo inicializar el esquema MySQL", exception);
        }
    }

    private void addColumnIfMissing(Statement statement, String table, String column, String definition) throws SQLException {
        // Migracion ligera: agrega columnas nuevas si la base fue creada con una version anterior.
        try {
            statement.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        } catch (SQLException exception) {
            if (!"42S21".equals(exception.getSQLState())) {
                throw exception;
            }
        }
    }

    private Connection openConnection() throws SQLException {
        // Punto unico de conexion JDBC hacia MySQL.
        return DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
    }

    private void loadDriverIfPresent() {
        // Carga MySQL Connector/J si esta disponible en el classpath de Gradle/Maven.
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
            // DriverManager tambien puede descubrir el driver si esta en el classpath.
        }
    }
}
