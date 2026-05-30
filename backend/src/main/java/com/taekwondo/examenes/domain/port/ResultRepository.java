package com.taekwondo.examenes.domain.port;

import com.taekwondo.examenes.domain.model.Result;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia y consulta de resultados de exámenes.
 *
 * Operaciones expresadas como necesidades de la aplicación, sin asumir
 * tecnología de persistencia.
 *
 * NO incluimos métodos de actualización (Result es inmutable: se crea
 * cuando el estudiante termina y no se modifica). save() solo se usa
 * para persistir un Result nuevo.
 *
 * NO incluimos delete: los resultados son log histórico de la federación.
 * Si en el futuro se necesita anonimizarlos o archivarlos, eso será un
 * caso de uso explícito y un método específico en el puerto, nunca un
 * delete genérico expuesto.
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
     * Indica si un estudiante (identificado por su nombre) ya tiene un
     * resultado para un examen dado.
     *
     * Sirve para prevenir intentos duplicados: si el mismo nombre intenta
     * acceder dos veces al mismo examen, se rechaza.
     */
    boolean existsByExamIdAndStudentName(Long examId, String studentName);

    /**
     * Cuenta cuántos resultados existen para un examen.
     */
    long countByExamId(Long examId);
}
