package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: realización de exámenes, entregas y resultados")
class ExamTakingCharacterizationTest extends ApiCharacterizationTest {

    private static final List<String> RESULT_FIELDS = List.of("id", "examId", "examTitle", "studentName",
            "studentClub", "studentEmail", "registered", "studentUserId", "studentKey", "scoreVisible", "answers", "correctAnswers", "totalQuestions", "score",
            "passed", "timeSpentSeconds", "completedAt", "autoSubmitted");
    private static final List<String> ANSWER_FIELDS = List.of("questionId", "studentAnswers", "correctAnswers", "correct");

    // ───── Portada e intento ─────

    @Test
    @DisplayName("Portada por código: pública, solo lo que necesita la portada; nada interno (DE-19)")
    void getByCode_accessible_returnsOnlyWhatThePageNeeds() {
        PublishedExam exam = publishedExam(adminToken(), 5, 10, true);

        HttpResult result = get("/api/exams/by-code/" + exam.code(), null).expectStatus(200);

        assertThat(result.asMap()).containsOnlyKeys("title", "code", "visibility", "accessMode", "config", "maxAttempts",
                "expiresAt");
        assertThat((Map<String, Object>) result.read("$.config")).containsOnlyKeys("numberOfQuestions", "timeLimitMinutes");
        assertThat((String) result.read("$.code")).isEqualTo(exam.code());
        assertThat((Integer) result.read("$.config.timeLimitMinutes")).isEqualTo(10);
        assertThat((Integer) result.read("$.config.numberOfQuestions")).isEqualTo(5);
    }

    @Test
    @DisplayName("Portada por código: inexistente 404; caducado por fecha o cerrado 409")
    void getByCode_unavailable() {
        String token = adminToken();
        PublishedExam closed = publishedExam(token, 5, null, true);
        post("/api/exams/" + closed.id() + "/close", token, null).expectStatus(200);
        PublishedExam expiring = publishedExam(token, 5, null, true);

        HttpResult unknown = get("/api/exams/by-code/EXM-00000000", null);
        HttpResult closedResult = get("/api/exams/by-code/" + closed.code(), null);
        clock.advance(Duration.ofMinutes(61));
        HttpResult expiredResult = get("/api/exams/by-code/" + expiring.code(), null);

        assertThat(unknown.expectStatus(404).message()).isEqualTo("No existe ningún examen con código EXM-00000000");
        assertThat(closedResult.expectStatus(409).message()).isEqualTo("Este examen no está disponible en este momento");
        assertThat(expiredResult.expectStatus(409).message()).isEqualTo("Este examen no está disponible en este momento");
    }

