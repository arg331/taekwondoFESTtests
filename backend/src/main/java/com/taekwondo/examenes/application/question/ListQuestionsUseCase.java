package com.taekwondo.examenes.application.question;

import com.taekwondo.examenes.application.question.dto.QuestionView;
import com.taekwondo.examenes.domain.port.QuestionRepository;

import java.util.List;

/**
 * Caso de uso: listar todas las preguntas de un profesor.
 *
 * El filtrado por propietario lo hace el repositorio. Aquí solo convertimos
 * a vistas.
 */
public class ListQuestionsUseCase {

    private final QuestionRepository questionRepository;

    public ListQuestionsUseCase(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<QuestionView> execute(Long ownerId) {
        return questionRepository.findAllByOwnerId(ownerId).stream()
                .map(QuestionView::from)
                .toList();
    }
}