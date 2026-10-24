package com.taekwondo.examenes.exam;

/**
 * La portada pública de un examen, para quien tiene el código (DE-19): solo lo que se enseña antes de empezar.
 * Sin ids del profesor ni de las preguntas, tags ni fechas internas.
 */
public record PublicExamResponse(
        String title,
        String code,
        /** De prueba (público) u oficial (privado), RF-49. */
        Visibility visibility,
        /** Si hace falta cuenta para hacerlo (DE-06). */
        ExamAccessMode accessMode,
        Config config,
        /** Intentos por alumno; null = sin límite (RF-52). */
        Integer maxAttempts
) {
    public record Config(int numberOfQuestions, Integer timeLimitMinutes) {}

    static PublicExamResponse from(Exam exam) {
        return new PublicExamResponse(exam.getTitle(), exam.getCode(), exam.getVisibility(), exam.getAccessMode(),
                new Config(exam.getConfig().getNumberOfQuestions(), exam.getConfig().getTimeLimitMinutes()),
                exam.getMaxAttempts());
    }
}
