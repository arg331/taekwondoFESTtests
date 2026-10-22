package com.taekwondo.examenes.attempt;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    /** Intentos de un alumno registrado en un examen. */
    List<ExamAttempt> findAllByExamIdAndStudentUserId(Long examId, Long studentUserId);

    /** Intentos anónimos con ese nombre, sin distinguir mayúsculas (RN-08). */
    List<ExamAttempt> findAllByExamIdAndStudentUserIdIsNullAndStudentNameIgnoreCase(Long examId, String studentName);
}
