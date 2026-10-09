package com.taekwondo.examenes;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorre la API de punta a punta: roles, creación y publicación de un
 * examen, intento anónimo con tiempo límite y nota oculta, y resultados.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExamFlowIntegrationTest {

    @TestConfiguration
    static class ClockTestConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-10-08T10:00:00Z"), ZoneId.of("Europe/Madrid"));
        }
    }

    @Autowired MockMvc mvc;
    @Autowired MutableClock clock;

    String adminToken;
    String studentToken;

    /**
     * Los tests comparten base de datos, así que cada uno usa su propio alumno
     * y crea sus propios exámenes: no dependen del orden ni de lo que dejen los demás.
     */
    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "admin123");
        String username = "alumno_" + UUID.randomUUID().toString().substring(0, 8);
        post("/api/auth/register", null, json(Map.of("username", username, "email", username + "@test.com",
                "plainPassword", "secreto1", "displayName", "Alumno Uno")))
                .andExpect(status().isCreated());
        studentToken = login(username, "secreto1");
    }

    @Test
    void studentsAndAnonymousCannotUseTeacherEndpoints() throws Exception {
        mvc.perform(get("/api/questions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/questions").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/exams/mine").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/exams/public").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void invalidPayloadsAreRejectedWith400() throws Exception {
        post("/api/questions", adminToken, """
                {"text":"¿?","options":["a"],"correctAnswer":0,"difficulty":"FACIL"}
                """).andExpect(status().isBadRequest());
        post("/api/questions", adminToken, """
                {"text":"¿?","options":["a","b","c"],"correctAnswer":3,"difficulty":"FACIL"}
                """).andExpect(status().isBadRequest());
        post("/api/questions", adminToken, """
                {"text":"¿Verdadero o falso?","options":["Verdadero","Falso"],"correctAnswer":1,"difficulty":"FACIL"}
                """).andExpect(status().isCreated());
        post("/api/auth/register", null, """
                {"username":"x","email":"no-es-email","plainPassword":"1","displayName":""}
                """).andExpect(status().isBadRequest());
        post("/api/results", null, "{}").andExpect(status().isBadRequest());
    }

    @Test
    void fullExamFlowWithHiddenScoreAndAnonymousStudent() throws Exception {
        List<Long> questionIds = createQuestions(5);
        long examId = createDraft(5, 10, false);
        put("/api/exams/" + examId + "/questions", adminToken, json(Map.of("questionIds", questionIds)))
                .andExpect(status().isOk());
        String code = publish(examId);

        // Un examen publicado ya no se puede reconfigurar ni perder preguntas
        patch("/api/exams/" + examId + "/config", adminToken, """
                {"numberOfQuestions":6,"timeLimitMinutes":10,"showScore":true,"randomizeOptions":false,"randomizeQuestionOrder":false}
                """).andExpect(status().isConflict());
        mvc.perform(delete("/api/questions/" + questionIds.get(0)).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        // El alumno anónimo ve el examen y empieza un intento sin ver las respuestas correctas
        mvc.perform(get("/api/exams/by-code/" + code)).andExpect(status().isOk());
        String attempt = post("/api/exams/by-code/" + code + "/attempts", null, "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(5)))
                .andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.questions[0].options[0].index").exists())
                .andReturn().getResponse().getContentAsString();
        String attemptToken = JsonPath.read(attempt, "$.attemptToken");

        clock.advance(Duration.ofMinutes(4));

        // Acierta 3 (correctAnswer = i % 4), falla 1 y deja 1 en blanco
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Map<String, Object> a = new java.util.HashMap<>();
            a.put("questionId", questionIds.get(i));
            Integer chosen = i < 3 ? Integer.valueOf(i % 4) : i == 3 ? Integer.valueOf(0) : null;
            a.put("chosenOption", chosen);
            answers.add(a);
        }
        String submission = json(Map.of("examCode", code, "attemptToken", attemptToken,
                "studentName", "  Juan Pérez ", "answers", answers));

        post("/api/results", null, submission)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scoreVisible").value(false))
                .andExpect(jsonPath("$.score").value(nullValue()))
                .andExpect(jsonPath("$.answers").value(nullValue()))
                .andExpect(jsonPath("$.timeSpentSeconds").value(240));

        // Mismo nombre con otras mayúsculas: no puede repetir
        post("/api/results", null, submission.replace("  Juan Pérez ", "JUAN PÉREZ"))
                .andExpect(status().isConflict());

        // El profesor sí ve la nota completa
        mvc.perform(get("/api/results/exam/" + examId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentName").value("Juan Pérez"))
                .andExpect(jsonPath("$[0].score").value(60))
                .andExpect(jsonPath("$[0].correctAnswers").value(3))
                .andExpect(jsonPath("$[0].answers[4].studentAnswer").value(nullValue()));
        mvc.perform(get("/api/results/exam/" + examId + "/statistics")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.totalAttempts").value(1))
                .andExpect(jsonPath("$.failedCount").value(1));
    }

    @Test
    void submissionAfterTimeLimitIsRejected() throws Exception {
        List<Long> questionIds = createQuestions(5);
        long examId = createDraft(5, 10, true);
        put("/api/exams/" + examId + "/questions", adminToken, json(Map.of("questionIds", questionIds)))
                .andExpect(status().isOk());
        String code = publish(examId);

        String attempt = post("/api/exams/by-code/" + code + "/attempts", null, "")
                .andReturn().getResponse().getContentAsString();
        String attemptToken = JsonPath.read(attempt, "$.attemptToken");

        clock.advance(Duration.ofMinutes(13));

        List<Map<String, Object>> answers = questionIds.stream()
                .map(id -> Map.<String, Object>of("questionId", id, "chosenOption", 0))
                .toList();
        post("/api/results", null, json(Map.of("examCode", code, "attemptToken", attemptToken,
                "studentName", "Lenta", "answers", answers)))
                .andExpect(status().isConflict());
    }

    @Test
    void registeredStudentSeesOwnResultWhenScoreIsShown() throws Exception {
        List<Long> questionIds = createQuestions(5);
        long examId = createDraft(5, null, true);
        put("/api/exams/" + examId + "/questions", adminToken, json(Map.of("questionIds", questionIds)))
                .andExpect(status().isOk());
        String code = publish(examId);

        String attempt = post("/api/exams/by-code/" + code + "/attempts", studentToken, "")
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> answers = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            answers.add(Map.of("questionId", questionIds.get(i), "chosenOption", i % 4));
        }
        post("/api/results", studentToken, json(Map.of("examCode", code,
                "attemptToken", JsonPath.read(attempt, "$.attemptToken"),
                "studentName", "Alumno Uno", "answers", answers)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.score").value(100))
                .andExpect(jsonPath("$.passed").value(true));

        // Un registrado no puede volver a empezar el mismo examen
        post("/api/exams/by-code/" + code + "/attempts", studentToken, "").andExpect(status().isConflict());

        mvc.perform(get("/api/results/me").header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].scoreVisible").value(true));
    }

    @Test
    void adminCannotDemoteThemselves() throws Exception {
        String me = mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + adminToken))
                .andReturn().getResponse().getContentAsString();
        Integer adminId = JsonPath.read(me, "$.id");
        patch("/api/users/" + adminId + "/demote", adminToken, "").andExpect(status().isConflict());
    }

    // ───── Auxiliares ─────

    private List<Long> createQuestions(int count) throws Exception {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String body = post("/api/questions", adminToken, json(Map.of(
                    "text", "Pregunta " + i,
                    "options", List.of("A", "B", "C", "D"),
                    "correctAnswer", i % 4,
                    "difficulty", "MEDIO")))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            ids.add(((Number) JsonPath.read(body, "$.id")).longValue());
        }
        return ids;
    }

    private long createDraft(int numberOfQuestions, Integer timeLimit, boolean showScore) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", "Examen de prueba");
        body.put("numberOfQuestions", numberOfQuestions);
        body.put("timeLimitMinutes", timeLimit);
        body.put("showScore", showScore);
        body.put("randomizeOptions", true);
        body.put("randomizeQuestionOrder", true);
        String response = post("/api/exams/drafts", adminToken, json(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private String publish(long examId) throws Exception {
        String response = post("/api/exams/" + examId + "/publish", adminToken, """
                {"visibility":"PUBLIC"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.code");
    }

    private String login(String user, String password) throws Exception {
        String response = post("/api/auth/login", null,
                json(Map.of("usernameOrEmail", user, "plainPassword", password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.token");
    }

    private ResultActions post(String url, String token, String body) throws Exception {
        return send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url), token, body);
    }

    private ResultActions put(String url, String token, String body) throws Exception {
        return send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url), token, body);
    }

    private ResultActions patch(String url, String token, String body) throws Exception {
        return send(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url), token, body);
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        request.contentType(MediaType.APPLICATION_JSON).content(body);
        if (token != null) request.header("Authorization", "Bearer " + token);
        return mvc.perform(request);
    }

    /** JSON mínimo para mapas, listas, números, booleanos, null y strings. */
    private static String json(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            map.forEach((k, v) -> sb.append(sb.length() > 1 ? "," : "").append(json(k.toString())).append(":").append(json(v)));
            return sb.append("}").toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            list.forEach(v -> sb.append(sb.length() > 1 ? "," : "").append(json(v)));
            return sb.append("]").toString();
        }
        throw new IllegalArgumentException("Tipo no soportado: " + value.getClass());
    }
}
