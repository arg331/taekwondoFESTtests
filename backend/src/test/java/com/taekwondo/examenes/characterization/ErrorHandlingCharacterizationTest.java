package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: formato de los errores")
class ErrorHandlingCharacterizationTest extends ApiCharacterizationTest {

    private static final String BAD_REQUEST_MESSAGE = "JSON malformado o petición inválida";

    @Test
    @DisplayName("JSON malformado devuelve 400 con mensaje genérico")
    void malformedJson_badRequest() {
        HttpResult result = post("/api/tags", adminToken(), "{\"name\": ");

        assertThat(result.expectStatus(400).message()).isEqualTo(BAD_REQUEST_MESSAGE);
        assertThat(result.asMap()).containsOnlyKeys("timestamp", "status", "error", "message");
        assertThat((Integer) result.read("$.status")).isEqualTo(400);
        assertThat((String) result.read("$.error")).isEqualTo("Bad Request");
    }

    @Test
    @DisplayName("Valor de enum desconocido en el cuerpo devuelve 400 con mensaje genérico")
    void unknownEnumValue_badRequest() {
        HttpResult result = post("/api/questions", adminToken(), obj("text", "x", "options", FOUR_OPTIONS,
                "correctAnswer", 0, "difficulty", "IMPOSIBLE", "tagIds", List.of()));

        assertThat(result.expectStatus(400).message()).isEqualTo(BAD_REQUEST_MESSAGE);
    }

    @Test
    @DisplayName("Id no numérico en la ruta devuelve 400 con mensaje genérico")
    void nonNumericPathId_badRequest() {
        HttpResult result = get("/api/exams/abc", adminToken());

        assertThat(result.expectStatus(400).message()).isEqualTo(BAD_REQUEST_MESSAGE);
    }

    @Test
    @DisplayName("Los errores de negocio usan el mismo formato con 404 Not Found y 409 Conflict")
    void businessErrors_sameShape() {
        String token = adminToken();
        PublishedExam exam = publishedExam(token, 5, null, true);

        HttpResult notFound = get("/api/exams/999999", token).expectStatus(404);
        HttpResult conflict = delete("/api/exams/" + exam.id(), token).expectStatus(409);

        assertThat(notFound.asMap()).containsOnlyKeys("timestamp", "status", "error", "message");
        assertThat((String) notFound.read("$.error")).isEqualTo("Not Found");
        assertThat(conflict.asMap()).containsOnlyKeys("timestamp", "status", "error", "message");
        assertThat((String) conflict.read("$.error")).isEqualTo("Conflict");
    }
}
