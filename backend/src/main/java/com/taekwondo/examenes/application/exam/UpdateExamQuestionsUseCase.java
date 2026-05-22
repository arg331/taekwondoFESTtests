package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.exam.dto.UpdateExamQuestionsInput;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.ExamRepository;
import com.taekwondo.examenes.domain.port.QuestionRepository;

/**
 * Caso de uso: reemplazar la lista de preguntas de un examen.
 *
 * Valida que:
 *  - El examen existe y pertenece al solicitante.
 *  - Cada pregunta de la lista existe y pertenece al mismo profesor.
 *  - No hay duplicados en la lista.
 */
public class UpdateExamQuestionsUseCase {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;

    public UpdateExamQuestionsUseCase(ExamRepository examRepository,
                                       QuestionRepository questionRepository) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
    }

    public ExamView execute(UpdateExamQuestionsInput input) {
        Exam exam = examRepository.findById(input.examId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + input.examId()));

        if (!exam.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + input.examId());
        }

        // Validar duplicados (la entidad no lo hace porque setQuestions reemplaza
        // toda la lista, pero conceptualmente no tiene sentido tener una misma
        // pregunta dos veces en el mismo examen)
        if (input.questionIds().size() != Set.copyOf(input.questionIds()).size()) {
            throw new BusinessRuleViolationException(
                    "Hay preguntas duplicadas en la lista");
        }

        // Validar que cada pregunta existe y pertenece al profesor
        for (Long questionId : input.questionIds()) {
            var question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "La pregunta con id " + questionId + " no existe"));

            if (!question.getOwnerId().equals(input.requesterOwnerId())) {
                throw new BusinessRuleViolationException(
                        "La pregunta con id " + questionId + " no existe");
            }
        }

        // Aplicar
        exam.setQuestions(input.questionIds());

        Exam saved = examRepository.save(exam);
        return ExamView.from(saved);
    }
}