package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * DE-03: al publicar, el examen guarda una copia de sus preguntas. Editar el banco después
 * no cambia lo que ven los alumnos ni cómo se corrige.
 */
@DisplayName("Caracterización: copia de las preguntas al publicar (DE-03)")
class PublishedQuestionsCharacterizationTest extends ApiCharacterizationTest {

    private static final List<String> EDITED_OPTIONS = List.of("W", "X", "Y", "Z", "Nueva");

    @Test
    @DisplayName("Editar una pregunta después de publicar no cambia lo que ve el alumno")
    void editAfterPublish_studentSeesPublishedVersion() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true);
        long first = exam.questionIds().get(0);
        String originalText = questionText(token, first);

        editQuestion(token, first, "Editada después de publicar", 4);

        Map<String, Object> shown = attemptQuestion(exam.code(), first);
        assertThat(shown.get("text")).isEqualTo(originalText);
        assertThat(optionTexts(shown)).containsExactlyElementsOf(FOUR_OPTIONS);
    }

    @Test
    @DisplayName("La corrección usa la solución publicada, aunque se cambie en el banco antes de entregar")
    void editBeforeSubmit_correctedAgainstPublishedVersion() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true);
        String attempt = startAttempt(exam.code(), null);
        exam.questionIds().forEach(id -> editQuestion(token, id, "Otra pregunta", 3));

        HttpResult result = submit(exam.code(), attempt, unique("A"), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);

        assertThat((Integer) result.read("$.score")).isEqualTo(100);
    }

    @Test
    @DisplayName("El profesor consulta las preguntas tal como se publicaron, con solución y explicación")
    void teacher_readsPublishedQuestions() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, false);
        long first = exam.questionIds().get(0);
        String originalText = questionText(teacher.token(), first);
        editQuestion(teacher.token(), first, "Editada", 4);

        List<Map<String, Object>> published = get("/api/exams/" + exam.id() + "/published-questions", teacher.token())
                .expectStatus(200).asList();

        assertThat(published).extracting(q -> ((Number) q.get("questionId")).longValue())
                .containsExactlyElementsOf(exam.questionIds());
        assertThat(published.get(0)).containsEntry("text", originalText)
                .containsEntry("options", FOUR_OPTIONS)
                .containsEntry("correctAnswer", 0)
                .containsEntry("explanation", "Explicación de " + originalText)
                .containsEntry("editedSincePublished", true);
        assertThat(published.get(1)).containsEntry("editedSincePublished", false);
    }

    @Test
    @DisplayName("Un borrador aún no tiene preguntas publicadas (409); un examen ajeno no existe (404)")
    void publishedQuestions_rules() {
        Session teacher = newTeacher();
        long draft = createDraft(teacher.token(), 5, null, true);
        PublishedExam foreign = publishedExam(adminToken(), 5, null, true);

        assertThat(get("/api/exams/" + draft + "/published-questions", teacher.token()).expectStatus(409).message())
                .isEqualTo("El examen todavía no está publicado");
        get("/api/exams/" + foreign.id() + "/published-questions", teacher.token()).expectStatus(404);
    }

    // ───── Ayudas ─────

    private String questionText(String token, long questionId) {
        return get("/api/questions/" + questionId, token).expectStatus(200).read("$.text");
    }

    private void editQuestion(String token, long questionId, String text, int correctAnswer) {
        put("/api/questions/" + questionId, token, obj("text", text, "options", EDITED_OPTIONS,
                "correctAnswer", correctAnswer, "grade", "VERDE")).expectStatus(200);
    }

    private Map<String, Object> attemptQuestion(String code, long questionId) {
        List<Map<String, Object>> questions = post("/api/exams/by-code/" + code + "/attempts", null, null)
                .expectStatus(200).read("$.questions");
        return questions.stream()
                .filter(q -> ((Number) q.get("id")).longValue() == questionId)
                .findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static List<String> optionTexts(Map<String, Object> question) {
        return ((List<Map<String, Object>>) question.get("options")).stream()
                .sorted((a, b) -> Integer.compare(((Number) a.get("index")).intValue(), ((Number) b.get("index")).intValue()))
                .map(o -> (String) o.get("text"))
                .toList();
    }
}
