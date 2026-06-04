package com.taekwondo.examenes.infrastructure.persistence.mapper;

import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.QuestionJpaEntity;
import com.taekwondo.examenes.infrastructure.persistence.jpa.entity.TagJpaEntity;

import java.util.HashSet;
import java.util.Set;

/**
 * Conversor entre Question (dominio) y QuestionJpaEntity (persistencia).
 *
 * Diferencia clave con TagMapper: este mapper trabaja con tags como
 * relaciones. Por eso toJpa() recibe los tags YA RESUELTOS por el adaptador
 * (con EntityManager.getReference), en vez de crearlos él mismo. Si el
 * mapper creara nuevos TagJpaEntity, Hibernate los trataría como entidades
 * nuevas e intentaría insertarlas, duplicando datos.
 */
public final class QuestionMapper {

    private QuestionMapper() {}

    /**
     * Convierte Question (dominio) a QuestionJpaEntity (JPA).
     *
     * @param question     la pregunta del dominio
     * @param managedTags  los TagJpaEntity ya resueltos (referencias gestionadas
     *                     por Hibernate). El adaptador del repositorio se
     *                     encarga de proporcionarlos.
     */
    public static QuestionJpaEntity toJpa(Question question, Set<TagJpaEntity> managedTags) {
        return new QuestionJpaEntity(
                question.getId(),
                question.getText(),
                question.getOptions(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getDifficulty(),
                question.getOwnerId(),
                managedTags,
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    /**
     * Convierte QuestionJpaEntity (JPA) a Question (dominio).
     *
     * Los tags JPA se convierten a Tags del dominio usando TagMapper.
     */
    public static Question toDomain(QuestionJpaEntity entity) {
        Set<Tag> domainTags = new HashSet<>();
        for (TagJpaEntity tagEntity : entity.getTags()) {
            domainTags.add(TagMapper.toDomain(tagEntity));
        }

        return Question.reconstitute(
                entity.getId(),
                entity.getText(),
                entity.getOptions(),
                entity.getCorrectAnswer(),
                entity.getExplanation(),
                entity.getDifficulty(),
                entity.getOwnerId(),
                domainTags,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
