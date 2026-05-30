package com.taekwondo.examenes.application.exam.dto;

import com.taekwondo.examenes.domain.model.ExamConfig;

/**
 * Vista de la configuración de un examen.
 *
 * No devolvemos directamente ExamConfig (entidad de dominio) hacia el
 * exterior: separamos la representación pública del modelo interno.
 */
public record ExamConfigView(
        int numberOfQuestions,
        Integer timeLimitMinutes,
        boolean showScore,
        boolean randomizeOptions,
        boolean randomizeQuestionOrder
) {
    public static ExamConfigView from(ExamConfig config) {
        return new ExamConfigView(
                config.getNumberOfQuestions(),
                config.getTimeLimitMinutes(),
                config.isShowScore(),
                config.isRandomizeOptions(),
                config.isRandomizeQuestionOrder()
        );
    }
}