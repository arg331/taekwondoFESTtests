package com.taekwondo.examenes.application.exam.dto;

import com.taekwondo.examenes.application.tag.dto.TagView;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.ExamStatus;
import com.taekwondo.examenes.domain.model.Visibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vista de un examen para devolver desde la capa de aplicación.
 *
 * Reutilizada en todos los casos de uso que devuelven un examen
 * (create, publish, list, get, etc.).
 *
 * Diseño: incluye los TagViews completos (no IDs) porque los clientes
 * casi siempre necesitan mostrar nombre/color de los tags de generación.
 * Las preguntas, en cambio, se mantienen como IDs: si el cliente las
 * necesita, las pide aparte (caso de uso específico para vista detallada).
 */
public record ExamView(
        Long id,
        String title,
        Long ownerId,
        ExamStatus status,
        Visibility visibility,
        ExamConfigView config,
        List<Long> questionIds,
        Set<TagView> generationTags,
        String code,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
    public static ExamView from(Exam exam) {
        Set<TagView> tagViews = exam.getGenerationTags().stream()
                .map(TagView::from)
                .collect(Collectors.toSet());

        return new ExamView(
                exam.getId(),
                exam.getTitle(),
                exam.getOwnerId(),
                exam.getStatus(),
                exam.getVisibility(),
                ExamConfigView.from(exam.getConfig()),
                exam.getQuestionIds(),
                tagViews,
                exam.getCode(),
                exam.getCreatedAt(),
                exam.getExpiresAt()
        );
    }
}