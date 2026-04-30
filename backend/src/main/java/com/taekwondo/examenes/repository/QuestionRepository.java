package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para operaciones de base de datos con Question
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Buscar preguntas por categoría
     */
    List<Question> findByCategory(Question.Category category);

    /**
     * Buscar preguntas por dificultad
     */
    List<Question> findByDifficulty(Question.Difficulty difficulty);

    /**
     * Buscar preguntas por categoría y dificultad
     */
    List<Question> findByCategoryAndDifficulty(
        Question.Category category, 
        Question.Difficulty difficulty
    );

    /**
     * Buscar preguntas que contengan un texto específico
     */
    List<Question> findByTextContainingIgnoreCase(String text);

    /**
     * Contar preguntas por categoría
     */
    long countByCategory(Question.Category category);

    /**
     * Obtener preguntas aleatorias (usamos consulta nativa)
     * Nota: En H2 usamos RAND(), en PostgreSQL sería RANDOM()
     */
    @Query(value = "SELECT * FROM questions ORDER BY RAND() LIMIT ?1", nativeQuery = true)
    List<Question> findRandomQuestions(int limit);

    /**
     * Obtener preguntas aleatorias por categoría
     */
    @Query(value = "SELECT * FROM questions WHERE category = ?1 ORDER BY RAND() LIMIT ?2", nativeQuery = true)
    List<Question> findRandomQuestionsByCategory(String category, int limit);
}
