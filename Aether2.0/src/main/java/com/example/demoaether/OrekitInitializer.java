package com.example.demoaether;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.orekit.data.DataContext;
import org.orekit.data.DataProvider;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.ZipJarCrawler;

/**
 * Inicializador de datos Orekit.
 *
 * Quien llama:
 * - HelloController.calculateOrbit().
 * - MissionSimulator.calculateInitialState().
 * - OrekitTrajectoryPlanner.precompute().
 *
 * Que hace:
 * - En el paquete desplegable, localiza orekit-data.zip junto a Aether2.0.jar.
 * - En desarrollo, conserva la lectura de src/main/resources/orekit-data.
 * - Registra la fuente en DataProvidersManager para cargar efemerides y modelos.
 */
public class OrekitInitializer {

    private static final String DATA_PROPERTY = "aether.orekit.data";
    private static final String DATA_ZIP = "orekit-data.zip";
    private static boolean initialized;

    /**
     * Inicializa el contexto de datos de Orekit una sola vez por sesion.
     */
    public static synchronized void initialize() {
        // Metodo sincronizado para cargar Orekit una sola vez durante toda la sesion.
        if (initialized) {
            return;
        }

        try {
            OrekitDataSource dataSource = locateOrekitData();

            DataProvidersManager manager =
                    DataContext.getDefault()
                            .getDataProvidersManager();

            manager.addProvider(dataSource.provider());

            // Fuerza una lectura real para detectar de inmediato un ZIP vacio o incompleto.
            DataContext.getDefault().getTimeScales().getUTC();

            initialized = true;
            System.out.println("Orekit inicializado correctamente desde: " + dataSource.description());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Error inicializando Orekit",
                    e
            );
        }
    }

    private static OrekitDataSource locateOrekitData() throws URISyntaxException {
        // El script puede indicar una ruta explicita para que el paquete sea totalmente portable.
        String configuredPath = System.getProperty(DATA_PROPERTY);
        if (configuredPath != null && !configuredPath.isBlank()) {
            File configuredData = new File(configuredPath).getAbsoluteFile();
            if (!configuredData.exists()) {
                throw new IllegalStateException(
                        "La ruta indicada en -D" + DATA_PROPERTY + " no existe: " + configuredData
                );
            }
            return createSource(configuredData);
        }

        // Si se ejecuta con java -jar, busca el ZIP en la misma carpeta que el JAR.
        File applicationData = new File(locateApplicationDirectory(), DATA_ZIP);
        if (applicationData.isFile()) {
            return createSource(applicationData);
        }

        // Permite iniciar desde una terminal situada en la carpeta del paquete.
        File workingDirectoryData = new File(DATA_ZIP).getAbsoluteFile();
        if (workingDirectoryData.isFile()) {
            return createSource(workingDirectoryData);
        }

        // Conserva el flujo utilizado por IntelliJ y por las pruebas del proyecto.
        File sourceTreeData = new File("src/main/resources/orekit-data").getAbsoluteFile();
        if (sourceTreeData.isDirectory()) {
            return createSource(sourceTreeData);
        }

        // Admite una futura copia de orekit-data.zip incluida directamente en el classpath.
        URL packagedResource = OrekitInitializer.class.getResource("/" + DATA_ZIP);
        if (packagedResource != null) {
            return new OrekitDataSource(
                    new ZipJarCrawler(packagedResource),
                    packagedResource.toExternalForm()
            );
        }

        URL directoryResource = OrekitInitializer.class.getResource("/orekit-data");
        if (directoryResource != null && "file".equals(directoryResource.getProtocol())) {
            return createSource(new File(directoryResource.toURI()));
        }

        throw new IllegalStateException(
                "No se encontro Orekit. Coloque " + DATA_ZIP
                        + " junto a Aether2.0.jar o indique -D" + DATA_PROPERTY + "=<ruta>."
        );
    }

    private static OrekitDataSource createSource(File data) {
        // ZipJarCrawler lee el archivo distribuido; DirectoryCrawler mantiene el modo de desarrollo.
        if (data.isFile()) {
            return new OrekitDataSource(new ZipJarCrawler(data), data.getAbsolutePath());
        }
        if (data.isDirectory()) {
            return new OrekitDataSource(new DirectoryCrawler(data), data.getAbsolutePath());
        }
        throw new IllegalStateException("La fuente de datos Orekit no es valida: " + data);
    }

    private static File locateApplicationDirectory() throws URISyntaxException {
        // CodeSource apunta al JAR desplegado o a target/classes durante el desarrollo.
        URL codeLocation = OrekitInitializer.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
        File codeSource = new File(codeLocation.toURI()).getAbsoluteFile();
        return codeSource.isFile() ? codeSource.getParentFile() : codeSource;
    }

    /** Relaciona el proveedor que consume Orekit con una ruta legible para diagnostico. */
    private record OrekitDataSource(DataProvider provider, String description) {
    }
}
