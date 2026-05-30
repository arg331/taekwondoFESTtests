package com.taekwondo.examenes.dto;

import com.taekwondo.examenes.model.Result;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO para transferir datos de Result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDTO {

    private Long id;
    private Long examId;
    private String examCode;
    private String studentName;
    private String studentClub;
    private String studentEmail;
    private Integer score;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Integer timeSpent;
    private LocalDateTime completedAt;
    private List<AnswerDetailDTO> answers;

    /**
     * Constructor desde entidad Result
     */
    public ResultDTO(Result result) {
        this.id = result.getId();
        this.examId = result.getExam().getId();
        this.examCode = result.getExam().getCode();
        this.studentName = result.getStudentName();
        this.studentClub = result.getStudentClub();
        this.studentEmail = result.getStudentEmail();
        this.score = result.getScore();
        this.correctAnswers = result.getCorrectAnswers();
        this.totalQuestions = result.getTotalQuestions();
        this.timeSpent = result.getTimeSpent();
        this.completedAt = result.getCompletedAt();
        this.answers = result.getAnswers().stream()
                .map(AnswerDetailDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Constructor sin detalles de respuestas (para listados)
     */
    public ResultDTO(Result result, boolean includeAnswers) {
        this(result);
        if (!includeAnswers) {
            this.answers = null;
        }
    }

    /**
     * DTO para el detalle de cada respuesta
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDetailDTO {
        private Long questionId;
        private Integer studentAnswer;
        private Integer correctAnswer;
        private Boolean isCorrect;

        public AnswerDetailDTO(Result.Answer answer) {
            this.questionId = answer.getQuestionId();
            this.studentAnswer = answer.getStudentAnswer();
            this.correctAnswer = answer.getCorrectAnswer();
            this.isCorrect = answer.getIsCorrect();
        }
    }
}
