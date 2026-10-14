package com.taekwondo.examenes.user;

import com.taekwondo.examenes.club.Club;
import com.taekwondo.examenes.common.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    /** Opcional (RF-01): quien no lo tiene recupera la cuenta a través del administrador. */
    @Column(length = 200)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    /** Como mucho un club (RF-39); null = ninguno. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id")
    private Club club;

    @Column(nullable = false)
    private boolean active = true;

    /** Contraseña temporal puesta por el administrador: hay que cambiarla antes de usar la app (RF-33). */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    /** Sube con cada cambio de contraseña; las sesiones de versiones anteriores dejan de valer. */
    @Column(name = "credentials_version", nullable = false)
    private int credentialsVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public User(String username, String email, String passwordHash,
                String displayName, UserRole role) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isTeacher() {
        return role == UserRole.TEACHER;
    }

    public boolean belongsTo(Club other) {
        return club != null && club.getId().equals(other.getId());
    }

    /** Entrar en un club deja el anterior: se pertenece como mucho a uno. */
    public void joinClub(Club newClub) {
        this.club = newClub;
    }

    public void leaveClub() {
        this.club = null;
    }

    /** El propio usuario cambia su contraseña (RF-06): deja de ser temporal y se cierran sus otras sesiones. */
    public void changePassword(String newPasswordHash) {
        passwordHash = newPasswordHash;
        mustChangePassword = false;
        credentialsVersion++;
    }

    /** El administrador pone una contraseña temporal (RF-35): habrá que cambiarla y se cierran sus sesiones. */
    public void resetPassword(String temporaryPasswordHash) {
        passwordHash = temporaryPasswordHash;
        mustChangePassword = true;
        credentialsVersion++;
    }

    /** RF-05: no puede entrar y sus sesiones se cierran; al reactivarlo no vuelven. Repetirlo no cambia nada. */
    public void deactivate() {
        if (active) {
            active = false;
            credentialsVersion++;
        }
    }

    public void activate() {
        active = true;
    }

    public void changeRole(UserRole newRole) {
        if (role == newRole) {
            throw new BusinessRuleException("El usuario ya tiene el rol " + newRole);
        }
        role = newRole;
    }
}
