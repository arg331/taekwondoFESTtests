package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.Question;
import com.taekwondo.examenes.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    /**
     * Encuentra todas las preguntas de un profesor
     */
    List<Question> findByOwnerId(Long ownerId);
    
    /**
     * Encuentra preguntas de un profesor que contienen un tag específico
     */
    @Query("SELECT q FROM Question q JOIN q.tags t WHERE q.ownerId = :ownerId AND t.id = :tagId")
    List<Question> findByOwnerIdAndTagId(@Param("ownerId") Long ownerId, @Param("tagId") Long tagId);
    
    /**
     * Encuentra preguntas de un profesor que contienen cualquiera de los tags dados
     */
    @Query("SELECT DISTINCT q FROM Question q JOIN q.tags t WHERE q.ownerId = :ownerId AND t IN :tags")
    List<Question> findByOwnerIdAndTagsIn(@Param("ownerId") Long ownerId, @Param("tags") List<Tag> tags);
    
    /**
     * Busca preguntas de un profesor por texto (búsqueda parcial)
     */
    @Query("SELECT q FROM Question q WHERE q.ownerId = :ownerId AND LOWER(q.text) LIKE LOWER(CONCAT('%', :searchText, '%'))")
    List<Question> searchByOwnerIdAndText(@Param("ownerId") Long ownerId, @Param("searchText") String searchText);
    
    /**
     * Selecciona N preguntas aleatorias de un profesor que tienen alguno de los tags permitidos
     */
    @Query(value = "SELECT DISTINCT q.* FROM questions q " +
                   "JOIN question_tags qt ON q.id = qt.question_id " +
                   "WHERE q.owner_id = :ownerId AND qt.tag_id IN :tagIds " +
                   "ORDER BY RAND() LIMIT :limit", 
           nativeQuery = true)
    List<Question> findRandomByOwnerIdAndTags(
        @Param("ownerId") Long ownerId,
        @Param("tagIds") List<Long> tagIds,
        @Param("limit") int limit
    );
    
    /**
     * Cuenta preguntas de un profesor
     */
    long countByOwnerId(Long ownerId);
    
    /**
     * Cuenta preguntas de un profesor con un tag específico
     */
    @Query("SELECT COUNT(DISTINCT q) FROM Question q JOIN q.tags t WHERE q.ownerId = :ownerId AND t.id = :tagId")
    long countByOwnerIdAndTagId(@Param("ownerId") Long ownerId, @Param("tagId") Long tagId);
}
