package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-09 (prueba del 18/10/2026): una pregunta puede tener varias respuestas correctas. El alumno sabe que
 * tiene que marcar todas y acierta solo si marca exactamente las correctas.
 */
@DisplayName("Caracterización: varias respuestas correctas (RF-09)")
class MultipleCorrectAnswersCharacterizationTest extends ApiCharacterizationTest {

    private static final String INVALID = "Respuestas correctas: debe señalar una o más opciones de la pregunta, sin repetir";

    @Test
    @DisplayName("Una pregunta guarda varias correctas; vacías, repetidas o fuera de las opciones dan 400")
    void question_severalCorrectAnswers() {
        String token = adminToken();

        HttpResult created = post("/api/questions", token, question(List.of(2, 0))).expectStatus(201);

        assertThat((List<Integer>) created.read("$.correctAnswers")).containsExactly(0, 2);
        assertThat(post("/api/questions", token, question(List.of())).expectStatus(400).message()).isEqualTo(INVALID);
        assertThat(post("/api/questions", token, question(List.of(1, 1))).expectStatus(400).message()).isEqualTo(INVALID);
        assertThat(post("/api/questions", token, question(List.of(0, 4))).expectStatus(400).message()).isEqualTo(INVALID);
    }

    @Test
    @DisplayName("En el intento, la pregunta dice si tiene varias correctas, sin decir cuáles")
    void attempt_flagsMultipleCorrect() {
        String token = adminToken();
        PublishedExam exam = examWithFirstQuestionCorrect(token, List.of(0, 2));

        List<Map<String, Object>> questions = start(exam.code(), null, obj("studentName", unique("A")))
                .expectStatus(200).read("$.questions");

        Map<String, Object> first = questions.stream()
                .filter(q -> ((Number) q.get("id")).longValue() == exam.questionIds().get(0)).findFirst().orElseThrow();
        assertThat(first).containsEntry("multipleCorrect", true).doesNotContainKey("correctAnswers");
        assertThat(questions).filteredOn(q -> q != first).allMatch(q -> Boolean.FALSE.equals(q.get("multipleCorrect")));
    }

    @Test
    @DisplayName("Corrección: acierta solo si marca exactamente las correctas")
    void correction_exactMatchOnly() {
        String token = adminToken();
        PublishedExam exam = examWithFirstQuestionCorrect(token, List.of(0, 2));

        assertThat(firstAnswerCorrect(exam, List.of(0, 2))).isTrue();
        assertThat(firstAnswerCorrect(exam, List.of(2, 0))).isTrue();
        assertThat(firstAnswerCorrect(exam, List.of(0))).isFalse();
        assertThat(firstAnswerCorrect(exam, List.of(0, 1, 2))).isFalse();
        assertThat(firstAnswerCorrect(exam, List.of())).isFalse();
    }

    @Test
    @DisplayName("El alumno ve qué marcó; el profesor, además, cuáles eran las correctas")
    void result_showsChosenAndCorrectOptions() {
        String token = adminToken();
        PublishedExam exam = examWithFirstQuestionCorrect(token, List.of(0, 2));

        HttpResult result = submitFirst(exam, List.of(2, 1));

        Map<String, Object> answer = result.<List<Map<String, Object>>>read("$.answers").get(0);
        assertThat(answer).containsEntry("studentAnswers", List.of(1, 2)).containsEntry("correct", false);
        assertThat(answer.get("correctAnswers")).isNull();
        Map<String, Object> teacherAnswer = get("/api/results/exam/" + exam.id(), token).expectStatus(200)
                .asList().get(0).entrySet().stream().filter(e -> e.getKey().equals("answers"))
                .map(e -> ((List<Map<String, Object>>) e.getValue()).get(0)).findFirst().orElseThrow();
        assertThat(teacherAnswer).containsEntry("correctAnswers", List.of(0, 2));
    }

    @Test
    @DisplayName("Una opción marcada que la pregunta no tiene da 409")
    void submit_unknownOption_conflict() {
        PublishedExam exam = examWithFirstQuestionCorrect(adminToken(), List.of(0, 2));

        assertThat(submitFirstRaw(exam, List.of(0, 7)).expectStatus(409).message())
                .isEqualTo("La opción 7 no existe en la pregunta " + exam.questionIds().get(0));
    }

    // ───── Ayudas ─────

    private Map<String, Object> question(List<Integer> correctAnswers) {
        return obj("text", unique("Varias correctas"), "options", FOUR_OPTIONS, "correctAnswers", correctAnswers);
    }

    /** Examen de 5 preguntas, 3 intentos y sin límite de tiempo; la primera tiene las correctas indicadas. */
    private PublishedExam examWithFirstQuestionCorrect(String token, List<Integer> correctAnswers) {
        List<Long> ids = new ArrayList<>();
        ids.add(post("/api/questions", token, question(correctAnswers)).expectStatus(201).id());
        ids.addAll(createQuestions(token, 4));
        long examId = createDraft(token, 5, null, true);
        setQuestions(token, examId, ids);
        String code = post("/api/exams/" + examId + "/publish", token,
                obj("visibility", "PUBLIC", "unlimitedAttempts", true)).expectStatus(200).read("$.code");
        return new PublishedExam(examId, code, ids);
    }

    private boolean firstAnswerCorrect(PublishedExam exam, List<Integer> chosen) {
        return submitFirst(exam, chosen).<List<Map<String, Object>>>read("$.answers").get(0).get("correct") == Boolean.TRUE;
    }

    private HttpResult submitFirst(PublishedExam exam, List<Integer> chosen) {
        return submitFirstRaw(exam, chosen).expectStatus(201);
    }

    /** Marca chosen en la primera pregunta y deja el resto en blanco. */
    private HttpResult submitFirstRaw(PublishedExam exam, List<Integer> chosen) {
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < exam.questionIds().size(); i++) {
            answers.add(obj("questionId", exam.questionIds().get(i), "chosenOptions", i == 0 ? chosen : List.of()));
        }
        return submit(exam.code(), startAttempt(exam.code(), null), null, answers);
    }
}
