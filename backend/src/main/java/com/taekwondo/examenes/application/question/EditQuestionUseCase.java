package com.taekwondo.examenes.application.question;

import com.taekwondo.examenes.application.question.dto.EditQuestionInput;
import com.taekwondo.examenes.application.question.dto.QuestionView;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.model.Tag;
import com.taekwondo.examenes.domain.port.QuestionRepository;
import com.taekwondo.examenes.domain.port.TagRepository;

import java.util.HashSet;
import java.util.Set;

/**
 * Caso de uso: editar el contenido de una pregunta existente.
 *
 * Actualiza texto, opciones, respuesta correcta, explicación, dificultad
 * y conjunto de tags. El ownerId no se cambia (una pregunta no cambia de
 * dueño).
 *
 * Pasos:
 *  1. Cargar la pregunta y verificar pertenencia.
 *  2. Resolver los nuevos tags (validando pertenencia al mismo profesor).
 *  3. Aplicar los cambios a través de las operaciones de negocio de la entidad.
 *  4. Persistir.
 */
public class EditQuestionUseCase {

    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;

    public EditQuestionUseCase(QuestionRepository questionRepository,
                                TagRepository tagRepository) {
        this.questionRepository = questionRepository;
        this.tagRepository = tagRepository;
    }

    public QuestionView execute(EditQuestionInput input) {
        // 1. Cargar la pregunta
        Question question = questionRepository.findById(input.questionId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una pregunta con id " + input.questionId()));

        // Pertenencia: no revelar preguntas ajenas
        if (!question.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe una pregunta con id " + input.questionId());
        }

        // 2. Resolver los nuevos tags
        Set<Tag> newTags = resolveTagsForOwner(input.tagIds(), input.requesterOwnerId());

        // 3. Aplicar cambios (la entidad valida cada uno)
        question.editText(input.text());
        question.editOptions(input.options(), input.correctAnswer());
        question.editExplanation(input.explanation());
        question.changeDifficulty(input.difficulty());

        // Sincronizar tags: quitar los que ya no están, añadir los nuevos.
        // Trabajamos con copias para no iterar y modificar a la vez.
        Set<Tag> currentTags = new HashSet<>(question.getTags());
        for (Tag tag : currentTags) {
            if (!newTags.contains(tag)) {
                question.removeTag(tag);
            }
        }
        for (Tag tag : newTags) {
            if (!currentTags.contains(tag)) {
                question.addTag(tag);
            }
        }

        // 4. Persistir y devolver
        Question saved = questionRepository.save(question);
        return QuestionView.from(saved);
    }

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
                throw new BusinessRuleViolationException(
                        "El tag con id " + tagId + " no existe");
            }

            resolved.add(tag);
        }
        return resolved;
    }
}