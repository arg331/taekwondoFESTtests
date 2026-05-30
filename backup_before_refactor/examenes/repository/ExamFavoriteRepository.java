package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.ExamFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamFavoriteRepository extends JpaRepository<ExamFavorite, Long> {
    
    /**
     * Encuentra todos los favoritos de un profesor
     */
    List<ExamFavorite> findByProfessorId(Long professorId);
    
    /**
     * Obtiene los IDs de exámenes favoritos de un profesor
     */
    @Query("SELECT ef.examId FROM ExamFavorite ef WHERE ef.professorId = :professorId")
    List<Long> findExamIdsByProfessorId(@Param("professorId") Long professorId);
    
    /**
     * Busca si ya existe un favorito específico
     */
    Optional<ExamFavorite> findByProfessorIdAndExamId(Long professorId, Long examId);
    
    /**
     * Verifica si un profesor ya tiene favoriteado un examen
     */
    boolean existsByProfessorIdAndExamId(Long professorId, Long examId);
    
    /**
     * Elimina un favorito específico
     */
    void deleteByProfessorIdAndExamId(Long professorId, Long examId);
}
