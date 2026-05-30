package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    
    /**
     * Encuentra examen por código (para estudiantes)
     */
    Optional<Exam> findByCode(String code);
    
    /**
     * Verifica si existe un examen con ese código
     */
    boolean existsByCode(String code);
    
    /**
     * Encuentra todos los exámenes de un profesor
     */
    List<Exam> findByOwnerId(Long ownerId);
    
    /**
     * Encuentra exámenes públicos (excluyendo los del profesor dado)
     */
    List<Exam> findByIsPublicTrueAndOwnerIdNot(Long ownerId);
    
    /**
     * Encuentra exámenes activos y no expirados
     */
    @Query("SELECT e FROM Exam e WHERE e.active = true AND e.isDraft = false AND e.expiresAt > :now")
    List<Exam> findActiveAndNotExpired(@Param("now") LocalDateTime now);
    
    /**
     * Encuentra el examen activo más reciente de un profesor
     */
    @Query("SELECT e FROM Exam e WHERE e.ownerId = :ownerId AND e.active = true AND e.isDraft = false ORDER BY e.createdAt DESC")
    Optional<Exam> findLatestActiveExamByOwnerId(@Param("ownerId") Long ownerId);
    
    /**
     * Encuentra todos los exámenes activos de un profesor
     */
    List<Exam> findByOwnerIdAndActiveTrue(Long ownerId);
    
    /**
     * Encuentra exámenes draft de un profesor
     */
    List<Exam> findByOwnerIdAndIsDraftTrue(Long ownerId);
    
    /**
     * Encuentra exámenes publicados de un profesor
     */
    List<Exam> findByOwnerIdAndIsDraftFalse(Long ownerId);
}
