package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.exam.dto.RenameExamInput;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: renombrar un examen.
 *
 * Permitido en cualquier estado (acordado: edición libre).
 */
public class RenameExamUseCase {

    private final ExamRepository examRepository;

    public RenameExamUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public ExamView execute(RenameExamInput input) {
        Exam exam = examRepository.findById(input.examId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + input.examId()));

        if (!exam.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + input.examId());
        }

        exam.rename(input.newTitle());

        Exam saved = examRepository.save(exam);
        return ExamView.from(saved);
    }
}