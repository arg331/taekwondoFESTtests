package com.taekwondo.examenes.attempt;

import com.taekwondo.examenes.common.OptionListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Una pregunta tal como la ve el alumno en su intento (RF-24): en qué orden salen sus opciones y
 * cuáles tiene marcadas. Inmutable: contestar crea otra.
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttemptQuestion {

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    /** Índices originales de las opciones, en el orden en que se muestran. */
    @Convert(converter = OptionListConverter.class)
    @Column(name = "option_order", nullable = false, length = 100)
    private List<Integer> optionOrder;

    /** Opciones marcadas, de menor a mayor; vacío = sin responder. */
    @Convert(converter = OptionListConverter.class)
    @Column(name = "chosen_options", nullable = false, length = 100)
    private List<Integer> chosenOptions;

    public AttemptQuestion(Long questionId, List<Integer> optionOrder) {
        this(questionId, optionOrder, List.of());
    }

    private AttemptQuestion(Long questionId, List<Integer> optionOrder, List<Integer> chosenOptions) {
        this.questionId = questionId;
        this.optionOrder = List.copyOf(optionOrder);
        this.chosenOptions = chosenOptions.stream().distinct().sorted().toList();
    }

    AttemptQuestion withChosenOptions(List<Integer> chosen) {
        return new AttemptQuestion(questionId, optionOrder, chosen);
    }
}
