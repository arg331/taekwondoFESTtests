package com.taekwondo.examenes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad Tag - Etiquetas para categorizar preguntas
 * 
 * REGLAS:
 * - Cada tag pertenece a un profesor (ownerId)
 * - Los tags NO se pueden eliminar (solo renombrar)
 * - Al importar CSV, se detectan conflictos de nombres
 */
@Entity
@Table(
    name = "tags",
    uniqueConstraints = @UniqueConstraint(columnNames = {"name", "owner_id"})
    // Un profesor no puede tener dos tags con el mismo nombre
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String name; // Ejemplo: "Puntuación", "Round Senior"
    
    @Column(length = 7)
    private String color; // Hex color para UI (ej: "#3B8BD4")
    
    @Column(nullable = false, name = "owner_id")
    private Long ownerId; // ID del profesor propietario
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Constructor sin ID (para crear nuevos)
    public Tag(String name, String color, Long ownerId) {
        this.name = name;
        this.color = color;
        this.ownerId = ownerId;
    }
}
