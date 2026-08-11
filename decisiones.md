<h2 alinear="centro">
    Registro de decisiones del proyecto
</h2>

<h3>
    Uso de la inteligencia artificial
</h3>

<p>
    En este apartado se detallara como se integraron las herramientas de inteligencia artificial en nuestro proyecto.
</p>

<br>

<h3>
    Partes Asistidas por IA
</h3>

<p>
    La inteligencia artificial fue utilizada como copiloto de desarrollo y redacción en las siguientes áreas:
</p>

<p>
        <strong>- Explicaciones de diversas partes del documento SRS:</strong> La IA nos asistió explicando partes o apartados del documento los cuales no estaban dentro de nuestros conocimientos.
</p>
<br>

<p>
        <strong>- Optimización y revisión de códigos:</strong> La IA nos ayudo con algunos problemas de optimización y con la revicion del codigo, gracias a eso nos ahorro mucho tiempo.
</p>
<br>
<p>
        <strong>- Redacción y documentación:</strong> en diversos contextos la IA fue utilizada para darle un mejor formato y claridad a lo que queríamos expresar.
</p>

<br>

<h3>
    Refinamientos requeridos
</h3>

<p>
    Si bien, la IA nos proporcionó una base sólida, realizaron las siguientes intervenciones humanas criticas:
</p>
<br>

<p>
        - Los códigos fueron revisados por el equipo de desarrollo antes de ser utilizado, evitando que este no se alineara con lo establecido.
</p>
<br>

<p>
        - La documentación fue leída y corregida, evitando redundancias y tecnicismos muy complejos.
</p>
<br>

<p>
       - Corrección de bibliotecas que pudieran estar obsoletas o que presentaban algún peligro para la seguridad de nuestro programa y equipos.
</p>

<br>

<h3>
    Herramientas utilizadas:
</h3>

<p> ChatGpt</p><br>
<p> Gemini</p><br>
<p> Codex</p><br>

<p>
    La inteligencia artificial fue utilizada como una herramienta de apoyo durante el desarrollo del proyecto. Todas las decisiones finales, validaciones técnicas y aprobaciones fueron realizadas por los integrantes del equipo, garantizando la calidad y confiabilidad de los resultados obtenidos.
</p>


<h2 align="center">
    Registros de Decisiones de Arquitectura (ADR)
</h2>

<br>

<h3>
    ADR-001 - Estrategia híbrida para obtener la trayectoria de la misión
</h3>

<table>
    <tr>
        <th>Campo</th>
        <th>Contenido</th>
    </tr>
<tr>
        <td><b>Título y estado</b></td>
        <td>
            ADR-001 - Estrategia híbrida para obtener la trayectoria de la misión.
            <br>
            <b>Estado:</b> Aceptada
        </td>
    </tr>
    <tr>
     <td><b>Contexto</b></td>
        <td>
            AETHER debe representar el perfil nominal de Artemis II y, al mismo tiempo,
            permitir simulaciones con parámetros personalizados. La versión Aether2.2.zip
            incluye una efeméride de referencia en formato CCSDS OEM y también un
            planificador de trayectoria basado en Orekit.
            <br><br>
            Una trayectoria de referencia permite reproducir el caso nominal, pero por
            sí sola no responde a cambios introducidos por el usuario. Una propagación
            numérica, en cambio, permite generar escenarios personalizados, aunque depende
            de los modelos de fuerza y simplificaciones implementados.
            <br><br>
            En el código revisado,  MissionSimulator  decide la fuente de la
            trayectoria mediante  ArtemisReferenceTrajectoryLoader  y
             OrekitTrajectoryPlanner .
             ArtemisReferenceTrajectoryLoader.supports(config)  delega en
             MissionPresets.isArtemisIIReference(config) .
        </td>
    </tr>
    <tr>
        <td><b>Decisión</b></td>
        <td>
            Se adopta una estrategia híbrida. Cuando la configuración corresponde al
            perfil de referencia de Artemis II,  MissionSimulator  utiliza
             ArtemisReferenceTrajectoryLoader  para cargar la efeméride OEM
            incluida como recurso del proyecto.
            <br><br>
            Cuando la configuración no corresponde al perfil de referencia,
             MissionSimulator  utiliza
             OrekitTrajectoryPlanner.precompute(config)  para generar una
            trayectoria numérica.
            <br><br>
            Ambas rutas entregan una  MissionTrajectory  compuesta por
             MissionState , de modo que telemetría, reproducción,
            visualización y reportes puedan consumir un modelo interno común sin
            depender del origen de la trayectoria.
        </td>
    </tr>
    <tr>
        <td><b>Alternativas consideradas</b></td>
        <td>
            <b>Alternativas evaluadas:</b>
            <br><br>
            <b>1. Usar propagación numérica para todos los escenarios:</b>
            Se descartó como única estrategia porque el caso nominal dejaría de
            aprovechar directamente la efeméride de referencia incluida y dependería
            por completo de las simplificaciones del modelo propagado.
            <br><br>
            <b>2. Usar únicamente la efeméride OEM:</b>
            Se descartó porque una trayectoria fija no puede responder adecuadamente
            a configuraciones personalizadas de la misión.
            <br><br>
            <b>3. Implementar un motor orbital propio:</b>
            Se descartó por el mayor esfuerzo de desarrollo, riesgo de errores y
            complejidad de validación frente al uso de una biblioteca especializada
            como Orekit.
        </td>
    </tr>
    <tr>
        <td><b>Consecuencias</b></td>
        <td>
            <b>Consecuencias positivas:</b>
            <ul>
                <li>
                    El escenario nominal puede utilizar una trayectoria de referencia
                    incluida en la aplicación.
                </li>
                <li>
                    Las configuraciones personalizadas pueden generar una trayectoria propia.
                </li>
                <li>
                    La telemetría, reproducción, mapa y reportes consumen un mismo
                    modelo de trayectoria.
                </li>
                <li>
                    Se combina referencia nominal con flexibilidad de simulación.
                </li>
            </ul>
            <b>Consecuencias negativas y compromisos:</b>
            <ul>
                <li>
                    Deben mantenerse y probarse dos mecanismos diferentes para obtener
                    la trayectoria.
                </li>
                <li>
                    El sistema debe reconocer correctamente cuándo una configuración
                    corresponde al perfil de referencia.
                </li>
                <li>
                    Las trayectorias personalizadas dependen de las simplificaciones
                    y modelos físicos configurados en el propagador.
                </li>
            </ul>
        </td>
    </tr>
