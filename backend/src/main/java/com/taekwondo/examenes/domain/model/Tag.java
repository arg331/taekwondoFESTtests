package com.taekwondo.examenes.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad de dominio: Tag (etiqueta para categorizar preguntas)
 *
 * REGLAS DE NEGOCIO:
 *  - Un tag tiene nombre y color, ambos no vacíos
 *  - Pertenece a un profesor (ownerId)
 *  - Los tags NO se pueden eliminar, solo renombrar
 *
 * NOTA: Esta clase es POJO puro. NO usa anotaciones JPA ni Lombok.
 *       Las anotaciones técnicas van en la capa de infraestructura.
 */
public final class Tag {

    private final Long id;
    private String name;
    private String color;
    private final Long ownerId;
    private final LocalDateTime createdAt;

    /**
     * Constructor para tag NUEVO (sin ID asignado todavía).
     * El ID lo asigna la base de datos al persistirse.
     */
    public static Tag createNew(String name, String color, Long ownerId) {
        validateName(name);
        validateColor(color);
        validateOwner(ownerId);
        return new Tag(null, name, color, ownerId, LocalDateTime.now());
    }

    /**
     * Constructor para reconstruir un tag YA persistido (desde BD).
     * Usado por la capa de infraestructura.
     */
    public static Tag reconstitute(Long id, String name, String color,
                                    Long ownerId, LocalDateTime createdAt) {
        Objects.requireNonNull(id, "id no puede ser null en reconstitución");
        return new Tag(id, name, color, ownerId, createdAt);
    }

    private Tag(Long id, String name, String color, Long ownerId, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    /**
     * Operación de negocio: renombrar el tag.
     * Aplica las mismas validaciones que la creación.
     */
    public void rename(String newName) {
        validateName(newName);
        this.name = newName;
    }

    /**
     * Operación de negocio: cambiar el color.
     */
    public void changeColor(String newColor) {
        validateColor(newColor);
        this.color = newColor;
    }

    // ──────────────────────────────────────────────────
    // Validaciones (reglas de negocio puras)
    // ──────────────────────────────────────────────────

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del tag no puede estar vacío");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("El nombre del tag no puede tener más de 50 caracteres");
        }
    }

    private static void validateColor(String color) {
        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException("El color del tag no puede estar vacío");
        }
        if (!color.matches("^#[0-9A-Fa-f]{6}$")) {
            throw new IllegalArgumentException("El color debe ser un hex válido (ej: #3B8BD4)");
        }
    }

    private static void validateOwner(Long ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("El tag debe tener un propietario");
        }
    }

    // ──────────────────────────────────────────────────
    // Getters (sin setters: inmutabilidad controlada)
    // ──────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getColor()             { return color; }
    public Long getOwnerId()             { return ownerId; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    // ──────────────────────────────────────────────────
    // Equals/hashCode basados en identidad (ID)
    // ──────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag tag)) return false;
        return Objects.equals(id, tag.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}