package com.taekwondo.examenes.domain.port;

import com.taekwondo.examenes.domain.model.ExamFavorite;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia y consulta de favoritos.
 *
 * La operación principal es persistir/consultar la relación. NO incluimos
 * update: un favorito existe o no existe, no se modifica.
 *
 * deleteByProfessorIdAndExamId expresa la intención del caso de uso
 * "desfavoritear" sin obligar a consultar primero para obtener el ID
 * de la entidad.
 */
public interface ExamFavoriteRepository {

    ExamFavorite save(ExamFavorite favorite);

    Optional<ExamFavorite> findByProfessorIdAndExamId(Long professorId, Long examId);

    boolean existsByProfessorIdAndExamId(Long professorId, Long examId);

    /**
     * Lista todos los favoritos de un profesor.
     * Devolver los favoritos directamente; el caso de uso resuelve
     * los exámenes asociados si los necesita.
     */
    List<ExamFavorite> findAllByProfessorId(Long professorId);

    /**
     * Elimina un favorito específico identificado por la pareja profesor-examen.
     * Si no existe, no hace nada (no falla).
     */
    void deleteByProfessorIdAndExamId(Long professorId, Long examId);
}
