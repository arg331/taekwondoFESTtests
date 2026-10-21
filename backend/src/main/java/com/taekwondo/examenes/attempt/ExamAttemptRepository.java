package com.taekwondo.examenes.attempt;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /** Intentos de un alumno registrado en un examen. */
    List<ExamAttempt> findAllByExamIdAndStudentUserId(Long examId, Long studentUserId);

    /** Intentos anónimos con ese nombre, sin distinguir mayúsculas, tildes ni espacios (RN-08): studentKey = nameKey. */
    List<ExamAttempt> findAllByExamIdAndStudentUserIdIsNullAndStudentKey(Long examId, String studentKey);

    /** Intentos sin entregar: los caducados se corrigen solos (RF-24). */
    List<ExamAttempt> findAllBySubmittedAtIsNull();

    List<ExamAttempt> findAllByExamIdAndSubmittedAtIsNull(Long examId);

    /**
     * Marca el intento como entregado si nadie lo ha hecho antes. Si dos peticiones lo intentan a la vez
     * (un doble clic, o la tarea periódica y una consulta del profesor), solo una lo consigue: devuelve 1,
     * y la otra, 0. Así cada intento tiene un único resultado.
     */
    @Modifying
    @Query("update ExamAttempt a set a.submittedAt = :at where a.id = :id and a.submittedAt is null")
    int markSubmitted(@Param("id") Long attemptId, @Param("at") LocalDateTime at);
}
