package com.taekwondo.examenes.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad de dominio: User.
 *
 * Cubre los DOS tipos de usuario:
 *  - ADMIN: profesores
 *  - STUDENT: estudiantes registrados
 *
 * Lo distingue el campo role (UserRole).
 *
 * Decisión de seguridad: la entidad NUNCA conoce la contraseña en claro.
 * Solo trabaja con el hash. Quien crea el User debe haber hasheado
 * previamente la contraseña con el puerto PasswordHasher.
 */
public final class User {

    private final Long id;
    private final String username;
    private String email;
    private String passwordHash;
    private String displayName;
    private UserRole role;
    private boolean active;
    private final LocalDateTime createdAt;

    // ──────────────────────────────────────────────────
    // Factory methods
    // ──────────────────────────────────────────────────

    public static User createNew(String username,
                                  String email,
                                  String passwordHash,
                                  String displayName,
                                  UserRole role) {
        validateUsername(username);
        validateEmail(email);
        validatePasswordHash(passwordHash);
        validateDisplayName(displayName);
        validateRole(role);
        return new User(null, username, email, passwordHash, displayName, role,
                true, LocalDateTime.now());
    }

    public static User reconstitute(Long id,
                                     String username,
                                     String email,
                                     String passwordHash,
                                     String displayName,
                                     UserRole role,
                                     boolean active,
                                     LocalDateTime createdAt) {
        Objects.requireNonNull(id, "id no puede ser null en reconstitución");
        return new User(id, username, email, passwordHash, displayName, role,
                active, createdAt);
    }

    private User(Long id, String username, String email, String passwordHash,
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

    // ──────────────────────────────────────────────────
    // Operaciones de negocio
    // ──────────────────────────────────────────────────

    public void changeEmail(String newEmail) {
        validateEmail(newEmail);
        this.email = newEmail;
    }

    public void changeDisplayName(String newDisplayName) {
        validateDisplayName(newDisplayName);
        this.displayName = newDisplayName;
    }

    public void changePasswordHash(String newPasswordHash) {
        validatePasswordHash(newPasswordHash);
        this.passwordHash = newPasswordHash;
    }

    public void deactivate() { this.active = false; }
    public void activate()   { this.active = true; }

    public boolean isAdmin()   { return role == UserRole.ADMIN; }
    public boolean isStudent() { return role == UserRole.STUDENT; }

    // ──────────────────────────────────────────────────
    // Validaciones
    // ──────────────────────────────────────────────────

    private static void validateUsername(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("El username no puede estar vacío");
        if (username.length() < 3 || username.length() > 50)
            throw new IllegalArgumentException("El username debe tener entre 3 y 50 caracteres");
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("El email no puede estar vacío");
        if (!email.contains("@"))
            throw new IllegalArgumentException("El email no tiene formato válido");
    }

    private static void validatePasswordHash(String hash) {
        if (hash == null || hash.isBlank())
            throw new IllegalArgumentException("El hash de contraseña no puede estar vacío");
    }

    private static void validateDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank())
            throw new IllegalArgumentException("El nombre a mostrar no puede estar vacío");
    }

    private static void validateRole(UserRole role) {
        if (role == null)
            throw new IllegalArgumentException("El rol no puede ser null");
    }

    // ──────────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────────

    public Long getId()                  { return id; }
    public String getUsername()          { return username; }
    public String getEmail()             { return email; }
    public String getPasswordHash()      { return passwordHash; }
    public String getDisplayName()       { return displayName; }
    public UserRole getRole()            { return role; }
    public boolean isActive()            { return active; }
    public LocalDateTime getCreatedAt()  { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return Objects.equals(id, u.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
