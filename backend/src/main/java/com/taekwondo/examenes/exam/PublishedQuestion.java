package com.taekwondo.examenes.exam;

import com.taekwondo.examenes.question.Question;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Copia de una pregunta tal como se publicó en un examen (DE-03). Los intentos y la corrección
 * usan esta copia: editar la pregunta del banco después no cambia el examen.
 */
@Entity
@Table(name = "published_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublishedQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int position;

    /** Pregunta del banco de la que sale; identifica la respuesta del alumno. */
    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(nullable = false, length = 1000)
    private String text;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "published_question_options",
            joinColumns = @JoinColumn(name = "published_question_id"))
    @OrderColumn(name = "option_order")
    @Column(name = "option_text", nullable = false, length = 500)
    private List<String> options = new ArrayList<>();

    @Column(name = "correct_answer", nullable = false)
    private int correctAnswer;

    @Column(length = 2000)
    private String explanation;

    private PublishedQuestion(int position, Question source) {
        this.position = position;
        this.questionId = source.getId();
        this.text = source.getText();
        this.options.addAll(source.getOptions());
        this.correctAnswer = source.getCorrectAnswer();
        this.explanation = source.getExplanation();
    }

    static PublishedQuestion copyOf(int position, Question source) {
        return new PublishedQuestion(position, source);
    }

    /** ¿La pregunta del banco sigue igual que cuando se publicó? */
    public boolean matches(Question current) {
        return text.equals(current.getText())
                && options.equals(current.getOptions())
                && correctAnswer == current.getCorrectAnswer()
                && Objects.equals(explanation, current.getExplanation());
    }

    public List<String> getOptions() {
        return Collections.unmodifiableList(options);
    }
}
