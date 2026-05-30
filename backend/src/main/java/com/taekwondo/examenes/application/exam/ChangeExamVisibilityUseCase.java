package com.taekwondo.examenes.application.exam;
import com.taekwondo.examenes.application.exam.dto.ChangeExamVisibilityInput;
import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: cambiar la visibilidad (privado/público) de un examen.
 *
 * Solo aplicable si el examen ya está publicado o expirado.
 * La entidad valida que no se cambie en estado DRAFT.
 */
public class ChangeExamVisibilityUseCase {

    private final ExamRepository examRepository;

    public ChangeExamVisibilityUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public ExamView execute(ChangeExamVisibilityInput input) {
        Exam exam = examRepository.findById(input.examId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + input.examId()));

        if (!exam.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + input.examId());
        }

        // La entidad valida el estado (no permite cambiar visibilidad en DRAFT)
        exam.changeVisibility(input.newVisibility());

        Exam saved = examRepository.save(exam);
        return ExamView.from(saved);
    }
}