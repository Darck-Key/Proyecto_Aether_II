# Entregable E5 - Justificacion de pruebas AETHER

## Objetivo

Este documento resume como se validan las funciones principales de AETHER para el Entregable 5. Las pruebas se enfocan en el nucleo verificable por JUnit: parametros de mision, propagacion Orekit, simulacion, telemetria y generacion de reportes. La interfaz JavaFX y los dialogos visuales se revisan principalmente por ejecucion manual, porque requieren una escena grafica activa.

## Herramientas usadas

- JUnit 5: ejecuta pruebas unitarias y de integracion.
- Mockito: simula datos de `MissionState` para validar el formateo de telemetria sin arrancar JavaFX.
- Maven Surefire: genera resultados de pruebas en XML/TXT y reporte HTML.
- JaCoCo: genera cobertura HTML y valida un minimo de 60% por clases cubiertas.
- Orekit: motor orbital usado por las pruebas OAM para validar propagacion, fuerzas y eventos.

## Configuracion de reportes

El proyecto usa `pom.xml` para centralizar las pruebas:

- `maven-surefire-plugin`: corre JUnit 5.
- `maven-surefire-report-plugin`: genera el reporte HTML de pruebas.
- `jacoco-maven-plugin`: prepara el agente, genera el HTML de cobertura y falla el build si no alcanza el minimo.

Clases excluidas del gate de cobertura: `HelloApplication`, `HelloController`, `Launcher`, `MissionMap3D`, `MissionInput`, `MissionPresets`, `MySqlAetherRepository`, `PendingDatabaseRepository` y `NetworkQualityService`. Se excluyen porque son UI JavaFX, entrada grafica o infraestructura externa; su validacion depende de ejecucion manual, base de datos o recursos del sistema. La cobertura automatizada mide la logica orbital y de dominio.

## Matriz de trazabilidad

| Requisito E5 | Prueba automatizada | Clase bajo prueba | Evidencia |
| --- | --- | --- | --- |
| OAM-1 Inicializar Orekit | `OrekitInitializerTest.oam1InitializesOrekitDataContext` | `OrekitInitializer` | Verifica que el contexto de datos de Orekit tenga proveedores cargados. |
| OAM-2 Crear orbita inicial | `OrbitFactoryTest.oam2CreatesCircularParkingOrbitNear185Km` | `OrbitFactory` | Valida orbita circular de estacionamiento cerca de 185 km. |
| OAM-3 Modelos de fuerza | `OrekitTrajectoryPlannerTest.oam3PropagatorIncludesRequiredForceModels` | `OrekitTrajectoryPlanner` | Confirma gravedad terrestre y perturbaciones de Luna/Sol. |
| OAM-4 Delta-v TLI modifica trayectoria | `OrekitTrajectoryPlannerTest.oam4ChangingTliDeltaVChangesTrajectory` | `OrekitTrajectoryPlanner` | Compara estados finales con diferentes delta-v. |
| OAM-5 Minimo de muestras | `OrekitTrajectoryPlannerTest.oam5TrajectoryHasAtLeast500ChronologicalSamples` | `MissionTrajectory`, `OrekitTrajectoryPlanner` | Exige al menos 500 muestras ordenadas temporalmente. |
| OAM-6 Evento lunar | `OrekitTrajectoryPlannerTest.oam6LunarPeriapsisEventIsRegistered` | `OrekitTrajectoryPlanner` | Verifica registro de periapsis lunar. |
| OAM-7 Reentrada | `OrekitTrajectoryPlannerTest.oam7ReentryDetectorUses120KmInterface` | `OrekitTrajectoryPlanner` | Valida umbral de reentrada de 120 km. |
| UI-2 Telemetria | `TelemetryViewModelTest.ui2FormatsTelemetryFromPhysicsStateUsingMockito` | `TelemetryViewModel` | Verifica textos de tiempo, velocidad, altitud y distancia. |
| UI-3 Validacion de parametros | `MissionConfigValidationTest` | `MissionConfig` | Valida rangos aceptados y rechazados para parametros orbitales. |
| UI-4 Recalculo con parametros | `OrekitTrajectoryPlannerTest.ui4ChangingInputParametersProducesDifferentTrajectory` | `MissionConfig`, `OrekitTrajectoryPlanner` | Demuestra que cambiar altitud inicial cambia la trayectoria. |
| Integracion simulacion completa | `MissionSimulatorRuntimeTest.integrationPipelineProducesStatesConsumedByListener` | `MissionSimulator`, `TelemetryViewModel`, `OrekitTrajectoryPlanner` | Valida flujo completo: configuracion, propagacion, listener y telemetria. |
| Reportes | `ReportGeneratorTest` | `ReportGenerator` | Valida creacion de archivo PDF de reporte. |

## Resultados actuales

Ultima ejecucion validada:

```text
mvn verify surefire-report:report
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
JaCoCo check: All coverage checks have been met.
```

Cobertura JaCoCo sobre clases medidas:

| Metrica | Resultado |
| --- | --- |
| Cobertura de clases | 73.33% |
| Clases cubiertas | 11 de 15 |
| Cobertura de lineas | 72.11% |
| Cobertura de instrucciones | 75.40% |

## Artefactos generados

- Reporte Surefire HTML: `target/reports/surefire.html`
- Reporte JaCoCo HTML: `target/site/jacoco/index.html`
- CSV de cobertura: `target/site/jacoco/jacoco.csv`
- JAR compilado: `target/aether-mission-control-1.0-SNAPSHOT.jar`

## Limitaciones y trabajo futuro

- Las ventanas JavaFX, estilos visuales y dialogos emergentes requieren pruebas manuales o una futura suite TestFX.
- La conexion MySQL depende del ambiente local; el proyecto deja lista la capa de repositorio, pero la validacion completa requiere credenciales y servicio activo.
- El mapa 3D fue dejado funcional para simulacion visual, pero una escena 3D avanzada puede integrarse despues sin cambiar el contrato principal de simulacion.
