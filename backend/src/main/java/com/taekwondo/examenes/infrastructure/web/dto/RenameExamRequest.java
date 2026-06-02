package com.taekwondo.examenes.infrastructure.web.dto;

/**
 * Body de PATCH /api/exams/{id}/title.
 */
public record RenameExamRequest(
        String newTitle
) {}
