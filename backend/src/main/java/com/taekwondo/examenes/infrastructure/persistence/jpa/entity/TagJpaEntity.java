package com.taekwondo.examenes.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidad de persistencia para Tag. NO es la entidad de dominio.
 *
 * Vive en infraestructura y tiene las anotaciones JPA que Hibernate
 * necesita. El dominio (com.taekwondo.examenes.domain.model.Tag)
 * queda intacto, sin acoplamiento a JPA.
 *
 * Conversiones Tag <-> TagJpaEntity las hace TagMapper.
 */
@Entity
@Table(name = "tags",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "owner_id"}))
public class TagJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 7)   // formato "#RRGGBB"
    private String color;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // JPA exige constructor sin args
    protected TagJpaEntity() {}

    public TagJpaEntity(Long id, String name, String color, Long ownerId, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getColor()             { return color; }
    public Long getOwnerId()             { return ownerId; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    public void setId(Long id)                       { this.id = id; }
    public void setName(String name)                 { this.name = name; }
    public void setColor(String color)               { this.color = color; }
    public void setOwnerId(Long ownerId)             { this.ownerId = ownerId; }
    public void setCreatedAt(LocalDateTime createdAt){ this.createdAt = createdAt; }
}
