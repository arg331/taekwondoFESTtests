package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.ExamTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamTemplateRepository extends JpaRepository<ExamTemplate, Long> {
    
    /**
     * Encuentra todas las plantillas de un profesor
     */
    List<ExamTemplate> findByOwnerId(Long ownerId);
    
    /**
     * Busca plantillas por nombre (búsqueda parcial)
     */
    List<ExamTemplate> findByOwnerIdAndNameContainingIgnoreCase(Long ownerId, String name);
}
