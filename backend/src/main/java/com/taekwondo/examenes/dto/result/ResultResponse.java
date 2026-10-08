package com.taekwondo.examenes.dto.result;

import com.taekwondo.examenes.entity.Answer;
import com.taekwondo.examenes.entity.Result;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resultado de una entrega. Cuando el examen oculta la nota (showScore=false)
 * y quien consulta es el alumno, scoreVisible=false y los campos de nota
 * y respuestas van a null.
 */
public record ResultResponse(
        Long id,
        Long examId,
        String examTitle,
        String studentName,
        String studentClub,
        String studentEmail,
        boolean scoreVisible,
        List<AnswerResponse> answers,
        Integer correctAnswers,
        int totalQuestions,
        Integer score,
        Boolean passed,
        int timeSpentSeconds,
        LocalDateTime completedAt
) {
    public record AnswerResponse(Long questionId, Integer studentAnswer, int correctAnswer, boolean correct) {
        static AnswerResponse from(Answer a) {
            return new AnswerResponse(a.getQuestionId(), a.getStudentAnswer(), a.getCorrectAnswer(), a.isCorrect());
        }
    }

    /** Vista completa (profesor, o alumno cuando el examen muestra la nota). */
    public static ResultResponse from(Result r, String examTitle) {
        return new ResultResponse(r.getId(), r.getExamId(), examTitle, r.getStudentName(), r.getStudentClub(),
                r.getStudentEmail(), true,
                r.getAnswers().stream().map(AnswerResponse::from).toList(),
                r.getCorrectAnswers(), r.getTotalQuestions(), r.getScore(), r.isPassed(),
                r.getTimeSpentSeconds(), r.getCompletedAt());
    }

    /** Vista del alumno: respeta showScore del examen. */
    public static ResultResponse forStudent(Result r, String examTitle, boolean showScore) {
        if (showScore) return from(r, examTitle);
        return new ResultResponse(r.getId(), r.getExamId(), examTitle, r.getStudentName(), r.getStudentClub(),
                r.getStudentEmail(), false, null, null, r.getTotalQuestions(), null, null,
                r.getTimeSpentSeconds(), r.getCompletedAt());
    }
}
