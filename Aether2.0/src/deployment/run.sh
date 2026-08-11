#!/bin/sh
set -u

# Resuelve la carpeta real del script para que el paquete pueda moverse.
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

if [ ! -f "Aether2.0.jar" ]; then
    echo "ERROR: No se encontro Aether2.0.jar en $SCRIPT_DIR" >&2
    exit 1
fi

if [ ! -f "orekit-data.zip" ]; then
    echo "ERROR: No se encontro orekit-data.zip en $SCRIPT_DIR" >&2
    exit 1
fi

if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java no esta instalado o no aparece en PATH." >&2
    echo "Instale JDK 17 o una version posterior y vuelva a intentarlo." >&2
    exit 1
fi

# Launcher carga JavaFX; la propiedad indica a Orekit donde estan sus datos.
exec java "-Daether.orekit.data=$SCRIPT_DIR/orekit-data.zip" -jar "Aether2.0.jar" "$@"
