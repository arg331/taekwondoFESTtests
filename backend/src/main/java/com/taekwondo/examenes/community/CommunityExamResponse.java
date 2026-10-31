package com.taekwondo.examenes.community;

import com.taekwondo.examenes.exam.Exam;
import com.taekwondo.examenes.exam.ExamStatus;

import java.time.LocalDateTime;

/** Examen de prueba de otro profesor (RF-30), tal como lo ve un profesor al buscar preguntas que reutilizar. */
public record CommunityExamResponse(
        Long id,
        String title,
        String code,
        String ownerName,
        int questionCount,
        ExamStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        /** Si lo tengo en favoritos (RF-31). */
        boolean favorite
) {
    static CommunityExamResponse from(Exam exam, String ownerName, boolean favorite) {
        return new CommunityExamResponse(exam.getId(), exam.getTitle(), exam.getCode(), ownerName,
                exam.getPublishedQuestions().size(), exam.getStatus(), exam.getCreatedAt(), exam.getExpiresAt(), favorite);
    }
}
