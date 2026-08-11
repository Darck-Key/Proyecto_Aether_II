param(
    [string]$User = "root",
    [string]$Password = "",
    [string]$HostName = "localhost",
    [int]$Port = 3306
)

$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $MyInvocation.MyCommand.Path
$schema = Join-Path $project "mysql-schema.sql"

$mysql = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysql) {
    Write-Host "No se encontro mysql.exe en PATH. Instala MySQL Server/Workbench o agrega mysql.exe al PATH." -ForegroundColor Yellow
    exit 1
}

$args = @("-h", $HostName, "-P", $Port, "-u", $User)
if ($Password -ne "") {
    $args += "-p$Password"
}
$args += @("--default-character-set=utf8mb4")

Get-Content -LiteralPath $schema | & $mysql.Source @args
Write-Host "Base de datos AETHER inicializada correctamente." -ForegroundColor Green