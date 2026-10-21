package com.taekwondo.examenes.attempt;

import com.taekwondo.examenes.common.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Intento de un alumno en un examen (DE-04). Se registra al empezar, con quién lo hace,
 * y se marca al entregarlo. Un intento sin entregar y fuera de plazo cuenta como gastado.
 * Guarda las preguntas en el orden en que las ve el alumno y lo que va contestando (RF-24).
 * studentUserId es null cuando el alumno es anónimo: entonces se le reconoce por el nombre.
 */
@Entity
@Table(name = "exam_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "student_user_id")
    private Long studentUserId;

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    /** Nombre sin mayúsculas, tildes ni espacios de más: así se reconoce al anónimo (RN-08). */
    @Column(name = "student_key", nullable = false, length = 100)
    private String studentKey;

    @Column(name = "student_club", length = 100)
    private String studentClub;

    @Column(name = "student_email", length = 200)
    private String studentEmail;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "attempt_questions", joinColumns = @JoinColumn(name = "attempt_id"))
    @OrderColumn(name = "position")
    private List<AttemptQuestion> questions = new ArrayList<>();

    public ExamAttempt(Long examId, Long studentUserId, String studentName, String studentClub,
                       String studentEmail, LocalDateTime startedAt) {
        this.examId = examId;
        this.studentUserId = studentUserId;
        this.studentName = studentName;
        this.studentKey = StudentIdentity.nameKey(studentName);
        this.studentClub = studentClub;
        this.studentEmail = studentEmail;
        this.startedAt = startedAt;
    }

    public boolean isSubmitted() {
        return submittedAt != null;
    }

    /** Sin entregar y dentro de su plazo: todavía se puede contestar y entregar. */
    public boolean isOpenAt(LocalDateTime now, Duration validity) {
        return !isSubmitted() && !now.isAfter(startedAt.plus(validity));
    }

    /** Sin entregar y con el plazo acabado: toca corregirlo con lo guardado. */
    public boolean isExpiredAt(LocalDateTime now, Duration validity) {
        return !isSubmitted() && now.isAfter(startedAt.plus(validity));
    }

    /** Las preguntas que le tocan, en el orden en que las verá. Solo una vez. */
    public void assignQuestions(List<AttemptQuestion> layout) {
        if (!questions.isEmpty()) {
            throw new IllegalStateException("El intento " + id + " ya tiene preguntas");
        }
        questions.addAll(layout);
    }

    public List<AttemptQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    /** Guarda lo marcado en una pregunta, sustituyendo lo anterior; las opciones tienen que ser de esa pregunta. */
    public void saveAnswer(Long questionId, List<Integer> chosen) {
        if (isSubmitted()) {
            throw new BusinessRuleException("Este intento ya se ha entregado");
        }
        int position = positionOf(questionId);
        AttemptQuestion question = questions.get(position);
        chosen.stream().filter(option -> !question.getOptionOrder().contains(option)).findFirst()
                .ifPresent(option -> {
                    throw new BusinessRuleException("La opción " + option + " no existe en la pregunta " + questionId);
                });
        questions.set(position, question.withChosenOptions(chosen));
    }

    /** Lo marcado en cada pregunta, en el orden del intento; vacío = sin responder. */
    public Map<Long, List<Integer>> getChosenOptions() {
        Map<Long, List<Integer>> chosen = new LinkedHashMap<>();
        questions.forEach(q -> chosen.put(q.getQuestionId(), q.getChosenOptions()));
        return chosen;
    }

    private int positionOf(Long questionId) {
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getQuestionId().equals(questionId)) return i;
        }
        throw new BusinessRuleException("La pregunta " + questionId + " no es de este examen");
    }

}
