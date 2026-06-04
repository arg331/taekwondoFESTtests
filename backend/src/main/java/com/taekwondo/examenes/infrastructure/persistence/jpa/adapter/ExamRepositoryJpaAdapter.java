package com.taekwondo.examenes.infrastructure.persistence.jpa.adapter;

import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.ExamStatus;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.model.Visibility;
import com.taekwondo.examenes.domain.port.ExamRepository;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ExamJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.TagJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.repository.ExamSpringDataRepository;
import com.taekwondo.examenes.infrastructure.persistence.mapper.ExamMapper;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adaptador de salida: implementa ExamRepository usando Spring Data JPA.
 *
 * Igual que QuestionRepositoryJpaAdapter, hidrata los tags con
 * EntityManager.getReference para evitar consultas extra y para que
 * JPA no intente re-insertar tags que ya existen.
 *
 * findAllPublic filtra por (Visibility=PUBLIC, Status=PUBLISHED). El
 * filtro doble es defensa en profundidad: aunque la entidad ya garantiza
 * que un DRAFT no pueda tener Visibility=PUBLIC, el repo no se fía y
 * vuelve a comprobar.
 */
@Repository
public class ExamRepositoryJpaAdapter implements ExamRepository {

    private final ExamSpringDataRepository springDataRepository;
    private final EntityManager entityManager;

    public ExamRepositoryJpaAdapter(ExamSpringDataRepository springDataRepository,
                                     EntityManager entityManager) {
        this.springDataRepository = springDataRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Exam save(Exam exam) {
        Set<TagJpaEntity> managedTags = resolveManagedTags(exam.getGenerationTags());
        ExamJpaEntity entity = ExamMapper.toJpa(exam, managedTags);
        ExamJpaEntity persisted = springDataRepository.save(entity);
        return ExamMapper.toDomain(persisted);
    }

    @Override
    public Optional<Exam> findById(Long id) {
        return springDataRepository.findById(id).map(ExamMapper::toDomain);
    }

    @Override
    public Optional<Exam> findByCode(String code) {
        return springDataRepository.findByCode(code).map(ExamMapper::toDomain);
    }

    @Override
    public List<Exam> findAllByOwnerId(Long ownerId) {
        return springDataRepository.findAllByOwnerId(ownerId).stream()
                .map(ExamMapper::toDomain)
                .toList();
    }

    @Override
    public List<Exam> findAllPublic() {
        return springDataRepository
                .findAllByVisibilityAndStatus(Visibility.PUBLIC, ExamStatus.PUBLISHED)
                .stream()
                .map(ExamMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public boolean existsByCode(String code) {
        return springDataRepository.existsByCode(code);
    }

    private Set<TagJpaEntity> resolveManagedTags(Set<Tag> domainTags) {
        Set<TagJpaEntity> managedTags = new HashSet<>();
        for (Tag tag : domainTags) {
            managedTags.add(entityManager.getReference(TagJpaEntity.class, tag.getId()));
        }
        return managedTags;
    }
}
