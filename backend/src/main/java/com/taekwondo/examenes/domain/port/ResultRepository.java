package com.taekwondo.examenes.domain.port;

import com.taekwondo.examenes.domain.model.Result;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia y consulta de resultados de exámenes.
 *
 * Result es inmutable: save() solo se usa al crear, nunca para actualizar.
 *
 * NO incluimos delete: los resultados son log histórico de la federación.
 */
public interface ResultRepository {

    /**
     * Persiste un resultado nuevo. No actualiza resultados existentes.
     */
    Result save(Result result);

    Optional<Result> findById(Long id);

    /**
     * Lista todos los resultados de un examen, en cualquier orden.
     */
    List<Result> findAllByExamId(Long examId);

    /**
     * Lista todos los resultados de un estudiante registrado, en cualquier orden.
     *
     * Sólo cubre resultados con studentUserId == studentUserId.
     * Los resultados anónimos NO aparecen aquí porque no tienen userId
     * asociado (un anónimo no tiene cuenta y no puede consultar "su" historial).
     */
    List<Result> findAllByStudentUserId(Long studentUserId);

    /**
     * Indica si un estudiante (identificado por su nombre) ya tiene un
     * resultado para un examen dado.
     */
    boolean existsByExamIdAndStudentName(Long examId, String studentName);

    /**
     * Cuenta cuántos resultados existen para un examen.
     */
    long countByExamId(Long examId);
}
