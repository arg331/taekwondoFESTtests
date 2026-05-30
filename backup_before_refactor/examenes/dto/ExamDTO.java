package com.taekwondo.examenes.dto;

import com.taekwondo.examenes.model.Exam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para transferir datos de Exam
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamDTO {

    private Long id;
    private String code;
    private Integer numberOfQuestions;
    private Boolean showScore;
    private Integer timeLimit;
    private Boolean randomizeOptions;
    private List<Long> questionIds;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    /**
     * Constructor desde entidad Exam
     */
    public ExamDTO(Exam exam) {
        this.id = exam.getId();
        this.code = exam.getCode();
        this.numberOfQuestions = exam.getNumberOfQuestions();
        this.showScore = exam.getShowScore();
        this.timeLimit = exam.getTimeLimit();
        this.randomizeOptions = exam.getRandomizeOptions();
        this.questionIds = exam.getQuestionIds();
        this.active = exam.getActive();
        this.createdAt = exam.getCreatedAt();
        this.expiresAt = exam.getExpiresAt();
    }
}
