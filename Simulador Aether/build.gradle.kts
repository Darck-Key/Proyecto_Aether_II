plugins {
    java
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "3.1.5"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"
val junitPlatformVersion = "1.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("com.example.demoaether")
    mainClass.set("com.example.demoaether.HelloApplication")
}

tasks.register<JavaExec>("com.example.demoaether.Launcher.main()") {
    group = "application"
    description = "Alias compatible con la configuracion temporal que IntelliJ crea para Launcher.main()."
    mainClass.set("com.example.demoaether.Launcher")
    classpath = sourceSets["main"].runtimeClasspath
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {

    implementation("org.orekit:orekit:12.0.2")
    implementation("org.hipparchus:hipparchus-geometry:3.0")
    implementation("org.hipparchus:hipparchus-ode:3.0")
    runtimeOnly("com.mysql:mysql-connector-j:8.4.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${junitVersion}")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:${junitPlatformVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}
