package com.taekwondo.examenes.application.result;

import com.taekwondo.examenes.application.result.dto.ResultView;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.Result;
import com.taekwondo.examenes.domain.port.ExamRepository;
import com.taekwondo.examenes.domain.port.ResultRepository;

/**
 * Caso de uso: obtener un resultado por su ID.
 *
 * El profesor solicitante solo puede ver resultados de SUS exámenes.
 * Como Result solo guarda examId (no ownerId), el caso de uso resuelve
 * el examen para verificar pertenencia.
 *
 * Esto es coordinación entre agregados (Result + Exam), por eso vive
 * en la capa de aplicación.
 */
public class GetResultUseCase {

    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;

    public GetResultUseCase(ResultRepository resultRepository,
                             ExamRepository examRepository) {
        this.resultRepository = resultRepository;
        this.examRepository = examRepository;
    }

    public ResultView execute(Long resultId, Long requesterOwnerId) {
        Result result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un resultado con id " + resultId));

        // Verificar que el examen al que pertenece este resultado
        // es propiedad del solicitante.
        Exam exam = examRepository.findById(result.getExamId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un resultado con id " + resultId));

        if (!exam.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe un resultado con id " + resultId);
        }

        return ResultView.from(result);
    }
}
