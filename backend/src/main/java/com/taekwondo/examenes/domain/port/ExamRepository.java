package com.taekwondo.examenes.domain.port;

import com.taekwondo.examenes.domain.model.Exam;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: persistencia y consulta de exámenes.
 *
 * Operaciones expresadas como necesidades de la aplicación, sin asumir
 * tecnología de persistencia.
 *
 * Filtros simples (por estado, por visibilidad) NO están aquí: los aplican
 * los casos de uso sobre el resultado de findAllByOwnerId. Si en el futuro
 * el volumen lo justifica, se añadirán métodos específicos.
 */
public interface ExamRepository {

    Exam save(Exam exam);

    Optional<Exam> findById(Long id);

    /**
     * Busca un examen por su código de acceso (el del QR).
     * Solo devuelve resultados si el examen está publicado y tiene código.
     */
    Optional<Exam> findByCode(String code);

    /**
     * Lista todos los exámenes (drafts y publicados) de un profesor.
     */
    List<Exam> findAllByOwnerId(Long ownerId);

    /**
     * Lista todos los exámenes públicos del sistema.
     * Sirve para que un profesor descubra exámenes de otros profesores.
     */
    List<Exam> findAllPublic();

    void deleteById(Long id);

    /**
     * Indica si ya existe un examen con ese código.
     * Útil para garantizar unicidad al generar el código de un nuevo examen.
     */
    boolean existsByCode(String code);
}