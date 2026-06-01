package com.taekwondo.examenes.infrastructure.persistence.jpa.entity;

import com.taekwondo.examenes.domain.model.UserRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entidad de persistencia para User. NO es la entidad de dominio.
 *
 * Restricciones a nivel BD:
 *  - username UNIQUE
 *  - email UNIQUE
 *  - role como STRING (no ordinal, para resistir cambios en el enum)
 *
 * Las conversiones User <-> UserJpaEntity las hace UserMapper.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // JPA exige constructor sin args
    protected UserJpaEntity() {}

    public UserJpaEntity(Long id, String username, String email, String passwordHash,
                          String displayName, UserRole role, boolean active,
                          LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId()                  { return id; }
    public String getUsername()          { return username; }
    public String getEmail()             { return email; }
    public String getPasswordHash()      { return passwordHash; }
    public String getDisplayName()       { return displayName; }
    public UserRole getRole()            { return role; }
    public boolean isActive()            { return active; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    public void setId(Long id)                              { this.id = id; }
    public void setUsername(String username)                { this.username = username; }
    public void setEmail(String email)                      { this.email = email; }
    public void setPasswordHash(String passwordHash)        { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName)          { this.displayName = displayName; }
    public void setRole(UserRole role)                      { this.role = role; }
    public void setActive(boolean active)                   { this.active = active; }
    public void setCreatedAt(LocalDateTime createdAt)       { this.createdAt = createdAt; }
}
