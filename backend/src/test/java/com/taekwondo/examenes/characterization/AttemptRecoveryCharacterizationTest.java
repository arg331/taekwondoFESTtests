package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import com.taekwondo.examenes.result.ResultService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-24 y DE-05: el intento guarda en el servidor las preguntas que vio el alumno y lo que va
 * contestando, así que recargar la página o perder la conexión no borra el examen ni reinicia el
 * reloj. Un intento que caduca sin entregar se corrige solo con lo guardado.
 */
@DisplayName("Caracterización: recuperar el intento y entrega automática")
class AttemptRecoveryCharacterizationTest extends ApiCharacterizationTest {

    private static final String INVALID = "El intento no es válido o ha superado el tiempo límite";
    private static final String SUBMITTED = "Este intento ya se ha entregado";
    private static final String OPEN_WITH_NAME = "Ya hay un intento en curso con ese nombre. Si es tuyo, sigue en el "
            + "navegador donde lo empezaste; si no, escribe tu nombre completo.";

    @Autowired
    ResultService resultService;

    // ───── Guardar y recuperar ─────

    @Test
    @DisplayName("Las respuestas se guardan al contestar; al recuperar el intento vuelven, con preguntas y opciones en el mismo orden")
    void resume_restoresLayoutAndAnswers() {
        String token = adminToken();
        List<Long> ids = createQuestions(token, 5);
        long examId = post("/api/exams/drafts", token, draftBody(unique("Barajado"), 5, null, true, true, true))
                .expectStatus(201).id();
        setQuestions(token, examId, ids);
        String code = publish(token, examId, "PUBLIC", null, null).expectStatus(200).read("$.code");
        HttpResult started = start(code, null, obj("studentName", unique("Ana"))).expectStatus(200);
        String attempt = started.read("$.attemptToken");
        long first = started.readLong("$.questions[0].id");
        long second = started.readLong("$.questions[1].id");
        long third = started.readLong("$.questions[2].id");

        saveAnswer(code, attempt, first, List.of(2)).expectStatus(204);
        saveAnswer(code, attempt, second, List.of(1)).expectStatus(204);
        // Cambiar de idea sustituye lo guardado
        saveAnswer(code, attempt, second, List.of(3)).expectStatus(204);
        HttpResult resumed = resume(code, attempt).expectStatus(200);

        assertThat((Object) resumed.read("$.questions")).isEqualTo(started.read("$.questions"));
        assertThat((Integer) resumed.read("$.attemptNumber")).isEqualTo(1);
        assertThat(saved(resumed, first)).containsExactly(2);
        assertThat(saved(resumed, second)).containsExactly(3);
        assertThat(saved(resumed, third)).isEmpty();
    }

    @Test
    @DisplayName("El reloj no vuelve a empezar: al recuperar, secondsLeft descuenta lo que ha pasado; sin límite, null")
    void resume_keepsClock() {
        PublishedExam timed = publishedExam(adminToken(), 5, 10, true);
        PublishedExam untimed = publishedExam(adminToken(), 5, null, true);
        HttpResult started = start(timed.code(), null, obj("studentName", unique("Ana"))).expectStatus(200);
        String free = startAttempt(untimed.code(), null, unique("Eva"));

        clock.advance(Duration.ofMinutes(4));

        assertThat((Integer) started.read("$.secondsLeft")).isEqualTo(600);
        assertThat((Integer) resume(timed.code(), started.read("$.attemptToken")).expectStatus(200)
                .read("$.secondsLeft")).isEqualTo(360);
        assertThat((Object) resume(untimed.code(), free).expectStatus(200).read("$.secondsLeft")).isNull();
    }

