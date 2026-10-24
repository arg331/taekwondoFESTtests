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
 * RF-54: los resultados se agrupan por alumno y las estadísticas usan su mejor nota. Un anónimo se
 * reconoce por su nombre sin distinguir mayúsculas, tildes ni espacios de más (decisión del 18/10/2026).
 */
@DisplayName("Caracterización: alumnos y su mejor nota (RF-54)")
class StudentGroupingCharacterizationTest extends ApiCharacterizationTest {

    @Test
    @DisplayName("Anónimo: «José  Pérez» y «jose perez» son el mismo alumno al contar intentos")
    void anonymousName_ignoresCaseAccentsAndSpaces() {
        PublishedExam exam = publishedExam(adminToken(), 5, 10, true);
        finish(exam, startAttempt(exam.code(), null, "José  Pérez"), allCorrect(5));

        assertThat(start(exam.code(), null, obj("studentName", "jose perez")).expectStatus(409).message())
                .isEqualTo("Ya has hecho todos los intentos de este examen");
        start(exam.code(), null, obj("studentName", "José Pérez García")).expectStatus(200);
    }

    @Test
    @DisplayName("Cada entrega dice de qué alumno es: los anónimos por su nombre, los registrados por su cuenta")
    void studentKey_groupsDeliveries() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true, 3);
        Session student = newStudent();
        finish(exam, startAttempt(exam.code(), null, "Ana Gómez"), allCorrect(5));
        finish(exam, startAttempt(exam.code(), null, "ana gomez"), allCorrect(5));
        finish(exam, startAttempt(exam.code(), student.token(), "Ana Gómez"), allCorrect(5), student.token());

        List<Map<String, Object>> results = get("/api/results/exam/" + exam.id(), token).expectStatus(200).asList();

        List<Object> anonymousKeys = results.stream().filter(r -> !(Boolean) r.get("registered"))
                .map(r -> r.get("studentKey")).distinct().toList();
        Object registeredKey = results.stream().filter(r -> (Boolean) r.get("registered"))
                .map(r -> r.get("studentKey")).findFirst().orElseThrow();
        assertThat(anonymousKeys).hasSize(1);
        assertThat(registeredKey).isNotEqualTo(anonymousKeys.get(0));
    }

    @Test
    @DisplayName("Estadísticas: cuentan alumnos con su mejor nota, y aparte el total de entregas")
    void statistics_useBestScorePerStudent() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true, 3);
        finish(exam, startAttempt(exam.code(), null, "Luis"), rightAnswers(2));
        finish(exam, startAttempt(exam.code(), null, "LUIS"), allCorrect(5));
        finish(exam, startAttempt(exam.code(), null, "Marta"), rightAnswers(3));

        HttpResult stats = get("/api/results/exam/" + exam.id() + "/statistics", token).expectStatus(200);

        assertThat(stats.asMap()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "examId", (int) exam.id(), "totalAttempts", 3, "totalStudents", 2, "averageScore", 80.0,
                "passedCount", 1, "failedCount", 1, "highestScore", 100, "lowestScore", 60));
    }

    // ───── Ayudas ─────

    /** Acierta las n primeras y deja el resto en blanco. */
    private static List<Integer> rightAnswers(int n) {
        List<Integer> chosen = new ArrayList<>(allCorrect(5));
        for (int i = n; i < 5; i++) chosen.set(i, null);
        return chosen;
    }

    private void finish(PublishedExam exam, String attemptToken, List<Integer> chosen) {
        finish(exam, attemptToken, chosen, null);
    }

    private void finish(PublishedExam exam, String attemptToken, List<Integer> chosen, String token) {
        submit(exam.code(), attemptToken, token, answers(exam.questionIds(), chosen)).expectStatus(201);
    }
}
