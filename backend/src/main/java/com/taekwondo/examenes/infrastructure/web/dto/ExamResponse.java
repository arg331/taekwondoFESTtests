package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.domain.model.ExamAccessMode;
import com.taekwondo.examenes.domain.model.ExamStatus;
import com.taekwondo.examenes.domain.model.Visibility;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO de salida con la información de un examen.
 *
 * Las questionIds vienen como lista de Long; si el cliente necesita el
 * detalle de las preguntas, las pide por separado al endpoint de Question.
 *
 * Los tags se serializan como TagResponse para reutilizar la representación
 * pública que ya conoce el frontend.
 */
public record ExamResponse(
        Long id,
        String title,
        Long ownerId,
        ExamStatus status,
        Visibility visibility,
        ExamAccessMode accessMode,
        ExamConfigResponse config,
        List<Long> questionIds,
        Set<TagResponse> generationTags,
        String code,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
    public static ExamResponse from(ExamView view) {
        Set<TagResponse> tagResponses = view.generationTags().stream()
                .map(TagResponse::from)
                .collect(Collectors.toSet());

        return new ExamResponse(
                view.id(),
                view.title(),
                view.ownerId(),
                view.status(),
                view.visibility(),
                view.accessMode(),
                ExamConfigResponse.from(view.config()),
                view.questionIds(),
                tagResponses,
                view.code(),
                view.createdAt(),
                view.expiresAt()
        );
    }
}
