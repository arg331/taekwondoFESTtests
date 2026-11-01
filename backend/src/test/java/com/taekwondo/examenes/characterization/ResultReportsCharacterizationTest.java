package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-28: acierto por pregunta, para detectar preguntas mal planteadas.
 * RF-55: los resultados de un usuario; el profesor ve los de sus exámenes y el administrador, todos.
 */
@DisplayName("Caracterización: acierto por pregunta y resultados de un usuario")
class ResultReportsCharacterizationTest extends ApiCharacterizationTest {

    private static final List<String> QUESTION_STATS_FIELDS = List.of("questionId", "text", "options",
            "correctAnswers", "submissions", "correct", "blank", "optionCounts", "successRate");

    // ───── Acierto por pregunta (RF-28) ─────

    @Test
    @DisplayName("Cada entrega cuenta: por pregunta, aciertos, en blanco, cuántos marcaron cada opción y el % de acierto")
    void questionStats_countsEverySubmission() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true, 3);
        List<Long> ids = exam.questionIds();
        // createQuestions(): la correcta de la pregunta i es i % 4
        submit(exam.code(), startAttempt(exam.code(), null, unique("A")), null, answers(ids, allCorrect(5)))
                .expectStatus(201);
        submit(exam.code(), startAttempt(exam.code(), null, unique("B")), null,
                answers(ids, Arrays.asList(1, 1, null, null, null))).expectStatus(201);
        submit(exam.code(), startAttempt(exam.code(), null, unique("C")), null,
                answers(ids, Arrays.asList(0, null, 0, null, null))).expectStatus(201);

        List<Map<String, Object>> stats = questionStats(exam.id(), token).expectStatus(200).asList();

        assertThat(stats).extracting(s -> ((Number) s.get("questionId")).longValue()).containsExactlyElementsOf(ids);
        assertThat(stats.get(0)).containsOnlyKeys(QUESTION_STATS_FIELDS)
                .containsEntry("options", FOUR_OPTIONS)
                .containsEntry("correctAnswers", List.of(0))
                .containsEntry("submissions", 3)
                .containsEntry("correct", 2)
                .containsEntry("blank", 0)
                .containsEntry("optionCounts", List.of(2, 1, 0, 0))
                .containsEntry("successRate", 67);
        assertThat(stats.get(1)).containsEntry("correct", 2).containsEntry("blank", 1)
                .containsEntry("optionCounts", List.of(0, 2, 0, 0));
        assertThat(stats.get(2)).containsEntry("correct", 1).containsEntry("blank", 1)
                .containsEntry("optionCounts", List.of(1, 0, 1, 0)).containsEntry("successRate", 33);
    }

    @Test
    @DisplayName("Sin entregas, cada pregunta sale con 0 y sin porcentaje")
    void questionStats_noSubmissions() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true);

        List<Map<String, Object>> stats = questionStats(exam.id(), token).expectStatus(200).asList();

        assertThat(stats).hasSize(5);
        assertThat(stats.get(0)).containsEntry("submissions", 0)
                .containsEntry("optionCounts", List.of(0, 0, 0, 0))
                .containsEntry("successRate", null);
    }

    @Test
    @DisplayName("Solo el dueño del examen (404), y no de un borrador (409)")
    void questionStats_rules() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);
        long draftId = createDraft(teacher.token(), 5, null, true);

        questionStats(exam.id(), teacher.token()).expectStatus(404);
        assertThat(questionStats(draftId, teacher.token()).expectStatus(409).message())
                .isEqualTo("El examen todavía no está publicado");
    }

    // ───── Resultados de un usuario (RF-55) ─────

    @Test
    @DisplayName("El profesor ve los resultados del usuario en sus exámenes; el administrador, en todos")
    void userResults_scopedByOwner() {
        Session teacher = newTeacher();
        Session student = newStudent();
        PublishedExam mine = publishedExam(teacher.token(), 5, null, true);
        PublishedExam other = publishedExam(adminToken(), 5, null, true);
        submit(mine.code(), startAttempt(mine.code(), student.token(), "Yo"), student.token(),
                answers(mine.questionIds(), allCorrect(5))).expectStatus(201);
        submit(other.code(), startAttempt(other.code(), student.token(), "Yo"), student.token(),
                answers(other.questionIds(), allCorrect(5))).expectStatus(201);

        HttpResult forTeacher = userResults(student.id(), teacher.token()).expectStatus(200);
        HttpResult forAdmin = userResults(student.id(), adminToken()).expectStatus(200);

        assertThat(forTeacher.asMap()).containsOnlyKeys("userId", "username", "displayName", "clubName", "results");
        assertThat((String) forTeacher.read("$.username")).isEqualTo(student.username());
        assertThat((String) forTeacher.read("$.displayName")).isEqualTo("Alumno " + student.username());
        assertThat((List<Integer>) forTeacher.read("$.results[*].examId")).containsExactly((int) mine.id());
        assertThat((List<Integer>) forAdmin.read("$.results[*].examId"))
                .containsExactlyInAnyOrder((int) mine.id(), (int) other.id());
        // El profesor lo ve todo de sus exámenes, también las respuestas correctas
        assertThat((Integer) forTeacher.read("$.results[0].score")).isEqualTo(100);
        assertThat((Integer) forTeacher.read("$.results[0].studentUserId")).isEqualTo((int) student.id());
    }

    @Test
    @DisplayName("Usuario inexistente, 404; un alumno no puede consultarlo, 403")
    void userResults_rules() {
        Session student = newStudent();

        userResults(999_999L, adminToken()).expectStatus(404);
        userResults(student.id(), student.token()).expectStatus(403);
    }

    @Test
    @DisplayName("Sin entregas en los exámenes del profesor: 404, como si no existiera (DE-23); el administrador lo ve vacío")
    void userResults_noVisibleResults_notFound() {
        Session teacher = newTeacher();
        Session student = newStudent();
        long adminId = get("/api/auth/me", adminToken()).expectStatus(200).id();

        HttpResult forTeacher = userResults(student.id(), teacher.token());
        HttpResult adminAccount = userResults(adminId, teacher.token());
        HttpResult forAdmin = userResults(student.id(), adminToken()).expectStatus(200);

        assertThat(forTeacher.expectStatus(404).message()).isEqualTo("No existe un usuario con id " + student.id());
        assertThat(adminAccount.expectStatus(404).message()).isEqualTo("No existe un usuario con id " + adminId);
        assertThat((List<?>) forAdmin.read("$.results")).isEmpty();
        assertThat((Object) forAdmin.read("$.clubName")).isNull();
    }

    // ───── Ayudas ─────

    private HttpResult questionStats(long examId, String token) {
        return get("/api/results/exam/" + examId + "/questions", token);
    }

    private HttpResult userResults(long userId, String token) {
        return get("/api/results/user/" + userId, token);
    }
}
