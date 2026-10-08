package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.entity.ExamFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamFavoriteRepository extends JpaRepository<ExamFavorite, Long> {

    boolean existsByProfessorIdAndExamId(Long professorId, Long examId);

    List<ExamFavorite> findAllByProfessorIdOrderByCreatedAtDesc(Long professorId);

    long deleteByProfessorIdAndExamId(Long professorId, Long examId);
}
