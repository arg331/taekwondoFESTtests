package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * DE-13 (decidido el 31/10/2026): el aprobado es del 50 %, pero el profesor lo elige en cada examen.
 */
@DisplayName("Caracterización: aprobado configurable por examen")
class PassingScoreCharacterizationTest extends ApiCharacterizationTest {

    @Test
    @DisplayName("Sin indicarlo, el aprobado es el 50 %: 5 de 10 aprueba y 4 de 10 suspende")
    void defaultPassingScore_isFifty() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 10, null, true, 2);

        assertThat((Integer) get("/api/exams/" + exam.id(), teacher.token()).read("$.config.passingScore")).isEqualTo(50);
        HttpResult five = finish(exam, "Ana", 5);
        HttpResult four = finish(exam, "Luis", 4);

        assertThat((Integer) five.read("$.score")).isEqualTo(50);
        assertThat((Boolean) five.read("$.passed")).isTrue();
        assertThat((Integer) four.read("$.score")).isEqualTo(40);
        assertThat((Boolean) four.read("$.passed")).isFalse();
    }

    @Test
    @DisplayName("Con el aprobado en 70: 6 de 10 suspende para el alumno, en los resultados y en las estadísticas")
    void customPassingScore_appliesEverywhere() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExamWithPassingScore(teacher.token(), 70);

        HttpResult six = finish(exam, "Ana", 6);
        finish(exam, "Luis", 7);

        assertThat((Boolean) six.read("$.passed")).isFalse();
        List<Map<String, Object>> results = get("/api/results/exam/" + exam.id(), teacher.token()).expectStatus(200).asList();
        assertThat(results).extracting(r -> r.get("studentName") + ":" + r.get("passed"))
                .containsExactlyInAnyOrder("Ana:false", "Luis:true");
        HttpResult stats = get("/api/results/exam/" + exam.id() + "/statistics", teacher.token()).expectStatus(200);
        assertThat(stats.asMap()).containsEntry("passedCount", 1).containsEntry("failedCount", 1)
                .containsEntry("passingScore", 70);
    }

    @Test
    @DisplayName("Se elige al crear el borrador y al cambiar su configuración; la copia lo conserva")
    void passingScore_setOnDraftAndConfig() {
        Session teacher = newTeacher();
        Map<String, Object> body = new HashMap<>(draftBody(unique("Examen"), 5, null, true, false, false));
        body.put("passingScore", 80);
        long examId = post("/api/exams/drafts", teacher.token(), body).expectStatus(201).id();
        assertThat((Integer) get("/api/exams/" + examId, teacher.token()).read("$.config.passingScore")).isEqualTo(80);

        Map<String, Object> config = obj("numberOfQuestions", 5, "timeLimitMinutes", null, "showScore", true,
                "randomizeOptions", false, "randomizeQuestionOrder", false, "passingScore", 65);
        assertThat((Integer) patch("/api/exams/" + examId + "/config", teacher.token(), config).expectStatus(200)
                .read("$.config.passingScore")).isEqualTo(65);

        long copyId = post("/api/exams/" + examId + "/copy", teacher.token(), null).expectStatus(201).id();
        assertThat((Integer) get("/api/exams/" + copyId, teacher.token()).read("$.config.passingScore")).isEqualTo(65);
    }

    @Test
    @DisplayName("Fuera de 1 a 100 se rechaza (400)")
    void passingScore_outOfRange() {
        Session teacher = newTeacher();
        for (int invalid : new int[]{0, 101}) {
            Map<String, Object> body = new HashMap<>(draftBody(unique("Examen"), 5, null, true, false, false));
            body.put("passingScore", invalid);
            post("/api/exams/drafts", teacher.token(), body).expectStatus(400);
        }
    }

    /** Examen de 10 preguntas con ese aprobado, publicado como los de publishedExam. */
    private PublishedExam publishedExamWithPassingScore(String token, int passingScore) {
        List<Long> questionIds = createQuestions(token, 10);
        Map<String, Object> body = new HashMap<>(draftBody(unique("Examen"), 10, null, true, false, false));
        body.put("passingScore", passingScore);
        long examId = post("/api/exams/drafts", token, body).expectStatus(201).id();
        setQuestions(token, examId, questionIds);
        String code = post("/api/exams/" + examId + "/publish", token, obj("visibility", "PUBLIC",
                "expiresAt", now().plusHours(1).toString(), "maxAttempts", 1)).expectStatus(200).read("$.code");
        return new PublishedExam(examId, code, questionIds);
    }

    /** Entrega con las primeras «right» bien y el resto en blanco. */
    private HttpResult finish(PublishedExam exam, String student, int right) {
        List<Integer> chosen = new ArrayList<>(allCorrect(exam.questionIds().size()));
        for (int i = right; i < chosen.size(); i++) {
            chosen.set(i, null);
        }
        return submit(exam.code(), startAttempt(exam.code(), null, student), null,
                answers(exam.questionIds(), chosen)).expectStatus(201);
    }
}
