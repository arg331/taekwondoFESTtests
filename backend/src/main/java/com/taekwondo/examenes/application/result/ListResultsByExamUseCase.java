package com.taekwondo.examenes.application.result;

import com.taekwondo.examenes.application.result.dto.ResultView;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.port.ExamRepository;
import com.taekwondo.examenes.domain.port.ResultRepository;

import java.util.List;

/**
 * Caso de uso: listar todos los resultados de un examen.
 *
 * Solo el dueño del examen puede ver sus resultados.
 */
public class ListResultsByExamUseCase {

    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;

    public ListResultsByExamUseCase(ResultRepository resultRepository,
                                      ExamRepository examRepository) {
        this.resultRepository = resultRepository;
        this.examRepository = examRepository;
    }

    public List<ResultView> execute(Long examId, Long requesterOwnerId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + examId));

        if (!exam.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + examId);
        }

        return resultRepository.findAllByExamId(examId).stream()
                .map(ResultView::from)
                .toList();
    }
}
