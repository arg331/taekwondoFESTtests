package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.Clock;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: acceso de un estudiante al examen mediante su código (QR).
 *
 * Reglas:
 *  - El examen debe existir.
 *  - Debe ser ACCESIBLE en este momento (PUBLISHED y, si tiene
 *    expiresAt, no haber pasado).
 *  - NO se comprueba ownerId: este caso de uso NO es del profesor;
 *    es de un usuario anónimo (el estudiante) que entra con su QR.
 *
 * Si el examen no es accesible (DRAFT, EXPIRED manual o vencido por
 * tiempo) lanzamos error específico de negocio para que el cliente
 * pueda mostrar un mensaje adecuado (p. ej., "este examen ha cerrado").
 */
public class GetExamByCodeUseCase {

    private final ExamRepository examRepository;
    private final Clock clock;

    public GetExamByCodeUseCase(ExamRepository examRepository, Clock clock) {
        this.examRepository = examRepository;
        this.clock = clock;
    }

    public ExamView execute(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessRuleViolationException("El código no puede estar vacío");
        }

        Exam exam = examRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe ningún examen con código " + code));

        // Verificación on-the-fly: la entidad combina estado y fecha de expiración.
        if (!exam.isAccessibleAt(clock)) {
            throw new BusinessRuleViolationException(
                    "Este examen no está disponible en este momento");
        }

        return ExamView.from(exam);
    }
}