</table>

<br>
<hr>
<br>

<h3>
    ADR-002 - Separación entre el cálculo orbital y la reproducción gráfica
</h3>

<table>
    <tr>
        <th>Campo</th>
        <th>Contenido</th>
    </tr>
    <tr>
        <td><b>Título y estado</b></td>
        <td>
            ADR-002 - Separación entre el cálculo orbital y la reproducción gráfica.
            <br>
            <b>Estado:</b> Aceptada
        </td>
    </tr>
    <tr>
        <td><b>Contexto</b></td>
        <td>
            La propagación orbital puede requerir cálculos costosos y generar numerosos
            estados. JavaFX concentra la actualización visual en su hilo de aplicación,
            por lo que realizar el cálculo físico directamente allí podría afectar la
            capacidad de respuesta de la interfaz.
            <br><br>
            AETHER también necesita controles de reproducción como iniciar, pausar,
            reanudar y cambiar la escala temporal. Esos controles pertenecen a la
            presentación y no deben modificar la trayectoria física calculada.
            <br><br>
            En Aether2.2.zip,  HelloController ,
             MissionSimulator ,  TrajectoryPlayback  y
             SimulationListener  distribuyen estas responsabilidades.
        </td>
    </tr>
    <tr>
        <td><b>Decisión</b></td>
        <td>
            Se separa el precálculo físico de la reproducción visual.
             HelloController  ejecuta
             MissionSimulator.prepareTrajectory()  dentro de un hilo
            denominado  aether-orekit-precalculation .
            <br><br>
            Al terminar el cálculo, la interfaz utiliza
             Platform.runLater()  para iniciar la reproducción en JavaFX.
             TrajectoryPlayback  extiende  AnimationTimer  y
            recorre los estados ya calculados, aplicando la velocidad de reproducción
            sin volver a propagar la órbita.
            <br><br>
             MissionSimulator  conserva un recorrido con
             Thread.sleep()  para compatibilidad con pruebas sin JavaFX,
            pero la ruta de interfaz revisada utiliza  TrajectoryPlayback 
            como mecanismo de reproducción.
        </td>
    </tr>
    <tr>
        <td><b>Alternativas consideradas</b></td>
        <td>
            <b>Alternativas evaluadas:</b>
            <br><br>
            <b>1. Propagar directamente en el hilo de JavaFX:</b>
            Se descartó porque una operación prolongada podría bloquear o volver poco
            responsiva la interfaz.
            <br><br>
            <b>2. Calcular y mostrar cada estado de forma acoplada:</b>
            Se descartó porque uniría la velocidad del cálculo físico con la velocidad
            de representación visual.
            <br><br>
            <b>3. Recalcular la trayectoria al cambiar la escala de tiempo:</b>
            Se descartó porque la escala temporal es una característica de reproducción
            y no un cambio en los parámetros físicos de la misión.
            <br><br>
            <b>4. Usar Thread.sleep() como mecanismo principal de animación:</b>
            Se descartó para la interfaz JavaFX porque acoplaría la experiencia visual
            al recorrido directo de las muestras; se conserva únicamente una ruta de
            compatibilidad para pruebas sin JavaFX.
        </td>
    </tr>
    <tr>
        <td><b>Consecuencias</b></td>
        <td>
            <b>Consecuencias positivas:</b>
            <ul>
                <li>
                    El cálculo orbital se mantiene fuera del hilo gráfico principal.
                </li>
                <li>
                    Pausar, reanudar o cambiar la velocidad no altera los resultados físicos.
                </li>
                <li>
                     TrajectoryPlayback  permite una reproducción continua
                    de los estados precalculados.
                </li>
                <li>
                    La misma trayectoria puede alimentar telemetría, mapa y otros componentes.
                </li>
            </ul>
            <b>Consecuencias negativas y compromisos:</b>
            <ul>
                <li>
                    Existe un tiempo inicial de espera mientras se prepara la trayectoria.
                </li>
                <li>
                    Los estados deben mantenerse disponibles durante la reproducción.
                </li>
                <li>
                    Se requiere coordinación entre el hilo de precálculo y el hilo de JavaFX.
                </li>
                <li>
                    La separación introduce componentes adicionales para cálculo,
                    reproducción y notificación.
                </li>
            </ul>
        </td>
    </tr>
