package com.taekwondo.examenes.repository;

import com.taekwondo.examenes.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    /**
     * Encuentra todos los tags de un profesor específico
     */
    List<Tag> findByOwnerId(Long ownerId);
    
    /**
     * Busca un tag por nombre y propietario
     * Útil para detectar conflictos al importar
     */
    Optional<Tag> findByNameAndOwnerId(String name, Long ownerId);
    
    /**
     * Verifica si un tag con ese nombre ya existe para el profesor
     */
    boolean existsByNameAndOwnerId(String name, Long ownerId);
}
