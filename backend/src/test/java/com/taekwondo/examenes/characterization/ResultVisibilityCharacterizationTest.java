package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-53: el profesor elige qué ve el alumno al entregar: nada, la nota, o la nota y sus respuestas
 * (bien o mal, nunca cuál era la correcta). El profesor lo ve siempre todo.
 */
@DisplayName("Caracterización: qué ve el alumno al entregar (RF-53)")
class ResultVisibilityCharacterizationTest extends ApiCharacterizationTest {

    @Test
    @DisplayName("Nota y respuestas: ve su nota y qué tiene bien o mal, pero no cuál era la correcta")
    void scoreAndAnswers_studentNeverSeesCorrectOption() {
        String token = adminToken();
        PublishedExam exam = published(token, true, true);
        Session student = newStudent();
        List<Integer> chosen = new ArrayList<>(allCorrect(5));
        chosen.set(0, 1);

        HttpResult result = submit(exam.code(), startAttempt(exam.code(), student.token(), "Yo"), student.token(),
                answers(exam.questionIds(), chosen)).expectStatus(201);

        assertThat((Integer) result.read("$.score")).isEqualTo(80);
        List<Map<String, Object>> answers = result.read("$.answers");
        assertThat(answers).extracting(a -> a.get("correct")).containsExactly(false, true, true, true, true);
        assertThat(answers).extracting(a -> a.get("studentAnswer")).containsExactly(1, 1, 2, 3, 0);
        assertThat(answers).allMatch(a -> a.containsKey("correctAnswer") && a.get("correctAnswer") == null);

        List<Map<String, Object>> history = get("/api/results/me", student.token()).expectStatus(200).asList();
        assertThat((List<Map<String, Object>>) history.get(0).get("answers")).allMatch(a -> a.get("correctAnswer") == null);
        List<Map<String, Object>> teacher = get("/api/results/exam/" + exam.id(), token).expectStatus(200).asList();
        assertThat((List<Map<String, Object>>) teacher.get(0).get("answers"))
                .extracting(a -> a.get("correctAnswer")).containsExactly(0, 1, 2, 3, 0);
    }

    @Test
    @DisplayName("Solo nota: ve la nota y los aciertos, pero no sus respuestas")
    void scoreOnly_noAnswers() {
        PublishedExam exam = published(adminToken(), true, false);

        HttpResult result = submit(exam.code(), startAttempt(exam.code(), null), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);

        assertThat((Boolean) result.read("$.scoreVisible")).isTrue();
        assertThat((Integer) result.read("$.score")).isEqualTo(100);
        assertThat((Integer) result.read("$.correctAnswers")).isEqualTo(5);
        assertThat((Object) result.read("$.answers")).isNull();
    }

    @Test
    @DisplayName("Nada: ni nota ni respuestas; el profesor lo ve todo")
    void nothing_hidden() {
        String token = adminToken();
        PublishedExam exam = published(token, false, false);

        HttpResult result = submit(exam.code(), startAttempt(exam.code(), null), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);

        assertThat((Boolean) result.read("$.scoreVisible")).isFalse();
        assertThat((Object) result.read("$.score")).isNull();
        assertThat((Object) result.read("$.answers")).isNull();
        assertThat(get("/api/results/exam/" + exam.id(), token).expectStatus(200).asList().get(0).get("score"))
                .isEqualTo(100);
    }

    @Test
    @DisplayName("Configuración: sin indicarlo, las respuestas se muestran si se muestra la nota; sin nota, nunca")
    void config_defaultsAndConsistency() {
        String token = adminToken();

        assertThat((Boolean) draft(token, true, null).read("$.config.showAnswers")).isTrue();
        assertThat((Boolean) draft(token, false, null).read("$.config.showAnswers")).isFalse();
        assertThat((Boolean) draft(token, true, false).read("$.config.showAnswers")).isFalse();
        assertThat((Boolean) draft(token, false, true).read("$.config.showAnswers")).isFalse();
    }

    // ───── Ayudas ─────

    private HttpResult draft(String token, boolean showScore, Boolean showAnswers) {
        Map<String, Object> body = new HashMap<>(draftBody(unique("Examen"), 5, null, showScore, false, false));
        body.put("showAnswers", showAnswers);
        return post("/api/exams/drafts", token, body).expectStatus(201);
    }

    private PublishedExam published(String token, boolean showScore, boolean showAnswers) {
        long examId = draft(token, showScore, showAnswers).id();
        List<Long> questionIds = createQuestions(token, 5);
        setQuestions(token, examId, questionIds);
        String code = publish(token, examId, "PUBLIC", null, null).expectStatus(200).read("$.code");
        return new PublishedExam(examId, code, questionIds);
    }
}