    @Test
    @DisplayName("Intento: preguntas sin solución ni explicación; sin aleatorizar, en orden y con índices originales")
    void startAttempt_returnsPublicQuestionsInOrder() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);

        HttpResult result = start(exam.code(), null, obj("studentName", unique("A"))).expectStatus(200);

        assertThat(result.asMap()).containsOnlyKeys("attemptToken", "questions", "attemptNumber", "maxAttempts", "answers",
                "secondsLeft");
        List<Map<String, Object>> questions = result.read("$.questions");
        assertThat(questions).extracting(q -> ((Number) q.get("id")).longValue())
                .containsExactlyElementsOf(exam.questionIds());
        assertThat(questions.get(0)).containsOnlyKeys("id", "text", "options", "multipleCorrect");
        assertThat((List<Map<String, Object>>) questions.get(0).get("options"))
                .extracting(o -> o.get("index"), o -> o.get("text"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(0, "A"), org.assertj.core.groups.Tuple.tuple(1, "B"),
                        org.assertj.core.groups.Tuple.tuple(2, "C"), org.assertj.core.groups.Tuple.tuple(3, "D"));
    }

    @Test
    @DisplayName("Intento: con aleatorización conserva el conjunto de preguntas y de opciones con su índice")
    void startAttempt_randomized_keepsSameContent() {
        String token = adminToken();
        List<Long> questionIds = createQuestions(token, 5);
        long examId = post("/api/exams/drafts", token, draftBody(unique("E"), 5, null, true, true, true))
                .expectStatus(201).id();
        setQuestions(token, examId, questionIds);
        String code = publish(token, examId, "PUBLIC", null, null).expectStatus(200).read("$.code");

        List<Map<String, Object>> questions = start(code, null, obj("studentName", unique("A")))
                .expectStatus(200).read("$.questions");

        assertThat(questions).extracting(q -> ((Number) q.get("id")).longValue())
                .containsExactlyInAnyOrderElementsOf(questionIds);
        for (Map<String, Object> question : questions) {
            assertThat((List<Map<String, Object>>) question.get("options"))
                    .allMatch(o -> FOUR_OPTIONS.get((Integer) o.get("index")).equals(o.get("text")));
        }
    }

    @Test
    @DisplayName("Intento: un examen solo para registrados rechaza al anónimo (409)")
    void startAttempt_registeredOnlyAnonymous_conflict() {
        String token = adminToken();
        long examId = createDraft(token, 5, null, true);
        setQuestions(token, examId, createQuestions(token, 5));
        String code = publish(token, examId, "PUBLIC", "REGISTERED_ONLY", null).expectStatus(200).read("$.code");

        HttpResult anonymous = start(code, null, obj("studentName", unique("A")));
        HttpResult registered = start(code, newStudent().token(), obj("studentName", "Yo"));

        assertThat(anonymous.expectStatus(409).message())
                .isEqualTo("Este examen requiere estar registrado e iniciar sesión");
        registered.expectStatus(200);
    }

    @Test
    @DisplayName("Oficial (privado): el profesor decide si hace falta cuenta (DE-06)")
    void official_teacherChoosesWhetherAnAccountIsNeeded() {
        String token = adminToken();
        long withAccount = createDraft(token, 5, null, true);
        setQuestions(token, withAccount, createQuestions(token, 5));
        String withAccountCode = publish(token, withAccount, "PRIVATE", "REGISTERED_ONLY", null).expectStatus(200).read("$.code");
        long open = createDraft(token, 5, null, true);
        setQuestions(token, open, createQuestions(token, 5));
        String openCode = publish(token, open, "PRIVATE", "OPEN", null).expectStatus(200).read("$.code");

        assertThat((String) get("/api/exams/by-code/" + withAccountCode, null).expectStatus(200).read("$.accessMode"))
                .isEqualTo("REGISTERED_ONLY");
        start(withAccountCode, null, obj("studentName", unique("A"))).expectStatus(409);
        start(withAccountCode, newStudent().token(), obj("studentName", "Yo")).expectStatus(200);
        start(openCode, null, obj("studentName", unique("A"))).expectStatus(200);
    }

    // ───── Entrega ─────

    @Test
    @DisplayName("Entrega con nota visible: corrige, calcula la nota y el tiempo desde el inicio del intento")
    void submit_showScore_fullResult() {
        PublishedExam exam = publishedExam(adminToken(), 5, 10, true);
        String attempt = startAttempt(exam.code(), null, "  Ana Pérez ");
        clock.advance(Duration.ofSeconds(150));
        List<Integer> chosen = new ArrayList<>(allCorrect(5));
        chosen.set(3, (chosen.get(3) + 1) % 4);
        chosen.set(4, null);

        HttpResult result = submit(exam.code(), attempt, null, answers(exam.questionIds(), chosen))
                .expectStatus(201);

        assertThat(result.asMap()).containsOnlyKeys(RESULT_FIELDS);
        assertThat((String) result.read("$.studentName")).isEqualTo("Ana Pérez");
        assertThat((Boolean) result.read("$.scoreVisible")).isTrue();
        assertThat((Integer) result.read("$.correctAnswers")).isEqualTo(3);
        assertThat((Integer) result.read("$.totalQuestions")).isEqualTo(5);
        assertThat((Integer) result.read("$.score")).isEqualTo(60);
        assertThat((Boolean) result.read("$.passed")).isTrue();
        assertThat((Integer) result.read("$.timeSpentSeconds")).isEqualTo(150);
        List<Map<String, Object>> answers = result.read("$.answers");
        assertThat(answers.get(0)).containsOnlyKeys(ANSWER_FIELDS);
        assertThat(answers).extracting(a -> a.get("correct")).containsExactly(true, true, true, false, false);
        assertThat((List<?>) answers.get(4).get("studentAnswers")).isEmpty();
    }

    @Test
    @DisplayName("Entrega con nota oculta: el alumno no ve nota, aciertos ni respuestas")
    void submit_hiddenScore_studentSeesNoScore() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, false);

        HttpResult result = submit(exam.code(), startAttempt(exam.code(), null, unique("Alumno")), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);

        assertThat((Boolean) result.read("$.scoreVisible")).isFalse();
        assertThat((Object) result.read("$.score")).isNull();
        assertThat((Object) result.read("$.passed")).isNull();
        assertThat((Object) result.read("$.correctAnswers")).isNull();
        assertThat((Object) result.read("$.answers")).isNull();
        assertThat((Integer) result.read("$.totalQuestions")).isEqualTo(5);
    }

    @Test
    @DisplayName("Entrega: token de intento inválido o de otro examen devuelve 409")
    void submit_invalidAttemptToken_conflict() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true);
        PublishedExam otherExam = publishedExam(token, 5, null, true);

        HttpResult garbage = submit(exam.code(), "no-es-un-token", null,
                answers(exam.questionIds(), allCorrect(5)));
        HttpResult fromOtherExam = submit(exam.code(), startAttempt(otherExam.code(), null, unique("A")), null,
                answers(exam.questionIds(), allCorrect(5)));
        HttpResult accessToken = submit(exam.code(), token, null,
                answers(exam.questionIds(), allCorrect(5)));

        String expected = "El intento no es válido o ha superado el tiempo límite";
        assertThat(garbage.expectStatus(409).message()).isEqualTo(expected);
        assertThat(fromOtherExam.expectStatus(409).message()).isEqualTo(expected);
        assertThat(accessToken.expectStatus(409).message()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Entrega: dentro del margen de 2 min se acepta; pasado el margen se rechaza")
    void submit_timeLimit_graceOfTwoMinutes() {
        PublishedExam exam = publishedExam(adminToken(), 5, 10, true);
        String onTime = startAttempt(exam.code(), null);
        String late = startAttempt(exam.code(), null);

        clock.advance(Duration.ofMinutes(11));
        HttpResult withinGrace = submit(exam.code(), onTime, null,
                answers(exam.questionIds(), allCorrect(5)));
        clock.advance(Duration.ofMinutes(1).plusSeconds(1));
        HttpResult afterGrace = submit(exam.code(), late, null,
                answers(exam.questionIds(), allCorrect(5)));

        assertThat((Integer) withinGrace.expectStatus(201).read("$.timeSpentSeconds")).isEqualTo(660);
        assertThat(afterGrace.expectStatus(409).message())
                .isEqualTo("El intento no es válido o ha superado el tiempo límite");
    }

    @Test
    @DisplayName("Cerrar a mano: quien ya había empezado sigue contestando y entrega; nadie empieza uno nuevo (DE-07)")
    void close_attemptsInProgressCanFinish() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, 30, true);
        String attempt = startAttempt(exam.code(), null, unique("A"));

        clock.advance(Duration.ofMinutes(5));
        post("/api/exams/" + exam.id() + "/close", token, null).expectStatus(200);

        put("/api/exams/by-code/" + exam.code() + "/attempts/answers", null, obj("attemptToken", attempt,
                "questionId", exam.questionIds().get(0), "chosenOptions", List.of(0))).expectStatus(204);
        assertThat(start(exam.code(), null, obj("studentName", unique("B"))).expectStatus(409).message())
                .isEqualTo("Este examen no está disponible en este momento");
        submit(exam.code(), attempt, null, answers(exam.questionIds(), allCorrect(5))).expectStatus(201);
    }

    @Test
    @DisplayName("Cerrar a mano: el intento empezado antes vale hasta que se le acaba su tiempo, no más")
    void close_attemptInProgressStillHasItsTimeLimit() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, 30, true);
        String attempt = startAttempt(exam.code(), null, unique("A"));
        post("/api/exams/" + exam.id() + "/close", token, null).expectStatus(200);

        clock.advance(Duration.ofMinutes(40));

        assertThat(submit(exam.code(), attempt, null, answers(exam.questionIds(), allCorrect(5))).expectStatus(409).message())
                .isEqualTo("El intento no es válido o ha superado el tiempo límite");
    }

    @Test
    @DisplayName("Entrega: si el examen caduca por fecha durante el intento, se acepta")
    void submit_examExpiresDuringAttempt() {
        String token = adminToken();
        PublishedExam expiring = publishedExam(token, 5, null, true);
        String expiringAttempt = startAttempt(expiring.code(), null);
        patch("/api/exams/" + expiring.id() + "/expiration", token, obj("newExpiresAt", now().plusMinutes(10).toString()))
                .expectStatus(200);

        clock.advance(Duration.ofMinutes(70));

        submit(expiring.code(), expiringAttempt, null, answers(expiring.questionIds(), allCorrect(5))).expectStatus(201);
    }

    @Test
    @DisplayName("Entrega: respuestas que no cubren exactamente las preguntas, o repetidas, devuelven 409")
    void submit_answersMismatch_conflict() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);
        List<Map<String, Object>> missingOne = answers(exam.questionIds().subList(0, 4), allCorrect(4));
        List<Map<String, Object>> duplicated = new ArrayList<>(answers(exam.questionIds(), allCorrect(5)));
        duplicated.add(obj("questionId", exam.questionIds().get(0), "chosenOptions", List.of(0)));

        HttpResult incomplete = submit(exam.code(), startAttempt(exam.code(), null, unique("A")), null, missingOne);
        HttpResult repeated = submit(exam.code(), startAttempt(exam.code(), null, unique("B")), null, duplicated);

        assertThat(incomplete.expectStatus(409).message())
                .isEqualTo("Las respuestas no coinciden con las preguntas del examen");
        assertThat(repeated.expectStatus(409).message())
                .isEqualTo("Hay respuestas duplicadas para la pregunta " + exam.questionIds().get(0));
    }

    @Test
    @DisplayName("Entrega: una opción negativa da 400; una que la pregunta no tiene, 409")
    void submit_invalidOption() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);

        HttpResult negative = submit(exam.code(), startAttempt(exam.code(), null), null,
                answers(exam.questionIds(), Arrays.asList(-1, 0, 0, 0, 0)));
        HttpResult missing = submit(exam.code(), startAttempt(exam.code(), null), null,
                answers(exam.questionIds(), Arrays.asList(4, 0, 0, 0, 0)));

        assertThat(negative.expectStatus(400).message()).startsWith("Opciones marcadas: ");
        assertThat(missing.expectStatus(409).message())
                .isEqualTo("La opción 4 no existe en la pregunta " + exam.questionIds().get(0));
    }

    @Test
    @DisplayName("Entrega: la corrección usa la respuesta correcta del momento; editar la pregunta después no la cambia")
    void submit_storesCorrectAnswerAtSubmissionTime() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true);
        submit(exam.code(), startAttempt(exam.code(), null, unique("A")), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);
        long firstQuestion = exam.questionIds().get(0);
        put("/api/questions/" + firstQuestion, token, obj("text", "Editada", "options", FOUR_OPTIONS,
                "correctAnswers", List.of(3), "grade", "VERDE")).expectStatus(200);

        List<Map<String, Object>> results = get("/api/results/exam/" + exam.id(), token).expectStatus(200).asList();

        assertThat(results.get(0).get("score")).isEqualTo(100);
    }

    // ───── Consultas de resultados ─────

    @Test
    @DisplayName("Profesor: lista de entregas (la más reciente primero) y estadísticas")
    void teacherResults_listAndStatistics() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, false);
        List<Integer> threeRight = new ArrayList<>(allCorrect(5));
        threeRight.set(0, null);
        threeRight.set(1, null);
        submit(exam.code(), startAttempt(exam.code(), null, "Primera"), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);
        clock.advance(Duration.ofSeconds(1));
        submit(exam.code(), startAttempt(exam.code(), null, "Segunda"), null,
                answers(exam.questionIds(), threeRight)).expectStatus(201);

        List<Map<String, Object>> results = get("/api/results/exam/" + exam.id(), teacher.token())
                .expectStatus(200).asList();
        HttpResult stats = get("/api/results/exam/" + exam.id() + "/statistics", teacher.token()).expectStatus(200);

        assertThat(results).extracting(r -> r.get("studentName")).containsExactly("Segunda", "Primera");
        assertThat(results).allMatch(r -> Boolean.TRUE.equals(r.get("scoreVisible")));
        assertThat(stats.asMap()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "examId", (int) exam.id(), "totalAttempts", 2, "totalStudents", 2, "averageScore", 80.0,
                "passedCount", 2, "failedCount", 0, "highestScore", 100, "lowestScore", 60, "passingScore", 50));
    }

    @Test
    @DisplayName("Profesor: cada entrega dice si la hizo un usuario registrado o un anónimo")
    void teacherResults_registeredFlag() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);
        Session student = newStudent();
        submit(exam.code(), startAttempt(exam.code(), student.token(), "Registrada"), student.token(),
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);
        submit(exam.code(), startAttempt(exam.code(), null, "Anónima"), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201);

        List<Map<String, Object>> results = get("/api/results/exam/" + exam.id(), teacher.token())
                .expectStatus(200).asList();

        assertThat(results).extracting(r -> r.get("studentName"), r -> r.get("registered"))
                .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("Registrada", true),
                        org.assertj.core.groups.Tuple.tuple("Anónima", false));
    }

    @Test
    @DisplayName("Profesor: estadísticas de un examen sin entregas son ceros")
    void teacherStatistics_noResults_zeros() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);

        HttpResult stats = get("/api/results/exam/" + exam.id() + "/statistics", teacher.token()).expectStatus(200);

        assertThat(stats.asMap()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "examId", (int) exam.id(), "totalAttempts", 0, "totalStudents", 0, "averageScore", 0.0,
                "passedCount", 0, "failedCount", 0, "highestScore", 0, "lowestScore", 0, "passingScore", 50));
    }

    @Test
    @DisplayName("Profesor: una entrega concreta solo la ve el dueño del examen")
    void teacherResult_onlyOwner() {
        Session owner = newTeacher();
        Session other = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        long resultId = submit(exam.code(), startAttempt(exam.code(), null, unique("A")), null,
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201).id();

        HttpResult asOwner = get("/api/results/" + resultId, owner.token()).expectStatus(200);
        HttpResult asOther = get("/api/results/" + resultId, other.token());
        HttpResult otherList = get("/api/results/exam/" + exam.id(), other.token());

        assertThat((String) asOwner.read("$.examTitle")).isNotBlank();
        assertThat(asOther.expectStatus(404).message()).isEqualTo("No existe un resultado con id " + resultId);
        assertThat(otherList.expectStatus(404).message()).isEqualTo("No existe un examen con id " + exam.id());
    }

    @Test
    @DisplayName("Alumno: su historial respeta la nota oculta de cada examen e incluye el título")
    void studentHistory_respectsShowScore() {
        String token = adminToken();
        Session student = newStudent();
        PublishedExam visible = publishedExam(token, 5, null, true);
        PublishedExam hidden = publishedExam(token, 5, null, false);
        submit(visible.code(), startAttempt(visible.code(), student.token(), "Yo"), student.token(),
                answers(visible.questionIds(), allCorrect(5))).expectStatus(201);
        clock.advance(Duration.ofSeconds(1));
        submit(hidden.code(), startAttempt(hidden.code(), student.token(), "Yo"), student.token(),
                answers(hidden.questionIds(), allCorrect(5))).expectStatus(201);

        List<Map<String, Object>> mine = get("/api/results/me", student.token()).expectStatus(200).asList();

        assertThat(mine).hasSize(2);
        assertThat(mine).extracting(r -> ((Number) r.get("examId")).longValue())
                .containsExactly(hidden.id(), visible.id());
        assertThat(mine.get(0).get("scoreVisible")).isEqualTo(false);
        assertThat(mine.get(0).get("score")).isNull();
        assertThat(mine.get(1).get("score")).isEqualTo(100);
        assertThat(mine).allMatch(r -> r.get("examTitle") != null);
    }
}
