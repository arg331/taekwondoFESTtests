package com.taekwondo.examenes.application.exam.dto;

public record RenameExamInput(
        Long examId,
        String newTitle,
        Long requesterOwnerId
) {}