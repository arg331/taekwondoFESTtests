package com.taekwondo.examenes.application.result;

import com.taekwondo.examenes.application.result.dto.ExamStatisticsView;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.Result;
import com.taekwondo.examenes.domain.port.ExamRepository;
import com.taekwondo.examenes.domain.port.ResultRepository;

import java.util.List;

/**
 * Caso de uso: calcular estadísticas agregadas de un examen.
 *
 * Solo el dueño del examen puede consultar sus estadísticas.
 *
 * Las estadísticas se calculan en memoria a partir de los resultados.
 * Si en el futuro hay volúmenes muy altos, se pueden mover a consultas
 * SQL específicas en el repositorio sin cambiar el contrato de este
 * caso de uso (OCP).
 */
public class GetExamStatisticsUseCase {

    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;

    public GetExamStatisticsUseCase(ResultRepository resultRepository,
                                      ExamRepository examRepository) {
        this.resultRepository = resultRepository;
        this.examRepository = examRepository;
    }

    public ExamStatisticsView execute(Long examId, Long requesterOwnerId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + examId));

        if (!exam.getOwnerId().equals(requesterOwnerId)) {
            throw new ResourceNotFoundException(
                    "No existe un examen con id " + examId);
        }

        List<Result> results = resultRepository.findAllByExamId(examId);

        if (results.isEmpty()) {
            return new ExamStatisticsView(examId, 0, 0.0, 0, 0, 0, 0);
        }

        int totalAttempts = results.size();
        double averageScore = results.stream()
                .mapToInt(Result::getScore)
                .average()
                .orElse(0.0);
        int passedCount = (int) results.stream().filter(Result::isPassed).count();
        int failedCount = totalAttempts - passedCount;
        int highestScore = results.stream()
                .mapToInt(Result::getScore)
                .max()
                .orElse(0);
        int lowestScore = results.stream()
                .mapToInt(Result::getScore)
                .min()
                .orElse(0);

        return new ExamStatisticsView(
                examId,
                totalAttempts,
                averageScore,
                passedCount,
                failedCount,
                highestScore,
                lowestScore
        );
    }
}
