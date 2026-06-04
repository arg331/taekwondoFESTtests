package com.taekwondo.examenes.infrastructure.persistence.jpa.adapter;

import com.taekwondo.examenes.domain.model.Result;
import com.taekwondo.examenes.domain.port.ResultRepository;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ResultJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.repository.ResultSpringDataRepository;
import com.taekwondo.examenes.infrastructure.persistence.mapper.ResultMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida: implementa ResultRepository usando Spring Data JPA.
 *
 * Más simple que los adapters anteriores (Question, Exam): Result no
 * tiene relaciones JPA externas que hidratar. Solo guarda IDs (examId,
 * studentUserId, questionId en cada Answer embebido), así que basta
 * con mapear ida y vuelta.
 *
 * No expone deleteById: Result es histórico inmutable.
 */
@Repository
public class ResultRepositoryJpaAdapter implements ResultRepository {

    private final ResultSpringDataRepository springDataRepository;

    public ResultRepositoryJpaAdapter(ResultSpringDataRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Result save(Result result) {
        ResultJpaEntity entity = ResultMapper.toJpa(result);
        ResultJpaEntity persisted = springDataRepository.save(entity);
        return ResultMapper.toDomain(persisted);
    }

    @Override
    public Optional<Result> findById(Long id) {
        return springDataRepository.findById(id).map(ResultMapper::toDomain);
    }

    @Override
    public List<Result> findAllByExamId(Long examId) {
        return springDataRepository.findAllByExamId(examId).stream()
                .map(ResultMapper::toDomain)
                .toList();
    }

    @Override
    public List<Result> findAllByStudentUserId(Long studentUserId) {
        return springDataRepository.findAllByStudentUserId(studentUserId).stream()
                .map(ResultMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByExamIdAndStudentName(Long examId, String studentName) {
        return springDataRepository.existsByExamIdAndStudentName(examId, studentName);
    }

    @Override
    public long countByExamId(Long examId) {
        return springDataRepository.countByExamId(examId);
    }
}