</table>

<br>
<hr>
<br>

<h3>
    ADR-003 - Abstracción de persistencia con MySQL y repositorio de respaldo
</h3>

<table>
    <tr>
        <th>Campo</th>
        <th>Contenido</th>
    </tr>
    <tr>
        <td><b>Título y estado</b></td>
        <td>
            ADR-003 - Abstracción de persistencia con MySQL y repositorio de respaldo.
            <br>
            <b>Estado:</b> Aceptada
        </td>
    </tr>
    <tr>
        <td><b>Contexto</b></td>
        <td>
            AETHER registra información asociada a las ejecuciones del simulador.
            MySQL ofrece persistencia entre sesiones, pero las funciones principales
            de simulación no deberían quedar bloqueadas si la base de datos no está
            configurada o disponible.
            <br><br>
            Acoplar directamente la interfaz con JDBC/MySQL también mezclaría
            responsabilidades de presentación, infraestructura y persistencia.
            <br><br>
            La implementación revisada incorpora  AetherRepository ,
             RepositoryFactory ,  MySqlAetherRepository ,
             PendingDatabaseRepository  y  DatabaseConfig 
            para aislar esta responsabilidad.
        </td>
    </tr>
    <tr>
        <td><b>Decisión</b></td>
        <td>
            Se define  AetherRepository  como contrato común de persistencia.
             RepositoryFactory  selecciona la implementación que utilizará
            la aplicación.
            <br><br>
            Si MySQL está habilitado y disponible, se utiliza
             MySqlAetherRepository . Si la base de datos no está disponible
            o la configuración falla, se utiliza
             PendingDatabaseRepository  como respaldo temporal en memoria.
            <br><br>
             DatabaseConfig  centraliza los datos de conexión y permite leer
            valores desde variables de entorno o propiedades de la JVM, evitando fijar
            credenciales directamente en la lógica de la interfaz.
        </td>
    </tr>
    <tr>
        <td><b>Alternativas consideradas</b></td>
        <td>
            <b>Alternativas evaluadas:</b>
            <br><br>
            <b>1. Acceder a MySQL directamente desde HelloController:</b>
            Se descartó porque aumentaría el acoplamiento entre interfaz, persistencia
            y manejo de conexiones.
            <br><br>
            <b>2. Hacer MySQL obligatorio para iniciar AETHER:</b>
            Se descartó porque una falla externa de infraestructura impediría usar
            funciones del simulador que pueden operar sin persistencia permanente.
            <br><br>
            <b>3. Usar únicamente almacenamiento en memoria:</b>
            Se descartó como solución principal porque los datos no se conservarían
            entre ejecuciones.
            <br><br>
            <b>4. Usar únicamente archivos locales:</b>
            Se descartó como estrategia principal porque no ofrece la misma organización
            estructurada y capacidad de consulta de una base de datos relacional para
            los registros manejados por el sistema.
        </td>
    </tr>
    <tr>
        <td><b>Consecuencias</b></td>
        <td>
            <b>Consecuencias positivas:</b>
            <ul>
                <li>
                    La lógica principal depende de una interfaz de repositorio y no
                    directamente de JDBC.
                </li>
                <li>
                    AETHER puede continuar operando cuando MySQL no está disponible.
                </li>
                <li>
                    La implementación de persistencia puede sustituirse con menor
                    impacto en otros módulos.
                </li>
                <li>
                    La configuración de la base de datos queda centralizada y las
                    credenciales pueden mantenerse fuera del código.
                </li>
            </ul>
            <b>Consecuencias negativas y compromisos:</b>
            <ul>
                <li>
                    Los datos guardados solo en  PendingDatabaseRepository 
                    se pierden al cerrar la aplicación.
                </li>
                <li>
                    Deben mantenerse dos implementaciones del mismo contrato.
                </li>
                <li>
                    El modo temporal no ofrece la misma durabilidad que MySQL.
                </li>
                <li>
                    La interfaz debe distinguir claramente entre persistencia permanente
                    y almacenamiento temporal.
                </li>
            </ul>
        </td>
    </tr>
</table>
