package com.example.demoaether;

/**
 * Decide que repositorio usar al iniciar la aplicacion.
 *
 * Si AETHER_DB_ENABLED=true intenta usar MySQL.
 * Si falta driver, credenciales o servidor, vuelve al modo pendiente sin romper la interfaz.
 */
public class RepositoryFactory {

    private RepositoryFactory() {
    }

    public static AetherRepository createRepository() {
        // Punto unico donde la app decide entre MySqlAetherRepository y PendingDatabaseRepository.
        DatabaseConfig config = DatabaseConfig.fromEnvironment();
        if (!config.isEnabled()) {
            return new PendingDatabaseRepository("AETHER_DB_ENABLED no esta activo");
        }

        try {
            MySqlAetherRepository repository = new MySqlAetherRepository(config);
            if (repository.isAvailable()) {
                return repository;
            }
            return new PendingDatabaseRepository("MySQL no responde");
        } catch (RuntimeException exception) {
            return new PendingDatabaseRepository(exception.getMessage());
        }
    }
}
