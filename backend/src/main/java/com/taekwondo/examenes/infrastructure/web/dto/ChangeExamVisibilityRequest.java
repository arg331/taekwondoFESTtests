package com.taekwondo.examenes.infrastructure.web.dto;

import com.taekwondo.examenes.domain.model.Visibility;

/**
 * Body de PATCH /api/exams/{id}/visibility.
 *
 * Solo aplicable a exámenes ya publicados (la entidad lo valida).
 */
public record ChangeExamVisibilityRequest(
        Visibility newVisibility
) {}
