package com.taekwondo.examenes.result;

import com.taekwondo.examenes.attempt.StudentIdentity;
import com.taekwondo.examenes.exam.ExamConfig;

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
        /** La hizo un usuario con cuenta, no un anónimo. */
        boolean registered,
        /** Qué alumno la hizo, para agrupar sus intentos (RF-54): «u:<id>» o «n:<nombre normalizado>». */
        String studentKey,
        boolean scoreVisible,
        List<AnswerResponse> answers,
        Integer correctAnswers,
        int totalQuestions,
        Integer score,
        Boolean passed,
        int timeSpentSeconds,
        LocalDateTime completedAt,
        /** Se corrigió sola al acabarse el plazo (RF-24). */
        boolean autoSubmitted
) {
    /**
     * Opciones que marcó el alumno (vacío = en blanco) y las correctas (RF-09). correctAnswers es null en la
     * vista del alumno: nunca ve cuáles eran las correctas (RF-53).
     */
    public record AnswerResponse(Long questionId, List<Integer> studentAnswers, List<Integer> correctAnswers,
                                 boolean correct) {
        static AnswerResponse from(Answer a) {
            return new AnswerResponse(a.getQuestionId(), a.getStudentAnswers(), a.getCorrectAnswers(), a.isCorrect());
        }

        static AnswerResponse withoutSolution(Answer a) {
            return new AnswerResponse(a.getQuestionId(), a.getStudentAnswers(), null, a.isCorrect());
        }
    }

    /** Vista completa, la del profesor. */
    public static ResultResponse from(Result r, String examTitle) {
        return new ResultResponse(r.getId(), r.getExamId(), examTitle, r.getStudentName(), r.getStudentClub(),
                r.getStudentEmail(), r.getStudentUserId() != null,
                StudentIdentity.key(r.getStudentUserId(), r.getStudentName()), true,
                r.getAnswers().stream().map(AnswerResponse::from).toList(),
                r.getCorrectAnswers(), r.getTotalQuestions(), r.getScore(), r.isPassed(),
                r.getTimeSpentSeconds(), r.getCompletedAt(), r.isAutoSubmitted());
    }

    /**
     * Vista del alumno según lo que el profesor eligió mostrar (RF-53): nada, la nota, o la nota y
     * sus respuestas bien o mal, nunca cuál era la correcta.
     */
    public static ResultResponse forStudent(Result r, String examTitle, ExamConfig config) {
        if (!config.isShowScore()) return hidden(r, examTitle);
        List<AnswerResponse> answers = config.isShowAnswers()
                ? r.getAnswers().stream().map(AnswerResponse::withoutSolution).toList()
                : null;
        return new ResultResponse(r.getId(), r.getExamId(), examTitle, r.getStudentName(), r.getStudentClub(),
                r.getStudentEmail(), r.getStudentUserId() != null,
                StudentIdentity.key(r.getStudentUserId(), r.getStudentName()), true, answers,
                r.getCorrectAnswers(), r.getTotalQuestions(), r.getScore(), r.isPassed(),
                r.getTimeSpentSeconds(), r.getCompletedAt(), r.isAutoSubmitted());
    }

    /** Vista del alumno sin nota ni respuestas. */
    public static ResultResponse hidden(Result r, String examTitle) {
        return new ResultResponse(r.getId(), r.getExamId(), examTitle, r.getStudentName(), r.getStudentClub(),
                r.getStudentEmail(), r.getStudentUserId() != null,
                StudentIdentity.key(r.getStudentUserId(), r.getStudentName()), false, null, null, r.getTotalQuestions(), null, null,
                r.getTimeSpentSeconds(), r.getCompletedAt(), r.isAutoSubmitted());
    }
}
