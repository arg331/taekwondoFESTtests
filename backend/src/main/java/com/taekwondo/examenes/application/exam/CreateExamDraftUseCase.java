package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.CreateExamDraftInput;
import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.ExamConfig;
import com.taekwondo.examenes.domain.port.ExamRepository;

import java.util.Set;

/**
 * Caso de uso: crear un draft de examen VACÍO (modo manual).
 *
 * El profesor configura los parámetros básicos. Después, en la vista
 * de ajuste, añadirá las preguntas una a una desde el banco.
 *
 * Para el modo "pre-generar aleatoriamente con tags" existe otro caso
 * de uso separado (PreGenerateExamDraftUseCase). Mantenemos ambos
 * separados por SRP: son acciones distintas del usuario.
 */
public class CreateExamDraftUseCase {

    private final ExamRepository examRepository;

    public CreateExamDraftUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public ExamView execute(CreateExamDraftInput input) {
        // Construir la configuración (valida rangos: 5-50 preguntas, 0-180 min)
        ExamConfig config = ExamConfig.of(
                input.numberOfQuestions(),
                input.timeLimitMinutes(),
                input.showScore(),
                input.randomizeOptions(),
                input.randomizeQuestionOrder()
        );

        // Crear el draft. Sin tags de generación porque es manual.
        Exam exam = Exam.createDraft(
                input.title(),
                input.ownerId(),
                config,
                Set.of()
        );

        // Persistir y devolver vista
        Exam persisted = examRepository.save(exam);
        return ExamView.from(persisted);
    }
}