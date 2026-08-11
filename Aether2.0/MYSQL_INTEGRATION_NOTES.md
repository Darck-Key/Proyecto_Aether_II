# Integracion MySQL de AETHER

## Que llama a que

- `HelloController` es la interfaz JavaFX.
- `HelloController` llama a `AetherRepository` cuando ocurre algo persistible.
- `RepositoryFactory` decide si usar MySQL o modo pendiente.
- `MySqlAetherRepository` guarda datos reales en MySQL.
- `PendingDatabaseRepository` evita que la app falle si MySQL no esta listo.

## Eventos guardados

- Al calcular orbita: `saveCalculation(...)` y evento `CALCULO_ORBITAL`.
- Al iniciar simulacion: evento `SIMULACION_INICIADA`.
- Al completar simulacion: evento `SIMULACION_COMPLETADA`.
- Al fallar simulacion: evento `SIMULACION_ERROR`.
- Al generar reporte: `saveReport(...)`.

## Variables para activar MySQL

En PowerShell, antes de ejecutar la app:

```powershell
$env:AETHER_DB_ENABLED="true"
$env:AETHER_DB_URL="jdbc:mysql://localhost:3306/aether?serverTimezone=UTC"
$env:AETHER_DB_USER="root"
$env:AETHER_DB_PASSWORD="tu_password"
```

## Driver JDBC

El codigo compila con `java.sql`, pero para conectar en runtime necesitas MySQL Connector/J en el classpath.
Puedes agregar en Gradle:

```kotlin
runtimeOnly("com.mysql:mysql-connector-j:8.4.0")
```

Si no hay driver o servidor, la app mostrara `MySQL: pendiente` y seguira funcionando.
## Archivos agregados por Codex

- `init-mysql.ps1`: ejecuta `mysql-schema.sql` cuando MySQL este instalado.
- `run-with-mysql.ps1`: ejecuta la app con variables de MySQL configuradas.
- `build.gradle.kts`: incluye `runtimeOnly("com.mysql:mysql-connector-j:8.4.0")`.

Ejemplo:

```powershell
.\init-mysql.ps1 -User root -Password "tu_password"
.\run-with-mysql.ps1 -User root -Password "tu_password"
```