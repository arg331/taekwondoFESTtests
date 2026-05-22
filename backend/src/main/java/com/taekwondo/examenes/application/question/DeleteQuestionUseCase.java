package com.taekwondo.examenes.application.question;

import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.port.QuestionRepository;

/**
 * Caso de uso: eliminar una pregunta.
 *
 * RECUERDA: por el modelo híbrido, los exámenes publicados guardan IDs de
 * preguntas (sin FK estricta). Eliminar la pregunta NO afecta a los exámenes
 * históricos: el examen conserva su lista de IDs, simplemente la pregunta
 * ya no existe para añadirla a exámenes futuros.
 */
public class DeleteQuestionUseCase {

    private final QuestionRepository questionRepository;

    public DeleteQuestionUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public void execute(Long questionId, Long requesterOwnerId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe una pregunta con id " + questionId));

        if (!question.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe una pregunta con id " + questionId);
        }

        questionRepository.deleteById(questionId);
    }
}