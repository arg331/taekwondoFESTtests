package com.taekwondo.examenes.infrastructure.persistence.jpa.adapter;

import com.taekwondo.examenes.domain.model.ExamFavorite;
import com.taekwondo.examenes.domain.port.ExamFavoriteRepository;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ExamFavoriteJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.repository.ExamFavoriteSpringDataRepository;
import com.taekwondo.examenes.infrastructure.persistence.mapper.ExamFavoriteMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa ExamFavoriteRepository usando Spring Data JPA.
 *
 * deleteByProfessorIdAndExamId requiere @Transactional explícito.
 * Spring Data NO lo aplica automáticamente a métodos derivados de
 * tipo void que ejecutan DELETE. Sin la anotación, falla en runtime
 * con "No EntityManager with actual transaction available".
 *
 * Los métodos de lectura (find, exists) no necesitan @Transactional:
 * Spring los envuelve por defecto en transacciones de solo lectura.
 */
@Repository
@Transactional
public class ExamFavoriteRepositoryJpaAdapter implements ExamFavoriteRepository {

    private final ExamFavoriteSpringDataRepository springDataRepository;

    public ExamFavoriteRepositoryJpaAdapter(ExamFavoriteSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public ExamFavorite save(ExamFavorite favorite) {
        ExamFavoriteJpaEntity entity = ExamFavoriteMapper.toJpa(favorite);
        ExamFavoriteJpaEntity persisted = springDataRepository.save(entity);
        return ExamFavoriteMapper.toDomain(persisted);
    }

    @Override
    public Optional<ExamFavorite> findByProfessorIdAndExamId(Long professorId, Long examId) {
        return springDataRepository
                .findByProfessorIdAndExamId(professorId, examId)
                .map(ExamFavoriteMapper::toDomain);
    }

    @Override
    public boolean existsByProfessorIdAndExamId(Long professorId, Long examId) {
        return springDataRepository.existsByProfessorIdAndExamId(professorId, examId);
    }

    @Override
    public List<ExamFavorite> findAllByProfessorId(Long professorId) {
        return springDataRepository.findAllByProfessorId(professorId).stream()
                .map(ExamFavoriteMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteByProfessorIdAndExamId(Long professorId, Long examId) {
        springDataRepository.deleteByProfessorIdAndExamId(professorId, examId);
    }
}
