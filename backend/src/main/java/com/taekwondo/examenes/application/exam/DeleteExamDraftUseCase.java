package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: eliminar un examen en estado DRAFT.
 *
 * IMPORTANTE: solo se eliminan drafts. Los exámenes ya publicados o
 * expirados conservan sus resultados (snapshot histórico) y no se borran
 * desde la aplicación. Si en el futuro hace falta archivar exámenes
 * publicados, se hará con un caso de uso separado y una entidad/estado
 * distinto (archivado vs eliminado).
 *
 * Esto refleja una regla de negocio clara: los exámenes publicados son
 * parte del histórico de la federación; los drafts son trabajo en
 * progreso y se pueden descartar.
 */
public class DeleteExamDraftUseCase {

    private final ExamRepository examRepository;

    public DeleteExamDraftUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public void execute(Long examId, Long requesterOwnerId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + examId));

        if (!exam.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + examId);
        }

        // Regla de negocio: solo se pueden eliminar drafts.
        // Esto NO está en la entidad porque eliminar es responsabilidad
        // del repositorio (no es una operación de la entidad).
        if (!exam.isDraft()) {
            throw new BusinessRuleViolationException(
                    "Solo se pueden eliminar exámenes en estado DRAFT. "
                            + "Estado actual: " + exam.getStatus());
        }

        examRepository.deleteById(examId);
    }
}