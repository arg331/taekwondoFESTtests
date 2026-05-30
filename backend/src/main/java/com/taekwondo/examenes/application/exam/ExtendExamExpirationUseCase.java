package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.exam.dto.ExtendExamExpirationInput;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.Clock;
import com.taekwondo.examenes.domain.port.ExamRepository;

import java.time.LocalDateTime;

/**
 * Caso de uso: extender la fecha de expiración de un examen publicado.
 *
 * El examen permanece en estado PUBLISHED (no pasa por EXPIRED).
 *
 * Diferencia conceptual con ReopenExam: aquí ALARGAMOS un examen
 * todavía activo; en ReopenExam, REVIVIMOS uno ya cerrado.
 */
public class ExtendExamExpirationUseCase {

    private final ExamRepository examRepository;
    private final Clock clock;

    public ExtendExamExpirationUseCase(ExamRepository examRepository, Clock clock) {
        this.examRepository = examRepository;
        this.clock = clock;
    }

    public ExamView execute(ExtendExamExpirationInput input) {
        Exam exam = examRepository.findById(input.examId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + input.examId()));

        if (!exam.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + input.examId());
        }

        // Si se especifica nueva fecha, debe ser futura
        if (input.newExpiresAt() != null) {
            LocalDateTime now = clock.now();
            if (!input.newExpiresAt().isAfter(now)) {
                throw new BusinessRuleViolationException(
                        "La nueva fecha de expiración debe ser posterior al momento actual");
            }
        }

        // La entidad valida que el estado actual sea PUBLISHED
        exam.extendExpiration(input.newExpiresAt());

        Exam saved = examRepository.save(exam);
        return ExamView.from(saved);
    }
}
