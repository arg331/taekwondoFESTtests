package com.taekwondo.examenes.application.question;

import com.taekwondo.examenes.application.question.dto.CreateQuestionInput;
import com.taekwondo.examenes.application.question.dto.QuestionView;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.port.QuestionRepository;
import com.taekwondo.examenes.domain.port.TagRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Caso de uso: crear una pregunta nueva.
 *
 * Pasos:
 *  1. Resolver los tags por sus IDs, verificando que existen y pertenecen
 *     al profesor.
 *  2. Construir la entidad Question (que valida formato de texto, opciones, etc.).
 *  3. Asociar los tags a la pregunta.
 *  4. Persistir y devolver vista.
 *
 * Nota: este caso de uso depende de DOS puertos (QuestionRepository y
 * TagRepository). Es normal: necesita coordinar dos agregados (preguntas y tags)
 * y por eso vive en la capa de aplicación, no en el dominio.
 */
public class CreateQuestionUseCase {

    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;

    public CreateQuestionUseCase(QuestionRepository questionRepository,
                                  TagRepository tagRepository) {
        this.questionRepository = questionRepository;
        this.tagRepository = tagRepository;
    }

    public QuestionView execute(CreateQuestionInput input) {
        // 1. Resolver tags. La validación de pertenencia es CRÍTICA:
        //    sin ella, un profesor podría asignar tags de otro profesor
        //    a sus preguntas.
        Set<Tag> tags = resolveTagsForOwner(input.tagIds(), input.ownerId());

        // 2. Construir la entidad (las validaciones de formato las hace ella)
        Question question = Question.createNew(
                input.text(),
                input.options(),
                input.correctAnswer(),
                input.explanation(),
                input.difficulty(),
                input.ownerId()
        );

        // 3. Asociar tags (la entidad valida que sean del mismo owner)
        for (Tag tag : tags) {
            question.addTag(tag);
        }

        // 4. Persistir y devolver
        Question persisted = questionRepository.save(question);
        return QuestionView.from(persisted);
    }

    /**
     * Carga los tags por sus IDs y verifica que todos existan y pertenezcan
     * al profesor. Si algún tag no existe o no le pertenece, falla.
     */
    private Set<Tag> resolveTagsForOwner(Set<Long> tagIds, Long ownerId) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Tag> resolved = new HashSet<>();
        for (Long tagId : tagIds) {
            Tag tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "El tag con id " + tagId + " no existe"));

            if (!tag.getOwnerId().equals(ownerId)) {
                // Mismo principio que con preguntas: no revelar existencia
                // de recursos que no pertenecen al solicitante.
                throw new BusinessRuleViolationException(
                        "El tag con id " + tagId + " no existe");
            }

            resolved.add(tag);
        }
        return resolved;
    }
}