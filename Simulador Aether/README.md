# AETHER Mission Control Simulator

Nombre clave de la mision: Artemis II.

Distintivo del equipo: AETHER Mission Control.

## Descripcion

AETHER es una aplicacion JavaFX para visualizar una simulacion academica de la mision Artemis II. Integra una interfaz de control, telemetria, reportes PDF, persistencia MySQL y una capa de precalculo orbital con Orekit.

## Requisitos

- Java 21 o superior.
- Maven 3.9 o superior para el entregable E4.
- MySQL 8 si se desea persistencia real.
- Internet solo para descargar dependencias Maven/Gradle la primera vez.

## Compilar con Maven

```powershell
mvn clean package
```

## Ejecutar con Maven

```powershell
mvn javafx:run
```

## Compilar con Gradle

```powershell
.\gradlew.bat build
```

## Ejecutar con Gradle

```powershell
.\gradlew.bat run
```

## Variables MySQL

La aplicacion lee estas variables para conectar la base de datos:

```powershell
$env:AETHER_DB_ENABLED="true"
$env:AETHER_DB_URL="jdbc:mysql://localhost:3306/aether?serverTimezone=UTC"
$env:AETHER_DB_USER="root"
$env:AETHER_DB_PASSWORD="tu_password"
```

Si MySQL no esta disponible, AETHER usa un repositorio local en memoria para conservar historial durante la sesion activa.

## Flujo tecnico principal

- `HelloController` recibe acciones de la interfaz y llama al simulador, reportes y repositorio.
- `MissionSimulator` ejecuta la simulacion en un hilo de fondo.
- `OrekitTrajectoryPlanner` precalcula la trayectoria con Orekit antes de animarla.
- `MissionMap3D` renderiza el mapa orbital en JavaFX 3D.
- `MySqlAetherRepository` persiste calculos, eventos, configuraciones y reportes.
- `ReportGenerator` crea reportes PDF.

## Motor Orekit

La capa Orekit configura:

- `NumericalPropagator`.
- Integrador `DormandPrince853`.
- Gravedad terrestre por armonicos esfericos 8x8 mediante `HolmesFeatherstoneAttractionModel`.
- Atraccion de tercer cuerpo de Luna y Sol.
- Maniobra TLI con `ImpulseManeuver`.
- Muestreo normalizado de minimo 500 puntos con `OrekitStepNormalizer`.
- Detector de reentrada a 120 km con `AltitudeDetector`.
- Configuracion inicial alineada con E4: orbita de estacionamiento circular de aproximadamente 185 km.

## Datos Orekit

Los datos estan en:

```text
src/main/resources/orekit-data
```

No deben moverse ni renombrarse.
