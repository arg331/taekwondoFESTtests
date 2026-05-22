package com.taekwondo.examenes.domain.port;

import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.model.QuestionCriteria;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia y consulta de preguntas.
 *
 * Operaciones simples como métodos directos. Para búsquedas con múltiples
 * filtros se usa QuestionCriteria (patrón Specification simplificado), lo
 * que permite añadir filtros nuevos sin modificar la interfaz (OCP).
 */
public interface QuestionRepository {

    Question save(Question question);

    Optional<Question> findById(Long id);

    List<Question> findAllByOwnerId(Long ownerId);

    long countByOwnerId(Long ownerId);

    void deleteById(Long id);

    /**
     * Búsqueda flexible mediante criterios.
     *
     * Devuelve TODAS las preguntas que cumplen los criterios. Si se necesita
     * paginación o selección aleatoria, eso se aplica en la capa de aplicación
     * (caso de uso) o se añade un método específico cuando haga falta.
     */
    List<Question> findByCriteria(QuestionCriteria criteria);
}