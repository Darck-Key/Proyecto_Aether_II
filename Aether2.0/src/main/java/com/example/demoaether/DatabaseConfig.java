package com.example.demoaether;

/**
 * Lee la configuracion de MySQL sin quemar credenciales en el codigo.
 *
 * Variables de entorno soportadas:
 * - AETHER_DB_ENABLED=true
 * - AETHER_DB_URL=jdbc:mysql://localhost:3306/aether?serverTimezone=UTC
 * - AETHER_DB_USER=root
 * - AETHER_DB_PASSWORD=tu_password
 */
public class DatabaseConfig {

    private final boolean enabled;
    private final String url;
    private final String user;
    private final String password;

    private DatabaseConfig(boolean enabled, String url, String user, String password) {
        this.enabled = enabled;
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static DatabaseConfig fromEnvironment() {
        // Lee variables de entorno o propiedades JVM; evita escribir credenciales sensibles en el codigo.
        boolean enabled = Boolean.parseBoolean(read("AETHER_DB_ENABLED", "true"));
        String url = read("AETHER_DB_URL", "jdbc:mysql://localhost:3306/aether?serverTimezone=UTC");
        String user = read("AETHER_DB_USER", "root");
        String password = read("AETHER_DB_PASSWORD", "");
        return new DatabaseConfig(enabled, url, user, password);
    }

    private static String read(String key, String fallback) {
        // Prioridad: variable de entorno, luego propiedad JVM, luego valor por defecto.
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return value == null || value.isBlank() ? fallback : value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
