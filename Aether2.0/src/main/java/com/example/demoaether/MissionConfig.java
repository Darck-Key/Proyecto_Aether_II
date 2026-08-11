package com.example.demoaether;

/**
 * Configuración de la misión.
 * Contiene los parámetros orbitales y de ejecución usados por Orekit.
 */
public class MissionConfig {

    // Modelo central de parametros: lo llena HelloController, lo calcula OrekitTrajectoryPlanner
    // y lo guarda MySqlAetherRepository en la tabla mission_configs.

    private String missionName;
    private String spacecraftName;
    private double spacecraftMass;
    private double initialAltitude;
    private double initialVelocity;
    private double inclination;
    private double eccentricity;
    private double argumentOfPerigee;
    private double tliDeltaVKms;
    private double tliBurnOffsetHours;
    private int simulationHours;
    private int simulationStepSeconds;
    private int simulationSpeed;
    private boolean saveReports;

    public MissionConfig() {
        // Valores por defecto para que la interfaz pueda iniciar sin depender de MySQL.
        missionName = "Artemis II";
        spacecraftName = "Orion";
        spacecraftMass = 26000;
        initialAltitude = 185;
        initialVelocity = 7.8;
        inclination = 28.5;
        eccentricity = 0.0;
        argumentOfPerigee = 0.0;
        tliDeltaVKms = 3.2;
        tliBurnOffsetHours = 23.5;
        simulationHours = 240;
        simulationStepSeconds = 60;
        simulationSpeed = 1000;
        saveReports = true;
    }

    public void validate() {
        // Validaciones usadas antes de guardar o ejecutar para evitar parametros imposibles.
        if (initialAltitude < 160 || initialAltitude > 2000) {
            throw new IllegalArgumentException("La altitud inicial debe estar entre 160 y 2000 km.");
        }
        if (initialVelocity <= 0 || initialVelocity > 20) {
            throw new IllegalArgumentException("La velocidad inicial debe estar entre 0 y 20 km/s.");
        }
        if (inclination < 0 || inclination > 180) {
            throw new IllegalArgumentException("La inclinación debe estar entre 0 y 180 grados.");
        }
        if (eccentricity < 0 || eccentricity >= 1) {
            throw new IllegalArgumentException("La excentricidad debe ser mayor o igual a 0 y menor que 1.");
        }
        if (argumentOfPerigee < 0 || argumentOfPerigee >= 360) {
            throw new IllegalArgumentException("El argumento del perigeo debe estar entre 0 y 360 grados.");
        }
        if (simulationHours <= 0 || simulationHours > 240) {
            throw new IllegalArgumentException("La duración simulada debe estar entre 1 y 240 horas.");
        }
        if (simulationStepSeconds <= 0) {
            throw new IllegalArgumentException("El paso de simulación debe ser mayor que cero.");
        }
        if (tliDeltaVKms < 0 || tliDeltaVKms > 5) {
            throw new IllegalArgumentException("El delta-v TLI debe estar entre 0 y 5 km/s.");
        }
        if (tliBurnOffsetHours < 0 || tliBurnOffsetHours > simulationHours) {
            throw new IllegalArgumentException("La epoca TLI debe estar dentro de la duracion simulada.");
        }
        if (simulationSpeed < 1 || simulationSpeed > 1000) {
            throw new IllegalArgumentException("La escala de tiempo debe estar entre 1x y 1000x.");
        }
    }

    public String getMissionName() { return missionName; }
    public void setMissionName(String missionName) { this.missionName = missionName; }
    public String getSpacecraftName() { return spacecraftName; }
    public void setSpacecraftName(String spacecraftName) { this.spacecraftName = spacecraftName; }
    public double getSpacecraftMass() { return spacecraftMass; }
    public void setSpacecraftMass(double spacecraftMass) { this.spacecraftMass = spacecraftMass; }
    public double getInitialAltitude() { return initialAltitude; }
    public void setInitialAltitude(double initialAltitude) { this.initialAltitude = initialAltitude; }
    public double getInitialVelocity() { return initialVelocity; }
    public void setInitialVelocity(double initialVelocity) { this.initialVelocity = initialVelocity; }
    public double getInclination() { return inclination; }
    public void setInclination(double inclination) { this.inclination = inclination; }
    public double getEccentricity() { return eccentricity; }
    public void setEccentricity(double eccentricity) { this.eccentricity = eccentricity; }
    public double getArgumentOfPerigee() { return argumentOfPerigee; }
    public void setArgumentOfPerigee(double argumentOfPerigee) { this.argumentOfPerigee = argumentOfPerigee; }
    public double getTliDeltaVKms() { return tliDeltaVKms; }
    public void setTliDeltaVKms(double tliDeltaVKms) { this.tliDeltaVKms = tliDeltaVKms; }
    public double getTliBurnOffsetHours() { return tliBurnOffsetHours; }
    public void setTliBurnOffsetHours(double tliBurnOffsetHours) { this.tliBurnOffsetHours = tliBurnOffsetHours; }
    public int getSimulationHours() { return simulationHours; }
    public void setSimulationHours(int simulationHours) { this.simulationHours = simulationHours; }
    public int getSimulationStepSeconds() { return simulationStepSeconds; }
    public void setSimulationStepSeconds(int simulationStepSeconds) { this.simulationStepSeconds = simulationStepSeconds; }
    public int getSimulationSpeed() { return simulationSpeed; }
    public void setSimulationSpeed(int simulationSpeed) { this.simulationSpeed = simulationSpeed; }
    public boolean isSaveReports() { return saveReports; }
    public void setSaveReports(boolean saveReports) { this.saveReports = saveReports; }
}
