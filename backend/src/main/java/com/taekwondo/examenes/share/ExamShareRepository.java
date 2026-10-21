package com.taekwondo.examenes.share;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamShareRepository extends JpaRepository<ExamShare, Long> {

    Optional<ExamShare> findByExamIdAndTeacherId(Long examId, Long teacherId);

    boolean existsByExamIdAndTeacherIdAndStatus(Long examId, Long teacherId, ShareStatus status);

    List<ExamShare> findAllByExamIdOrderByCreatedAt(Long examId);

    /** Invitaciones pendientes de un profesor, o exámenes que le han compartido (ACCEPTED). */
    List<ExamShare> findAllByTeacherIdAndStatusOrderByCreatedAtDesc(Long teacherId, ShareStatus status);
}
