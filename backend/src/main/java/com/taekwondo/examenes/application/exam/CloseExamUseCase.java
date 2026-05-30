package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: cerrar manualmente un examen publicado.
 *
 * Equivale a "marcar como EXPIRED" antes de que llegue su fecha natural
 * de expiración. Útil cuando el profesor quiere cortar el acceso de los
 * estudiantes inmediatamente (por ejemplo, terminada la clase).
 *
 * La entidad valida que solo se cierren exámenes en estado PUBLISHED.
 */
public class CloseExamUseCase {

    private final ExamRepository examRepository;

    public CloseExamUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public ExamView execute(Long examId, Long requesterOwnerId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + examId));

        if (!exam.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + examId);
        }

        // La entidad valida el estado (debe estar PUBLISHED)
        exam.closeManually();

        Exam saved = examRepository.save(exam);
        return ExamView.from(saved);
    }
}