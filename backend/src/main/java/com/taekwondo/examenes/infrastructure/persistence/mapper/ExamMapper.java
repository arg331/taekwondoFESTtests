package com.taekwondo.examenes.infrastructure.persistence.mapper;

import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.ExamConfig;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ExamConfigJpaEmbeddable;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.ExamJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.TagJpaEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Conversión bidireccional entre Exam (dominio) y ExamJpaEntity (JPA).
 *
 * Como Question, el método toJpa recibe Set<TagJpaEntity> ya gestionados
 * por el contexto de persistencia (vía EntityManager.getReference desde
 * el adapter), para evitar consultas extra y problemas de duplicación
 * en la tabla de relación.
 */
public final class ExamMapper {

    private ExamMapper() {}

    // ──────────────────────────────────────────────
    // JPA → dominio
    // ──────────────────────────────────────────────

    public static Exam toDomain(ExamJpaEntity entity) {
        Set<Tag> tags = entity.getGenerationTags().stream()
                .map(TagMapper::toDomain)
                .collect(Collectors.toSet());

        ExamConfig config = configToDomain(entity.getConfig());

        return Exam.reconstitute(
                entity.getId(),
                entity.getTitle(),
                entity.getOwnerId(),
                entity.getStatus(),
                entity.getVisibility(),
                entity.getAccessMode(),
                config,
                new ArrayList<>(entity.getQuestionIds()),
                tags,
                entity.getCode(),
                entity.getCreatedAt(),
                entity.getExpiresAt()
        );
    }

    private static ExamConfig configToDomain(ExamConfigJpaEmbeddable embedded) {
        return ExamConfig.of(
                embedded.getNumberOfQuestions(),
                embedded.getTimeLimitMinutes(),
                embedded.isShowScore(),
                embedded.isRandomizeOptions(),
                embedded.isRandomizeQuestionOrder()
        );
    }

    // ──────────────────────────────────────────────
    // Dominio → JPA
    // ──────────────────────────────────────────────

    public static ExamJpaEntity toJpa(Exam exam, Set<TagJpaEntity> managedTags) {
        ExamJpaEntity entity = new ExamJpaEntity();
        entity.setId(exam.getId());
        entity.setTitle(exam.getTitle());
        entity.setOwnerId(exam.getOwnerId());
        entity.setStatus(exam.getStatus());
        entity.setVisibility(exam.getVisibility());
        entity.setAccessMode(exam.getAccessMode());
        entity.setConfig(configToJpa(exam.getConfig()));
        entity.setQuestionIds(new ArrayList<>(exam.getQuestionIds()));
        entity.setGenerationTags(new HashSet<>(managedTags));
        entity.setCode(exam.getCode());
        entity.setCreatedAt(exam.getCreatedAt());
        entity.setExpiresAt(exam.getExpiresAt());
        return entity;
    }

    private static ExamConfigJpaEmbeddable configToJpa(ExamConfig config) {
        return new ExamConfigJpaEmbeddable(
                config.getNumberOfQuestions(),
                config.getTimeLimitMinutes(),
                config.isShowScore(),
                config.isRandomizeOptions(),
                config.isRandomizeQuestionOrder()
        );
    }
}
