package com.taekwondo.examenes.infrastructure.persistence.jpa.adapter;

import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.model.QuestionCriteria;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.port.QuestionRepository;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.QuestionJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.TagJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.repository.QuestionSpringDataRepository;
import com.taekwondo.examenes.infrastructure.persistence.jpa.specification.QuestionSpecs;
import com.taekwondo.examenes.infrastructure.persistence.mapper.QuestionMapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Adaptador de salida: implementa QuestionRepository usando Spring Data JPA.
 *
 * Aquí se resuelven dos cosas no triviales:
 *
 *  1. Hidratación de tags: cuando se guarda una Question, sus tags llegan
 *     como objetos del dominio (Tag). Para que JPA no intente "re-insertar"
 *     tags que ya existen, usamos EntityManager.getReference, que devuelve
 *     un proxy con solo el ID.
 *
 *  2. Búsqueda con criterios: el puerto recibe un QuestionCriteria. El
 *     adaptador lo traduce en una combinación de Specifications.
 *     Spring Data exige que las Specifications no sean null al combinarlas
 *     con allOf, así que filtramos los nulls (filtros no aplicables) antes.
 */
@Repository
public class QuestionRepositoryJpaAdapter implements QuestionRepository {

    private final QuestionSpringDataRepository springDataRepository;
    private final EntityManager entityManager;

    public QuestionRepositoryJpaAdapter(QuestionSpringDataRepository springDataRepository,
                                         EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Question save(Question question) {
        Set<TagJpaEntity> managedTags = resolveManagedTags(question.getTags());

        QuestionJpaEntity entity = QuestionMapper.toJpa(question, managedTags);
        QuestionJpaEntity persisted = springDataRepository.save(entity);
        return QuestionMapper.toDomain(persisted);
    }

    @Override
    public Optional<Question> findById(Long id) {
        return springDataRepository.findById(id).map(QuestionMapper::toDomain);
    }

    @Override
    public List<Question> findAllByOwnerId(Long ownerId) {
        return springDataRepository.findAllByOwnerId(ownerId).stream()
                .map(QuestionMapper::toDomain)
                .toList();
    }

    @Override
    public long countByOwnerId(Long ownerId) {
        return springDataRepository.countByOwnerId(ownerId);
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public List<Question> findByCriteria(QuestionCriteria criteria) {
        // Construimos la lista de specs, descartando las que devuelvan null
        // (filtros no aplicables). Specification.allOf NO acepta nulls
        // en Spring Data 3.x+, por eso filtramos antes.
        List<Specification<QuestionJpaEntity>> specs = Stream.of(
                        QuestionSpecs.ownedBy(criteria.getOwnerId()),
                        QuestionSpecs.hasAnyOfTags(criteria.getAnyOfTags()),
                        QuestionSpecs.textContains(criteria.getTextContains())
                )
                .filter(Objects::nonNull)
                .toList();

        Specification<QuestionJpaEntity> spec = Specification.allOf(specs);

        return springDataRepository.findAll(spec).stream()
                .map(QuestionMapper::toDomain)
                .toList();
    }

    private Set<TagJpaEntity> resolveManagedTags(Set<Tag> domainTags) {
        Set<TagJpaEntity> managedTags = new HashSet<>();
        for (Tag tag : domainTags) {
            managedTags.add(entityManager.getReference(TagJpaEntity.class, tag.getId()));
        }
        return managedTags;
    }
}
