package com.taekwondo.examenes.dto;

import com.taekwondo.examenes.model.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para transferir datos de Question
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {

    private Long id;
    private String category;
    private String difficulty;
    private String text;
    private List<String> options;
    private Integer correctAnswer;
    private String explanation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Constructor desde entidad Question
     */
    public QuestionDTO(Question question) {
        this.id = question.getId();
        this.category = question.getCategory().name();
        this.difficulty = question.getDifficulty().name();
        this.text = question.getText();
        this.options = question.getOptions();
        this.correctAnswer = question.getCorrectAnswer();
        this.explanation = question.getExplanation();
        this.createdAt = question.getCreatedAt();
        this.updatedAt = question.getUpdatedAt();
    }

    /**
     * Convertir a entidad Question (para crear/actualizar)
     */
    public Question toEntity() {
        Question question = new Question();
        question.setId(this.id);
        question.setCategory(Question.Category.valueOf(this.category));
        question.setDifficulty(Question.Difficulty.valueOf(this.difficulty));
        question.setText(this.text);
        question.setOptions(this.options);
        question.setCorrectAnswer(this.correctAnswer);
        question.setExplanation(this.explanation);
        return question;
    }
}
