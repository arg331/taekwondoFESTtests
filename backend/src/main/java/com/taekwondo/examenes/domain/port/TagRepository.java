package com.taekwondo.examenes.domain.port;

import com.taekwondo.examenes.domain.model.Tag;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida: contrato para persistir y consultar tags.
 *
 * <p>El dominio define lo que necesita. La infraestructura lo implementa
 * con la tecnología que sea (JPA, MongoDB, en memoria, ...). El dominio
 * no conoce a la infraestructura.</p>
 *
 * <p>Cada método se justifica por un caso de uso real. No añadir métodos
 * "por si acaso": ISP (Interface Segregation Principle).</p>
 */
public interface TagRepository {

    /**
     * Persiste un tag nuevo o actualiza uno existente.
     *
     * @return el tag persistido (con ID asignado si era nuevo)
     */
    Tag save(Tag tag);

    /**
     * Busca un tag por su identificador.
     */
    Optional<Tag> findById(Long id);

    /**
     * Lista todos los tags de un profesor.
     */
    List<Tag> findAllByOwnerId(Long ownerId);

    /**
     * Indica si ya existe un tag con ese nombre para ese profesor.
     * Usado para detectar duplicados al crear o renombrar.
     */
    boolean existsByNameAndOwnerId(String name, Long ownerId);
}
