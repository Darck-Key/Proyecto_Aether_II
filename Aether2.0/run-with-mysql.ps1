param(
    [string]$User = "root",
    [string]$Password = "",
    [string]$Url = "jdbc:mysql://localhost:3306/aether?serverTimezone=UTC"
)

$ErrorActionPreference = "Stop"
$env:AETHER_DB_ENABLED = "true"
$env:AETHER_DB_URL = $Url
$env:AETHER_DB_USER = $User
$env:AETHER_DB_PASSWORD = $Password
$env:JAVA_HOME = "C:\Users\pamii\.jdks\temurin-21.0.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

.\gradlew.bat run