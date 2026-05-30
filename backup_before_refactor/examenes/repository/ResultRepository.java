package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para operaciones de base de datos con Result
 */
@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    /**
     * Buscar resultados por ID de examen
     */
    List<Result> findByExamId(Long examId);

    /**
     * Buscar resultados por nombre de estudiante
     */
    List<Result> findByStudentNameContainingIgnoreCase(String name);

    /**
     * Buscar resultados por nombre de estudiante y examen
     */
    List<Result> findByStudentNameAndExamId(String studentName, Long examId);

    /**
     * Verificar si un estudiante ya hizo un examen específico
     */
    boolean existsByStudentNameAndExamId(String studentName, Long examId);

    /**
     * Buscar resultados en un rango de fechas
     */
    List<Result> findByCompletedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Buscar resultados por club
     */
    List<Result> findByStudentClubContainingIgnoreCase(String club);

    /**
     * Obtener resultados ordenados por puntuación descendente
     */
    List<Result> findByExamIdOrderByScoreDesc(Long examId);

    /**
     * Estadísticas: Promedio de puntuación por examen
     */
    @Query("SELECT AVG(r.score) FROM Result r WHERE r.exam.id = :examId")
    Double getAverageScoreByExam(Long examId);

    /**
     * Estadísticas: Contar aprobados (score >= 70)
     */
    @Query("SELECT COUNT(r) FROM Result r WHERE r.exam.id = :examId AND r.score >= :passingScore")
    long countPassedResults(Long examId, Integer passingScore);

    /**
     * Estadísticas: Obtener mejores resultados
     */
    @Query("SELECT r FROM Result r ORDER BY r.score DESC, r.completedAt ASC")
    List<Result> findTopResults();

    /**
     * Buscar últimos N resultados
     */
    List<Result> findTop10ByOrderByCompletedAtDesc();

    /**
     * Contar total de exámenes realizados
     */
    @Query("SELECT COUNT(r) FROM Result r")
    long countTotalResults();
}
