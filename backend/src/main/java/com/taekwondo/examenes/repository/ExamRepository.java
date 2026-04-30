package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para operaciones de base de datos con Exam
 */
@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    /**
     * Buscar examen por código único
     */
    Optional<Exam> findByCode(String code);

    /**
     * Verificar si existe un examen con ese código
     */
    boolean existsByCode(String code);

    /**
     * Buscar exámenes activos
     */
    List<Exam> findByActiveTrue();

    /**
     * Buscar exámenes activos que no hayan expirado
     */
    @Query("SELECT e FROM Exam e WHERE e.active = true AND e.expiresAt > :now")
    List<Exam> findActiveAndNotExpired(LocalDateTime now);

    /**
     * Buscar último examen activo
     */
    @Query("SELECT e FROM Exam e WHERE e.active = true AND e.expiresAt > :now ORDER BY e.createdAt DESC")
    Optional<Exam> findLatestActiveExam(LocalDateTime now);

    /**
     * Buscar exámenes creados en un rango de fechas
     */
    List<Exam> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Contar exámenes activos
     */
    long countByActiveTrue();

    /**
     * Buscar exámenes que expiran pronto (para limpieza)
     */
    @Query("SELECT e FROM Exam e WHERE e.expiresAt < :threshold AND e.active = true")
    List<Exam> findExpiringExams(LocalDateTime threshold);
}
