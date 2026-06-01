package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.question.dto.QuestionView;
import com.taekwondo.examenes.domain.model.Difficulty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO HTTP de salida para una pregunta.
 *
 * Convertido desde QuestionView (aplicación). Reutiliza TagResponse
 * para los tags, manteniendo formato JSON consistente con el endpoint
 * de tags.
 */
public record QuestionResponse(
        Long id,
        String text,
        List<String> options,
        int correctAnswer,
        String explanation,
        Difficulty difficulty,
        Set<TagResponse> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuestionResponse from(QuestionView view) {
        Set<TagResponse> tagResponses = view.tags().stream()
                .map(TagResponse::from)
                .collect(Collectors.toSet());

        return new QuestionResponse(
                view.id(),
                view.text(),
                view.options(),
                view.correctAnswer(),
                view.explanation(),
                view.difficulty(),
                tagResponses,
                view.createdAt(),
                view.updatedAt()
        );
    }
}
