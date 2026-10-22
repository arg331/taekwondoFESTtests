package com.taekwondo.examenes.club;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Club de taekwondo (RF-38). El nombre es único sin distinguir mayúsculas (índice en la migración V4).
 * Los miembros son los usuarios cuyo club es este: cada usuario pertenece como mucho a uno.
 */
@Entity
@Table(name = "clubs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Club {

    public static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = MAX_NAME_LENGTH)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Club(String name) {
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void rename(String newName) {
        this.name = newName;
    }
}