    @Test
    @DisplayName("El registrado que vuelve a empezar con un intento abierto lo recupera; sin sesión, el nombre no basta (DE-21)")
    void startAgain_resumesOpenAttempt() {
        String admin = adminToken();
        PublishedExam exam = publishedExam(admin, 5, 10, true, 3);
        Session student = newStudent();
        String name = unique("Luis Pérez");
        String mine = startAttempt(exam.code(), student.token(), "Yo");
        String anonymous = startAttempt(exam.code(), null, name);
        long question = exam.questionIds().get(0);
        saveAnswer(exam.code(), mine, question, List.of(1)).expectStatus(204);
        saveAnswer(exam.code(), anonymous, question, List.of(2)).expectStatus(204);
        clock.advance(Duration.ofMinutes(3));

        HttpResult again = start(exam.code(), student.token(), obj("studentName", "Otro nombre")).expectStatus(200);
        HttpResult sameName = start(exam.code(), null, obj("studentName", "  " + name.toUpperCase() + " "));

        assertThat((Integer) again.read("$.attemptNumber")).isEqualTo(1);
        assertThat(saved(again, question)).containsExactly(1);
        assertThat((Integer) again.read("$.secondsLeft")).isEqualTo(420);
        // Sin sesión, quien escribe el mismo nombre no se lleva el token ni las respuestas del otro (DE-21)
        assertThat(sameName.expectStatus(409).message()).isEqualTo(OPEN_WITH_NAME);
        // El dueño sigue con su intento, que solo se recupera con su token
        HttpResult owner = resume(exam.code(), anonymous).expectStatus(200);
        assertThat((Integer) owner.read("$.attemptNumber")).isEqualTo(1);
        assertThat(saved(owner, question)).containsExactly(2);
        // Es el mismo intento, con los datos del principio: al entregarlo, el siguiente es el 2
        assertThat((String) submit(exam.code(), again.read("$.attemptToken"), student.token(),
                answers(exam.questionIds(), allCorrect(5))).expectStatus(201).read("$.studentName")).isEqualTo("Yo");
        assertThat((Integer) start(exam.code(), student.token(), obj("studentName", "Yo")).expectStatus(200)
                .read("$.attemptNumber")).isEqualTo(2);
    }

    @Test
    @DisplayName("Guardar: opción inexistente o pregunta ajena, 409; intento entregado, caducado o token falso, 409")
    void saveAnswer_rules() {
        PublishedExam exam = publishedExam(adminToken(), 5, 10, true, 3);
        String attempt = startAttempt(exam.code(), null, unique("Ana"));
        long question = exam.questionIds().get(0);

        assertThat(saveAnswer(exam.code(), attempt, question, List.of(4)).expectStatus(409).message())
                .isEqualTo("La opción 4 no existe en la pregunta " + question);
        assertThat(saveAnswer(exam.code(), attempt, 999_999L, List.of(0)).expectStatus(409).message())
                .isEqualTo("La pregunta 999999 no es de este examen");
        assertThat(saveAnswer(exam.code(), "no-es-un-token", question, List.of(0)).expectStatus(409).message())
                .isEqualTo(INVALID);

        submit(exam.code(), attempt, null, answers(exam.questionIds(), allCorrect(5))).expectStatus(201);
        assertThat(saveAnswer(exam.code(), attempt, question, List.of(0)).expectStatus(409).message())
                .isEqualTo(SUBMITTED);
        assertThat(resume(exam.code(), attempt).expectStatus(409).message()).isEqualTo(SUBMITTED);

        String late = startAttempt(exam.code(), null, unique("Eva"));
        clock.advance(Duration.ofMinutes(13));
        assertThat(saveAnswer(exam.code(), late, question, List.of(0)).expectStatus(409).message()).isEqualTo(INVALID);
        assertThat(resume(exam.code(), late).expectStatus(409).message()).isEqualTo(INVALID);
    }

    // ───── Entrega automática al caducar ─────

