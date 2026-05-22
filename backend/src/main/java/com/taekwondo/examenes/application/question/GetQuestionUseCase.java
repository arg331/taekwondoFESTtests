package com.taekwondo.examenes.application.question;

import com.taekwondo.examenes.application.question.dto.QuestionView;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.port.QuestionRepository;

/**
 * Caso de uso: obtener una pregunta por su ID, comprobando que pertenece
 * al profesor que la solicita.
 */
public class GetQuestionUseCase {

    private final QuestionRepository questionRepository;

    public GetQuestionUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public QuestionView execute(Long questionId, Long requesterOwnerId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una pregunta con id " + questionId));

        // Seguridad a nivel de aplicación: no exponer preguntas ajenas
        if (!question.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe una pregunta con id " + questionId);
        }

        return QuestionView.from(question);
    }
}