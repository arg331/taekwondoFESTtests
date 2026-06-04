package com.taekwondo.examenes.infrastructure.persistence.jpa.adapter;

import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.port.TagRepository;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.TagJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.repository.TagSpringDataRepository;
import com.taekwondo.examenes.infrastructure.persistence.mapper.TagMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa el puerto TagRepository del dominio
 * usando Spring Data JPA.
 *
 * Aquí "entra" Spring al sistema de persistencia. Esta clase tiene
 * la anotación @Repository de Spring, mientras que el puerto
 * (TagRepository) sigue siendo Java puro.
 *
 * Es el corazón del patrón hexagonal: el dominio define el contrato,
 * la infraestructura lo cumple.
 */
@Repository
public class TagRepositoryJpaAdapter implements TagRepository {

    private final TagSpringDataRepository springDataRepository;

    public TagRepositoryJpaAdapter(TagSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Tag save(Tag tag) {
        TagJpaEntity entity = TagMapper.toJpa(tag);
        TagJpaEntity persisted = springDataRepository.save(entity);
        return TagMapper.toDomain(persisted);
    }

    @Override
    public Optional<Tag> findById(Long id) {
        return springDataRepository.findById(id).map(TagMapper::toDomain);
    }

    @Override
    public List<Tag> findAllByOwnerId(Long ownerId) {
        return springDataRepository.findAllByOwnerId(ownerId).stream()
                .map(TagMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByNameAndOwnerId(String name, Long ownerId) {
        return springDataRepository.existsByNameAndOwnerId(name, ownerId);
    }
}