    @Test
    @DisplayName("Un intento que caduca sin entregar se corrige solo con lo guardado y se marca como entrega automática")
    void expiredAttempt_autoSubmitted() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, 10, true, 2);
        List<Long> ids = exam.questionIds();
        String name = unique("Ana");
        LocalDateTime startedAt = now();
        String attempt = startAttempt(exam.code(), null, name);
        // createQuestions(): la correcta de la pregunta i es i % 4
        saveAnswer(exam.code(), attempt, ids.get(0), List.of(0)).expectStatus(204);
        saveAnswer(exam.code(), attempt, ids.get(1), List.of(1)).expectStatus(204);
        saveAnswer(exam.code(), attempt, ids.get(2), List.of(0)).expectStatus(204);
        submit(exam.code(), startAttempt(exam.code(), null, unique("Eva")), null, answers(ids, allCorrect(5)))
                .expectStatus(201);

        clock.advance(Duration.ofMinutes(13));
        List<Map<String, Object>> results = get("/api/results/exam/" + exam.id(), token).expectStatus(200).asList();

        Map<String, Object> auto = results.stream().filter(r -> name.equals(r.get("studentName"))).findFirst()
                .orElseThrow();
        assertThat(auto).containsEntry("autoSubmitted", true)
                .containsEntry("correctAnswers", 2)
                .containsEntry("score", 40)
                .containsEntry("timeSpentSeconds", 600);
        assertThat(LocalDateTime.parse((String) auto.get("completedAt"))).isEqualTo(startedAt.plusMinutes(10));
        assertThat(results).filteredOn(r -> !name.equals(r.get("studentName")))
                .extracting(r -> r.get("autoSubmitted")).containsExactly(false);
        // Se corrige una sola vez, y ya no se puede entregar
        assertThat(get("/api/results/exam/" + exam.id(), token).expectStatus(200).asList()).hasSize(2);
        assertThat(submit(exam.code(), attempt, null, answers(ids, allCorrect(5))).expectStatus(409).message())
                .isEqualTo(INVALID);
    }

    @Test
    @DisplayName("Si dos consultas corrigen a la vez (resultados y estadísticas), cada intento se entrega una sola vez")
    void concurrentAutoSubmit_onceEach() throws Exception {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, 10, true);
        int students = 5;
        for (int i = 0; i < students; i++) {
            startAttempt(exam.code(), null, unique("Alumno"));
        }
        clock.advance(Duration.ofMinutes(13));

        // La página de resultados pide la lista y las estadísticas a la vez
        CountDownLatch ready = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<HttpResult> list = pool.submit(() -> {
                ready.await();
                return get("/api/results/exam/" + exam.id(), token);
            });
            Future<HttpResult> stats = pool.submit(() -> {
                ready.await();
                return get("/api/results/exam/" + exam.id() + "/statistics", token);
            });
            ready.countDown();
            list.get().expectStatus(200);
            stats.get().expectStatus(200);
        } finally {
            pool.shutdown();
        }

        assertThat(get("/api/results/exam/" + exam.id(), token).expectStatus(200).asList()).hasSize(students);
    }

    @Test
    @DisplayName("La tarea periódica corrige los intentos caducados; el alumno lo ve en sus resultados")
    void scheduledAutoSubmit() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);
        Session student = newStudent();
        startAttempt(exam.code(), student.token(), "Yo");

        clock.advance(Duration.ofHours(11));
        resultService.autoSubmitExpired();
        // La sesión dura 8 horas (DE-10): el alumno vuelve a entrar
        String token = login(student.username(), STUDENT_PASSWORD);
        assertThat(get("/api/results/me", token).expectStatus(200).asList()).isEmpty();

        clock.advance(Duration.ofHours(2));
        resultService.autoSubmitExpired();
        List<Map<String, Object>> mine = get("/api/results/me", token).expectStatus(200).asList();
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0)).containsEntry("autoSubmitted", true)
                .containsEntry("score", 0)
                .containsEntry("timeSpentSeconds", 12 * 3600);
    }

    // ───── Ayudas ─────

    private HttpResult saveAnswer(String code, String attemptToken, Long questionId, List<Integer> options) {
        return put("/api/exams/by-code/" + code + "/attempts/answers", null, obj(
                "attemptToken", attemptToken,
                "questionId", questionId,
                "chosenOptions", options));
    }

    private HttpResult resume(String code, String attemptToken) {
        return post("/api/exams/by-code/" + code + "/attempts/resume", null, obj("attemptToken", attemptToken));
    }

    private static List<Integer> saved(HttpResult attempt, long questionId) {
        return attempt.read("$.answers['" + questionId + "']");
    }
}
