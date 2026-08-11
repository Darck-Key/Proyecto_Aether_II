package com.example.demoaether;

import java.time.LocalDateTime;

/**
 * Fila de evento de mision.
 *
 * Quien la crea:
 * - MySqlAetherRepository al leer mission_events.
 * - PendingDatabaseRepository si MySQL esta pendiente.
 *
 * Quien la usa:
 * - HelloController.refreshRecentEvents() para mostrar eventos recientes en la interfaz.
 */
public class MissionEventEntry {

    private final long id;
    private final String eventType;
    private final String description;
    private final LocalDateTime executedAt;

    public MissionEventEntry(long id, String eventType, String description, LocalDateTime executedAt) {
        // Guarda tipo, descripcion y fecha del evento operativo.
        this.id = id;
        this.eventType = eventType;
        this.description = description;
        this.executedAt = executedAt;
    }

    public long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }
}
