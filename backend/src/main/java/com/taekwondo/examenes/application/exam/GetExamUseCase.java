package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: obtener un examen por su ID, comprobando que pertenece
 * al profesor solicitante.
 *
 * Se usa cuando el profesor abre un examen para verlo o editarlo desde
 * su panel. NO es para que un estudiante entre con un QR
 * (para eso está GetExamByCodeUseCase).
 */
public class GetExamUseCase {

    private final ExamRepository examRepository;

    public GetExamUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public ExamView execute(Long examId, Long requesterOwnerId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + examId));

        // Seguridad: no exponemos exámenes que no pertenezcan al solicitante,
        // ni siquiera aunque sean públicos (eso es trabajo de ListPublicExams).
        // Aquí GetExam significa "ver mi examen".
        if (!exam.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + examId);
        }

        return ExamView.from(exam);
    }
}
