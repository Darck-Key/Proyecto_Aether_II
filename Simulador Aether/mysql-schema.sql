-- Esquema MySQL de AETHER.
-- MySqlAetherRepository crea estas tablas automaticamente, pero este archivo sirve como referencia.

CREATE DATABASE IF NOT EXISTS aether CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE aether;

CREATE TABLE IF NOT EXISTS orbital_calculations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mission_name VARCHAR(120) NOT NULL,
    spacecraft_name VARCHAR(120) NOT NULL,
    altitude_km DOUBLE NOT NULL,
    velocity_kms DOUBLE NOT NULL,
    inclination_deg DOUBLE NOT NULL,
    eccentricity DOUBLE NOT NULL,
    argument_of_perigee_deg DOUBLE NOT NULL,
    elapsed_seconds DOUBLE NOT NULL,
    x_km DOUBLE NOT NULL,
    y_km DOUBLE NOT NULL,
    z_km DOUBLE NOT NULL,
    distance_earth_km DOUBLE NOT NULL,
    distance_moon_km DOUBLE NOT NULL,
    calculated_altitude_km DOUBLE NOT NULL,
    executed_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS mission_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(80) NOT NULL,
    description TEXT,
    executed_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS mission_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    mission_name VARCHAR(120) NOT NULL,
    generated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS mission_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mission_name VARCHAR(120) NOT NULL,
    spacecraft_name VARCHAR(120) NOT NULL,
    spacecraft_mass DOUBLE NOT NULL,
    initial_altitude_km DOUBLE NOT NULL,
    initial_velocity_kms DOUBLE NOT NULL,
    inclination_deg DOUBLE NOT NULL,
    eccentricity DOUBLE NOT NULL,
    argument_of_perigee_deg DOUBLE NOT NULL,
    simulation_hours INT NOT NULL,
    simulation_step_seconds INT NOT NULL,
    simulation_speed INT NOT NULL,
    save_reports BOOLEAN NOT NULL,
    saved_at DATETIME NOT NULL
);
