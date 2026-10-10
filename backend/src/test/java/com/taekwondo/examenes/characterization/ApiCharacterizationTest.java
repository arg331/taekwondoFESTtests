package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.MutableClock;
import com.taekwondo.examenes.TestcontainersConfiguration;
import com.taekwondo.examenes.characterization.support.HttpResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static com.taekwondo.examenes.characterization.support.Json.write;

/**
 * Base de los tests de caracterización de la API.
 *
 * Fijan el comportamiento ACTUAL de la API HTTP (estados, cuerpos, permisos y
 * mensajes) para que los refactors no lo cambien sin darnos cuenta. Son tests de
 * caja negra: solo hablan HTTP y no importan clases de producción, así que mover
 * paquetes o reescribir servicios no obliga a tocarlos.
 *
 * Todas las clases hijas comparten el mismo contexto de Spring y el mismo
 * PostgreSQL (Testcontainers). Por eso cada test crea sus propios datos con
 * nombres únicos y no depende de lo que hagan los demás.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, ApiCharacterizationTest.ClockConfig.class})
abstract class ApiCharacterizationTest {

    static final String ADMIN_USERNAME = "admin";
    static final String ADMIN_PASSWORD = "admin123";
    static final String STUDENT_PASSWORD = "secreto1";
    static final List<String> FOUR_OPTIONS = List.of("A", "B", "C", "D");

    @Autowired
    MockMvc mvc;

    @Autowired
    MutableClock clock;

    /** Reloj fijo que los tests adelantan para simular el paso del tiempo. */
    @TestConfiguration(proxyBeanMethods = false)
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock characterizationClock() {
            return new MutableClock(Instant.parse("2026-10-08T10:00:00Z"), ZoneId.of("Europe/Madrid"));
        }
    }

    /** Usuario conectado: id, nombre de usuario y token de sesión. */
    record Session(long id, String username, String token) {}

    // ───── Peticiones HTTP ─────

    HttpResult get(String url, String token) {
        return send(HttpMethod.GET, url, token, null);
    }

    HttpResult post(String url, String token, Object body) {
        return send(HttpMethod.POST, url, token, body);
    }

    HttpResult put(String url, String token, Object body) {
        return send(HttpMethod.PUT, url, token, body);
    }

    HttpResult patch(String url, String token, Object body) {
        return send(HttpMethod.PATCH, url, token, body);
    }

    HttpResult delete(String url, String token) {
        return send(HttpMethod.DELETE, url, token, null);
    }

    /** Envía una petición; body puede ser un Map/List (se serializa) o un String con JSON literal. */
    HttpResult send(HttpMethod method, String url, String token, Object body) {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(method, url);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(body instanceof String s ? s : write(body));
        }
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        try {
            MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
            return new HttpResult(response.getStatus(), response.getContentAsString(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Fallo al ejecutar " + method + " " + url, ex);
        }
    }

    // ───── Usuarios ─────

    static String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    String adminToken() {
        return login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    String login(String usernameOrEmail, String password) {
        return post("/api/auth/login", null, obj("usernameOrEmail", usernameOrEmail, "plainPassword", password))
                .expectStatus(200)
                .read("$.token");
    }

    Session newStudent() {
        String username = unique("alumno");
        long id = post("/api/auth/register", null, obj(
                "username", username,
                "email", username + "@test.com",
                "plainPassword", STUDENT_PASSWORD,
                "displayName", "Alumno " + username))
                .expectStatus(201)
                .id();
        return new Session(id, username, login(username, STUDENT_PASSWORD));
    }

    /** Profesor (rol TEACHER): se registra como alumno y el admin le cambia el rol. */
    Session newTeacher() {
        return newUserWithRole("TEACHER");
    }

    /** Usuario con el rol indicado: se registra como alumno y el admin le cambia el rol. */
    Session newUserWithRole(String role) {
        Session student = newStudent();
        patch("/api/users/" + student.id() + "/role", adminToken(), obj("role", role)).expectStatus(200);
        return new Session(student.id(), student.username(), login(student.username(), STUDENT_PASSWORD));
    }

    // ───── Banco de preguntas ─────

    long createTag(String token, String name, String color) {
        return post("/api/tags", token, obj("name", name, "color", color)).expectStatus(201).id();
    }

    long createQuestion(String token, String text, List<String> options, int correctAnswer, List<Long> tagIds) {
        return post("/api/questions", token, obj(
                "text", text,
                "options", options,
                "correctAnswer", correctAnswer,
                "explanation", "Explicación de " + text,
                "difficulty", "MEDIO",
                "tagIds", tagIds))
                .expectStatus(201)
                .id();
    }

    /** Crea n preguntas de 4 opciones; la correcta de la i-ésima es i % 4. */
    List<Long> createQuestions(String token, int n) {
        String batch = unique("lote");
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ids.add(createQuestion(token, "Pregunta " + i + " " + batch, FOUR_OPTIONS, i % 4, List.of()));
        }
        return ids;
    }

    // ───── Exámenes ─────

    static Map<String, Object> draftBody(String title, int numberOfQuestions, Integer timeLimit, boolean showScore,
                                         boolean randomizeOptions, boolean randomizeQuestionOrder) {
        return obj(
                "title", title,
                "numberOfQuestions", numberOfQuestions,
                "timeLimitMinutes", timeLimit,
                "showScore", showScore,
                "randomizeOptions", randomizeOptions,
                "randomizeQuestionOrder", randomizeQuestionOrder);
    }

    long createDraft(String token, int numberOfQuestions, Integer timeLimit, boolean showScore) {
        return post("/api/exams/drafts", token,
                draftBody(unique("Examen"), numberOfQuestions, timeLimit, showScore, false, false))
                .expectStatus(201)
                .id();
    }

    void setQuestions(String token, long examId, List<Long> questionIds) {
        put("/api/exams/" + examId + "/questions", token, obj("questionIds", questionIds)).expectStatus(200);
    }

    HttpResult publish(String token, long examId, String visibility, String accessMode, LocalDateTime expiresAt) {
        return post("/api/exams/" + examId + "/publish", token, obj(
                "visibility", visibility,
                "accessMode", accessMode,
                "expiresAt", expiresAt == null ? null : expiresAt.toString()));
    }

    /** Examen publicado y abierto (OPEN, PUBLIC, expira en 1 h) con n preguntas nuevas. */
    PublishedExam publishedExam(String token, int n, Integer timeLimit, boolean showScore) {
        List<Long> questionIds = createQuestions(token, n);
        long examId = createDraft(token, n, timeLimit, showScore);
        setQuestions(token, examId, questionIds);
        String code = publish(token, examId, "PUBLIC", null, null).expectStatus(200).read("$.code");
        return new PublishedExam(examId, code, questionIds);
    }

    record PublishedExam(long id, String code, List<Long> questionIds) {}

    // ───── Intentos y entregas ─────

    String startAttempt(String code, String token) {
        return post("/api/exams/by-code/" + code + "/attempts", token, null).expectStatus(200).read("$.attemptToken");
    }

    /** Respuestas para todas las preguntas: chosen.get(i) es la opción elegida en la pregunta i (null = en blanco). */
    static List<Map<String, Object>> answers(List<Long> questionIds, List<Integer> chosen) {
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            answers.add(obj("questionId", questionIds.get(i), "chosenOption", chosen.get(i)));
        }
        return answers;
    }

    /** Las respuestas correctas de createQuestions(): i % 4. */
    static List<Integer> allCorrect(int n) {
        List<Integer> chosen = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            chosen.add(i % 4);
        }
        return chosen;
    }

    HttpResult submit(String code, String attemptToken, String studentName, String token,
                      List<Map<String, Object>> answers) {
        return post("/api/results", token, obj(
                "examCode", code,
                "attemptToken", attemptToken,
                "studentName", studentName,
                "studentClub", null,
                "studentEmail", null,
                "answers", answers));
    }

    LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
