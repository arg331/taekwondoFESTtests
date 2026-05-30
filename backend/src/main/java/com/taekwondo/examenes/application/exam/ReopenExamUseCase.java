package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.exam.dto.ReopenExamInput;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.Clock;
import com.taekwondo.examenes.domain.port.ExamRepository;

import java.time.LocalDateTime;

/**
 * Caso de uso: reabrir un examen previamente cerrado (estado EXPIRED).
 *
 * Vuelve a PUBLISHED y aplica la nueva fecha de expiración.
 * Mantiene el código original (el QR sigue siendo válido).
 *
 * Requiere Clock para validar que la nueva fecha sea futura.
 */
public class ReopenExamUseCase {

    private final ExamRepository examRepository;
    private final Clock clock;

    public ReopenExamUseCase(ExamRepository examRepository, Clock clock) {
        this.examRepository = examRepository;
        this.clock = clock;
    }

    public ExamView execute(ReopenExamInput input) {
        Exam exam = examRepository.findById(input.examId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + input.examId()));

        if (!exam.getOwnerId().equals(input.requesterOwnerId())) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + input.examId());
        }

        // Si hay nueva fecha de expiración, validamos que sea futura
        if (input.newExpiresAt() != null) {
            LocalDateTime now = clock.now();
            if (!input.newExpiresAt().isAfter(now)) {
                throw new BusinessRuleViolationException(
                        "La nueva fecha de expiración debe ser posterior al momento actual");
            }
        }

        // La entidad valida que el estado actual sea EXPIRED
        exam.reopen(input.newExpiresAt());

        Exam saved = examRepository.save(exam);
        return ExamView.from(saved);
    }
}