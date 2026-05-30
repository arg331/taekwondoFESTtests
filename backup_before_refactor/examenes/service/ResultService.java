package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.ResultDTO;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.model.Result;
import com.taekwondo.examenes.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para la lógica de negocio de Result
 */
@Service
@RequiredArgsConstructor
public class ResultService {

    private final ResultRepository resultRepository;

    /**
     * Obtener todos los resultados
     */
    @Transactional(readOnly = true)
    public List<ResultDTO> getAllResults() {
        return resultRepository.findAll().stream()
                .map(result -> new ResultDTO(result, false)) // Sin detalles de respuestas
                .collect(Collectors.toList());
    }

    /**
     * Obtener resultado por ID (con detalles completos)
     */
    @Transactional(readOnly = true)
    public ResultDTO getResultById(Long id) {
        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resultado no encontrado con ID: " + id));
        return new ResultDTO(result); // Con detalles de respuestas
    }

    /**
     * Obtener resultados por examen
     */
    @Transactional(readOnly = true)
    public List<ResultDTO> getResultsByExam(Long examId) {
        return resultRepository.findByExamId(examId).stream()
                .map(result -> new ResultDTO(result, false))
                .collect(Collectors.toList());
    }

    /**
     * Buscar resultados por nombre de estudiante
     */
    @Transactional(readOnly = true)
    public List<ResultDTO> searchResultsByStudent(String studentName) {
        return resultRepository.findByStudentNameContainingIgnoreCase(studentName).stream()
                .map(result -> new ResultDTO(result, false))
                .collect(Collectors.toList());
    }

    /**
     * Obtener resultados de un examen ordenados por puntuación
     */
    @Transactional(readOnly = true)
    public List<ResultDTO> getResultsByExamOrderedByScore(Long examId) {
        return resultRepository.findByExamIdOrderByScoreDesc(examId).stream()
                .map(result -> new ResultDTO(result, false))
                .collect(Collectors.toList());
    }

    /**
     * Obtener últimos 10 resultados
     */
    @Transactional(readOnly = true)
    public List<ResultDTO> getLatestResults() {
        return resultRepository.findTop10ByOrderByCompletedAtDesc().stream()
                .map(result -> new ResultDTO(result, false))
                .collect(Collectors.toList());
    }

    /**
     * Obtener estadísticas de un examen
     */
    @Transactional(readOnly = true)
    public ExamStatistics getExamStatistics(Long examId) {
        Double averageScore = resultRepository.getAverageScoreByExam(examId);
        long totalResults = resultRepository.findByExamId(examId).size();
        long passedCount = resultRepository.countPassedResults(examId, 70); // 70% = aprobado
        
        return new ExamStatistics(
            examId,
            totalResults,
            averageScore != null ? averageScore : 0.0,
            passedCount,
            totalResults - passedCount
        );
    }

    /**
     * Eliminar resultado
     */
    @Transactional
    public void deleteResult(Long id) {
        if (!resultRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resultado no encontrado con ID: " + id);
        }
        resultRepository.deleteById(id);
    }

    /**
     * Clase interna para estadísticas
     */
    public static class ExamStatistics {
        public final Long examId;
        public final long totalAttempts;
        public final double averageScore;
        public final long passedCount;
        public final long failedCount;

        public ExamStatistics(Long examId, long totalAttempts, double averageScore, long passedCount, long failedCount) {
            this.examId = examId;
            this.totalAttempts = totalAttempts;
            this.averageScore = averageScore;
            this.passedCount = passedCount;
            this.failedCount = failedCount;
        }
    }
}
