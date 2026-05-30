package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ChangeExamConfigInput;
import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.ExamConfig;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: cambiar la configuración de un examen.
 *
 * Permitido en cualquier estado (edición libre según decisión de diseño).
 * La entidad ExamConfig valida que los rangos sean correctos al construirse.
 */
public class ChangeExamConfigUseCase {

    private final ExamRepository examRepository;

    public ChangeExamConfigUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public ExamView execute(ChangeExamConfigInput input) {
        Exam exam = examRepository.findById(input.examId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + input.examId()));

        if (!exam.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + input.examId());
        }

        // Construir nueva configuración (la entidad valida rangos: 5-50 preguntas, 0-180 min)
        ExamConfig newConfig = ExamConfig.of(
                input.numberOfQuestions(),
                input.timeLimitMinutes(),
                input.showScore(),
                input.randomizeOptions(),
                input.randomizeQuestionOrder()
        );

        exam.changeConfig(newConfig);

        Exam saved = examRepository.save(exam);
        return ExamView.from(saved);
    }
}