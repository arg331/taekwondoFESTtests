package com.taekwondo.examenes.result;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findAllByExamIdOrderByCompletedAtDesc(Long examId);

    List<Result> findAllByStudentUserIdOrderByCompletedAtDesc(Long studentUserId);

    boolean existsByExamIdAndStudentUserId(Long examId, Long studentUserId);

    boolean existsByExamIdAndStudentNameIgnoreCase(Long examId, String studentName);
}
