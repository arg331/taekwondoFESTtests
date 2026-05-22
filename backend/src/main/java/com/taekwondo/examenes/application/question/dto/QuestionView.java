package com.taekwondo.examenes.application.question.dto;

import com.taekwondo.examenes.application.tag.dto.TagView;
import com.taekwondo.examenes.domain.model.Difficulty;
import com.taekwondo.examenes.domain.model.Question;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vista de una pregunta para devolver desde la capa de aplicación.
 *
 * Reutilizada en múltiples casos de uso (create, edit, list, get, search).
 *
 * Decisión: incluye TagView (no IDs sueltos). Razón: cuando un cliente
 * (controller, test) recibe una pregunta, casi siempre necesita también
 * el nombre y color de sus tags para mostrarlos. Devolver solo IDs
 * obligaría al cliente a hacer una segunda llamada.
 */
public record QuestionView(
        Long id,
        String text,
        List<String> options,
        int correctAnswer,
        String explanation,
        Difficulty difficulty,
        Set<TagView> tags,
        Long ownerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuestionView from(Question question) {
        Set<TagView> tagViews = question.getTags().stream()
                .map(TagView::from)
                .collect(Collectors.toSet());

        return new QuestionView(
                question.getId(),
                question.getText(),
                question.getOptions(),
                question.getCorrectAnswer(),
                question.getExplanation(),
                question.getDifficulty(),
                tagViews,
                question.getOwnerId(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}