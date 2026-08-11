package com.example.demoaether;

/**
 * Estado de telemetria de la nave en un instante.
 *
 * Quien lo crea:
 * - OrekitTrajectoryPlanner al convertir SpacecraftState de Orekit.
 * - ArtemisReferenceTrajectoryLoader al leer la efemeride oficial de NASA.
 *
 * Quien lo consume:
 * - HelloController para actualizar etiquetas, mapa y reportes.
 * - ReportGenerator para escribir los valores orbitales en PDF.
 * - AetherRepository para guardar el historial.
 */
public class MissionState {

    private double elapsedTime;

    private double x;

    private double y;

    private double z;

    private double velocity;

    private double distanceEarth;

    private double distanceMoon;

    private double altitude;

    private double moonX;

    private double moonY;

    private double moonZ;

    public MissionState() {
    }

    public MissionState(
            double elapsedTime,
            double x,
            double y,
            double z,
            double velocity,
            double distanceEarth,
            double distanceMoon,
            double altitude) {

        this(elapsedTime, x, y, z, velocity, distanceEarth, distanceMoon, altitude,
                Double.NaN, Double.NaN, Double.NaN);
    }

    /**
     * Construye un estado que tambien conserva la posicion de la Luna.
     *
     * <p>Quien lo llama: los adaptadores de Orekit. A quien sirve:
     * MissionMap3D usa estas coordenadas para proyectar el sobrevuelo sin
     * inventar un giro visual alrededor de la Luna.</p>
     */
    public MissionState(
            double elapsedTime,
            double x,
            double y,
            double z,
            double velocity,
            double distanceEarth,
            double distanceMoon,
            double altitude,
            double moonX,
            double moonY,
            double moonZ) {

        // Guarda valores ya normalizados a kilometros y segundos para UI, MySQL y PDF.
        this.elapsedTime = elapsedTime;
        this.x = x;
        this.y = y;
        this.z = z;
        this.velocity = velocity;
        this.distanceEarth = distanceEarth;
        this.distanceMoon = distanceMoon;
        this.altitude = altitude;
        this.moonX = moonX;
        this.moonY = moonY;
        this.moonZ = moonZ;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getVelocity() {
        return velocity;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }

    public double getDistanceEarth() {
        return distanceEarth;
    }

    public void setDistanceEarth(double distanceEarth) {
        this.distanceEarth = distanceEarth;
    }

    public double getDistanceMoon() {
        return distanceMoon;
    }

    public void setDistanceMoon(double distanceMoon) {
        this.distanceMoon = distanceMoon;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getMoonX() {
        return moonX;
    }

    public double getMoonY() {
        return moonY;
    }

    public double getMoonZ() {
        return moonZ;
    }

    /**
     * Indica si el estado incluye el vector lunar en el mismo marco que la nave.
     */
    public boolean hasMoonPosition() {
        return Double.isFinite(moonX) && Double.isFinite(moonY) && Double.isFinite(moonZ);
    }

}
