package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.exam.dto.PreGenerateExamDraftInput;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.domain.model.*;
import com.taekwondo.examenes.domain.port.ExamRepository;
import com.taekwondo.examenes.domain.port.QuestionRepository;
import com.taekwondo.examenes.domain.port.TagRepository;

import java.util.*;

/**
 * Caso de uso: pre-generar un draft de examen con preguntas aleatorias
 * filtradas por tags obligatorios (modo asistido).
 *
 * Pasos:
 *  1. Resolver tags pedidos (verificando pertenencia al profesor).
 *  2. Construir el criterio de búsqueda.
 *  3. Obtener todas las preguntas que cumplen el criterio.
 *  4. Comprobar que hay suficientes preguntas para el examen.
 *  5. Aleatorizar y seleccionar las N primeras.
 *  6. Crear el draft con esas preguntas y los tags como "generationTags".
 *  7. Persistir y devolver.
 *
 * La aleatoriedad vive aquí (no en el repositorio): es decisión de la
 * aplicación cómo seleccionar, no responsabilidad de la persistencia.
 */
public class PreGenerateExamDraftUseCase {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final TagRepository tagRepository;

    public PreGenerateExamDraftUseCase(ExamRepository examRepository,
                                        QuestionRepository questionRepository,
                                        TagRepository tagRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.tagRepository = tagRepository;
    }

    public ExamView execute(PreGenerateExamDraftInput input) {
        // 1. Resolver tags pedidos (todos deben existir y pertenecer al profesor)
        Set<Tag> requiredTags = resolveTagsForOwner(
                input.requiredAnyOfTagIds(),
                input.ownerId()
        );

        // 2. Construir criterio (si no hay tags, busca todas las del profesor)
        QuestionCriteria criteria = QuestionCriteria.forOwner(input.ownerId())
                .withAnyOfTags(requiredTags)
                .build();

        // 3. Buscar las preguntas candidatas
        List<Question> candidates = new ArrayList<>(
                questionRepository.findByCriteria(criteria)
        );

        // 4. Comprobar que hay suficientes
        if (candidates.size() < input.numberOfQuestions()) {
            throw new BusinessRuleViolationException(
                    "No hay suficientes preguntas con los tags solicitados: "
                            + "se necesitan " + input.numberOfQuestions()
                            + " y solo hay " + candidates.size());
        }

        // 5. Aleatorizar y tomar las primeras N
        Collections.shuffle(candidates);
        List<Long> selectedIds = candidates.stream()
                .limit(input.numberOfQuestions())
                .map(Question::getId)
                .toList();

        // 6. Construir el draft con configuración y tags de generación
        ExamConfig config = ExamConfig.of(
                input.numberOfQuestions(),
                input.timeLimitMinutes(),
                input.showScore(),
                input.randomizeOptions(),
                input.randomizeQuestionOrder()
        );

        Exam exam = Exam.createDraft(
                input.title(),
                input.ownerId(),
                config,
                requiredTags
        );
        exam.setQuestions(selectedIds);

        // 7. Persistir y devolver
        Exam persisted = examRepository.save(exam);
        return ExamView.from(persisted);
    }

    /**
     * Resuelve tags por ID, verificando que existen y pertenecen al profesor.
     * Falla con excepción si algún tag no existe o es de otro profesor.
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
                throw new BusinessRuleViolationException(
                        "El tag con id " + tagId + " no existe");
            }

            resolved.add(tag);
        }
        return resolved;
    }
}