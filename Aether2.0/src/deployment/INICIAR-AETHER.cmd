@echo off
setlocal

rem Inicio grafico para Windows: abre AETHER sin mantener una consola visible.
cd /d "%~dp0"

if not exist "Aether2.0.jar" (
    echo ERROR: No se encontro Aether2.0.jar en %CD%
    pause
    exit /b 1
)

if not exist "orekit-data.zip" (
    echo ERROR: No se encontro orekit-data.zip en %CD%
    pause
    exit /b 1
)

where javaw.exe >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java no esta instalado o no aparece en PATH.
    echo Instale JDK 17 o una version posterior y vuelva a intentarlo.
    pause
    exit /b 1
)

rem javaw inicia JavaFX directamente; run.bat queda disponible para ver diagnosticos.
start "" /D "%CD%" javaw.exe "-Daether.orekit.data=%CD%\orekit-data.zip" -jar "Aether2.0.jar"
if errorlevel 1 (
    echo ERROR: Windows no pudo iniciar AETHER.
    echo Ejecute run.bat para ver el detalle tecnico.
    pause
    exit /b 1
)

exit /b 0
