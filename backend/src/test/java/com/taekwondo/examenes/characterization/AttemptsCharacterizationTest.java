package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * DE-04 y RF-52: el alumno se identifica al empezar, cada intento queda registrado en el servidor
 * y el profesor fija cuántos tiene cada alumno (1 por defecto).
 */
@DisplayName("Caracterización: intentos registrados y número de intentos")
class AttemptsCharacterizationTest extends ApiCharacterizationTest {

    private static final String ALL_USED = "Ya has hecho todos los intentos de este examen";
    private static final String BOTH_LIMITS = "Indica un número de intentos o «sin límite», no las dos cosas";

    // ───── Identificarse al empezar (DE-04) ─────

    @Test
    @DisplayName("El alumno se identifica al empezar; la entrega usa esos datos aunque mande otros")
    void identity_fixedAtStart() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);
        String attempt = start(exam.code(), null, obj("studentName", "  Ana Pérez ", "studentClub", "Club Ávila",
                "studentEmail", "ana@x.es")).expectStatus(200).read("$.attemptToken");

        HttpResult result = post("/api/results", null, obj("examCode", exam.code(), "attemptToken", attempt,
                "studentName", "Otro nombre", "answers", answers(exam.questionIds(), allCorrect(5)))).expectStatus(201);

        assertThat((String) result.read("$.studentName")).isEqualTo("Ana Pérez");
        assertThat((String) result.read("$.studentClub")).isEqualTo("Club Ávila");
        assertThat((String) result.read("$.studentEmail")).isEqualTo("ana@x.es");
    }

    @Test
    @DisplayName("Sin nombre, o con un email mal escrito, no se puede empezar (400)")
    void identity_required() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);

        assertThat(start(exam.code(), null, obj("studentName", " ")).expectStatus(400).message()).startsWith("Nombre");
        assertThat(start(exam.code(), null, obj("studentName", "Ana", "studentEmail", "no-es-email"))
                .expectStatus(400).message()).startsWith("Email");
    }

    @Test
    @DisplayName("El intento dice cuál es y cuántos hay")
    void start_reportsAttemptNumber() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);

        HttpResult attempt = start(exam.code(), null, obj("studentName", unique("Ana"))).expectStatus(200);

        assertThat(attempt.asMap()).containsOnlyKeys("attemptToken", "questions", "attemptNumber", "maxAttempts", "answers",
                "secondsLeft");
        assertThat((Integer) attempt.read("$.attemptNumber")).isEqualTo(1);
        assertThat((Integer) attempt.read("$.maxAttempts")).isEqualTo(1);
    }

    // ───── Número de intentos (RF-52) ─────

    @Test
    @DisplayName("Por defecto, 1 intento: ni el registrado ni el anónimo (mismo nombre, otras mayúsculas) repiten")
    void defaultOneAttempt() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true);
        Session student = newStudent();
        String name = unique("Juan");
        finish(exam, startAttempt(exam.code(), student.token(), "Yo"), student.token());
        finish(exam, startAttempt(exam.code(), null, name), null);

        assertThat((Integer) get("/api/exams/by-code/" + exam.code(), null).read("$.maxAttempts")).isEqualTo(1);
        assertThat(start(exam.code(), student.token(), obj("studentName", "Otro")).expectStatus(409).message())
                .isEqualTo(ALL_USED);
        assertThat(start(exam.code(), null, obj("studentName", name.toUpperCase())).expectStatus(409).message())
                .isEqualTo(ALL_USED);
        start(exam.code(), null, obj("studentName", unique("Otra"))).expectStatus(200);
    }

    @Test
    @DisplayName("Con 3 intentos, el alumno hace 3 y el cuarto se rechaza")
    void threeAttempts() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true, 3);
        Session student = newStudent();

        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            HttpResult attempt = start(exam.code(), student.token(), obj("studentName", "Yo")).expectStatus(200);
            numbers.add(attempt.read("$.attemptNumber"));
            finish(exam, attempt.read("$.attemptToken"), student.token());
        }

        assertThat(numbers).containsExactly(1, 2, 3);
        assertThat(start(exam.code(), student.token(), obj("studentName", "Yo")).expectStatus(409).message())
                .isEqualTo(ALL_USED);
        assertThat(get("/api/results/me", student.token()).expectStatus(200).asList()).hasSize(3);
    }

    @Test
    @DisplayName("Publicar: de 1 a 4 intentos o sin límite; fuera de ese rango, 400; las dos cosas a la vez, 409")
    void publish_attemptsRange() {
        String token = adminToken();
        long examId = draftWithQuestions(token);

        assertThat(publish(token, examId, obj("visibility", "PUBLIC", "maxAttempts", 0)).expectStatus(400).message())
                .startsWith("Intentos: ");
        assertThat(publish(token, examId, obj("visibility", "PUBLIC", "maxAttempts", 5)).expectStatus(400).message())
                .startsWith("Intentos: ");
        assertThat(publish(token, examId, obj("visibility", "PUBLIC", "maxAttempts", 2, "unlimitedAttempts", true))
                .expectStatus(409).message()).isEqualTo(BOTH_LIMITS);
        assertThat((Integer) publish(token, examId, obj("visibility", "PUBLIC", "maxAttempts", 4))
                .expectStatus(200).read("$.maxAttempts")).isEqualTo(4);
    }

    @Test
    @DisplayName("Sin límite: el alumno repite las veces que quiera y el intento no dice «de N»")
    void unlimitedAttempts() {
        String token = adminToken();
        long examId = draftWithQuestions(token);
        HttpResult published = publish(token, examId, obj("visibility", "PUBLIC", "unlimitedAttempts", true))
                .expectStatus(200);
        PublishedExam exam = new PublishedExam(examId, published.read("$.code"),
                get("/api/exams/" + examId, token).<List<Integer>>read("$.questionIds").stream().map(Integer::longValue).toList());
        Session student = newStudent();

        assertThat((Object) published.read("$.maxAttempts")).isNull();
        for (int i = 1; i <= 6; i++) {
            HttpResult attempt = start(exam.code(), student.token(), obj("studentName", "Yo")).expectStatus(200);
            assertThat((Integer) attempt.read("$.attemptNumber")).isEqualTo(i);
            assertThat((Object) attempt.read("$.maxAttempts")).isNull();
            finish(exam, attempt.read("$.attemptToken"), student.token());
        }
    }

    // ───── Cambiar los intentos de un examen publicado ─────

    @Test
    @DisplayName("Después de publicar se pueden subir o bajar los intentos; quien ya los ha usado no empieza otro")
    void changeAttemptsAfterPublishing() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true, 3);
        Session student = newStudent();
        finish(exam, startAttempt(exam.code(), student.token(), "Yo"), student.token());
        finish(exam, startAttempt(exam.code(), student.token(), "Yo"), student.token());

        assertThat((Integer) changeAttempts(exam.id(), token, obj("maxAttempts", 1)).expectStatus(200)
                .read("$.maxAttempts")).isEqualTo(1);
        assertThat(start(exam.code(), student.token(), obj("studentName", "Yo")).expectStatus(409).message())
                .isEqualTo(ALL_USED);

        assertThat((Object) changeAttempts(exam.id(), token, obj("unlimitedAttempts", true)).expectStatus(200)
                .read("$.maxAttempts")).isNull();
        assertThat((Integer) start(exam.code(), student.token(), obj("studentName", "Yo")).expectStatus(200)
                .read("$.attemptNumber")).isEqualTo(3);
    }

    @Test
    @DisplayName("Cambiar intentos: no en un borrador (409), solo el dueño (404), de 1 a 4 (400)")
    void changeAttempts_rules() {
        String token = adminToken();
        long draft = draftWithQuestions(token);
        PublishedExam exam = publishedExam(token, 5, null, true);

        assertThat(changeAttempts(draft, token, obj("maxAttempts", 2)).expectStatus(409).message())
                .isEqualTo("Los intentos de un borrador se eligen al publicarlo");
        changeAttempts(exam.id(), newTeacher().token(), obj("maxAttempts", 2)).expectStatus(404);
        assertThat(changeAttempts(exam.id(), token, obj("maxAttempts", 5)).expectStatus(400).message())
                .startsWith("Intentos: ");
        assertThat(changeAttempts(exam.id(), token, obj("maxAttempts", 2, "unlimitedAttempts", true))
                .expectStatus(409).message()).isEqualTo(BOTH_LIMITS);
    }

    // ───── Intentos abandonados ─────
    // Volver a empezar con un intento abierto lo recupera: AttemptRecoveryCharacterizationTest (RF-24)

    @Test
    @DisplayName("Un intento abandonado cuenta cuando se le acaba el tiempo; entonces se puede empezar el siguiente")
    void abandonedAttemptCounts() {
        PublishedExam single = publishedExam(adminToken(), 5, 10, true);
        PublishedExam twice = publishedExam(adminToken(), 5, 10, true, 2);
        Session student = newStudent();
        startAttempt(single.code(), student.token(), "Yo");
        startAttempt(twice.code(), student.token(), "Yo");

        clock.advance(Duration.ofMinutes(13));

        assertThat(start(single.code(), student.token(), obj("studentName", "Yo")).expectStatus(409).message())
                .isEqualTo(ALL_USED);
        assertThat((Integer) start(twice.code(), student.token(), obj("studentName", "Yo")).expectStatus(200)
                .read("$.attemptNumber")).isEqualTo(2);
    }

    @Test
    @DisplayName("Un intento solo se entrega una vez")
    void submitTwice_conflict() {
        PublishedExam exam = publishedExam(adminToken(), 5, null, true, 3);
        String attempt = startAttempt(exam.code(), null, unique("Ana"));
        finish(exam, attempt, null);

        assertThat(submit(exam.code(), attempt, null, answers(exam.questionIds(), allCorrect(5)))
                .expectStatus(409).message()).isEqualTo("Este intento ya se ha entregado");
    }

    // ───── Opciones de cada pregunta ─────

    @Test
    @DisplayName("Se puede elegir cualquier opción de la pregunta, también más allá de la cuarta; una que no existe, 409")
    void answers_validatedAgainstEachQuestion() {
        String token = adminToken();
        List<String> six = List.of("A", "B", "C", "D", "E", "F");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ids.add(createQuestion(token, unique("Seis opciones"), six, 5, List.of()));
        }
        long examId = createDraft(token, 5, null, true);
        setQuestions(token, examId, ids);
        String code = publish(token, examId, "PUBLIC", null, null).expectStatus(200).read("$.code");

        HttpResult right = submit(code, startAttempt(code, null, unique("A")), null,
                answers(ids, List.of(5, 5, 5, 5, 5))).expectStatus(201);
        HttpResult missing = submit(code, startAttempt(code, null, unique("B")), null,
                answers(ids, List.of(6, 5, 5, 5, 5)));

        assertThat((Integer) right.read("$.score")).isEqualTo(100);
        assertThat(missing.expectStatus(409).message()).isEqualTo("La opción 6 no existe en la pregunta " + ids.get(0));
    }

    // ───── Ayudas ─────

    private HttpResult publish(String token, long examId, Map<String, Object> body) {
        return post("/api/exams/" + examId + "/publish", token, body);
    }

    private long draftWithQuestions(String token) {
        long examId = createDraft(token, 5, null, true);
        setQuestions(token, examId, createQuestions(token, 5));
        return examId;
    }

    private HttpResult changeAttempts(long examId, String token, Map<String, Object> body) {
        return patch("/api/exams/" + examId + "/attempts", token, body);
    }

    private void finish(PublishedExam exam, String attemptToken, String token) {
        submit(exam.code(), attemptToken, token, answers(exam.questionIds(), allCorrect(5))).expectStatus(201);
    }
}
