@echo off
setlocal

rem Este script inicia AETHER desde su propia carpeta, aunque el paquete se mueva.
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

where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java no esta instalado o no aparece en PATH.
    echo Instale JDK 17 o una version posterior y vuelva a intentarlo.
    pause
    exit /b 1
)

rem Launcher carga JavaFX; la propiedad indica a Orekit donde estan sus datos.
java "-Daether.orekit.data=%CD%\orekit-data.zip" -jar "Aether2.0.jar" %*
set "AETHER_EXIT=%ERRORLEVEL%"

if not "%AETHER_EXIT%"=="0" (
    echo.
    echo AETHER termino con el codigo de error %AETHER_EXIT%.
    pause
)

exit /b %AETHER_EXIT%
