package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static com.taekwondo.examenes.characterization.support.Json.write;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RNF-13: los errores de validación salen en español, llegue la cabecera Accept-Language que llegue,
 * y con el nombre del campo que ve el usuario en vez del técnico.
 */
@DisplayName("Caracterización: mensajes de validación en español")
class ValidationMessagesCharacterizationTest extends ApiCharacterizationTest {

    private static final String BAD_EMAIL = "Email: debe ser una dirección de correo electrónico con formato correcto";

    @Test
    @DisplayName("En español y con el nombre del campo, sin Accept-Language y aunque pida inglés")
    void spanishWhateverTheHeader() throws Exception {
        Map<String, Object> body = registration("no-es-un-email");

        assertThat(post("/api/auth/register", null, body).expectStatus(400).message()).isEqualTo(BAD_EMAIL);

        MockHttpServletResponse english = mvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
                        .header("Accept-Language", "en-US,en;q=0.9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(write(body)))
                .andReturn().getResponse();
        assertThat(new HttpResult(english.getStatus(), english.getContentAsString(StandardCharsets.UTF_8))
                .expectStatus(400).message()).isEqualTo(BAD_EMAIL);
    }

    @Test
    @DisplayName("Los campos se nombran como los ve el usuario, también dentro de una lista")
    void fieldLabels() {
        Session student = newStudent();
        String admin = adminToken();

        assertThat(post("/api/auth/register", null, obj("username", unique("u"), "plainPassword", "123",
                "displayName", "Nombre")).expectStatus(400).message())
                .isEqualTo("Contraseña: el tamaño debe estar entre 6 y 100");
        assertThat(patch("/api/users/" + student.id() + "/role", admin, obj()).expectStatus(400).message())
                .isEqualTo("Rol: no debe ser nulo");
        assertThat(post("/api/results", null, obj("examCode", "EXM-1", "attemptToken", "x",
                "answers", List.of(obj("questionId", 1, "chosenOptions", List.of(-1))))).expectStatus(400).message())
                .isEqualTo("Opciones marcadas: debe ser mayor que o igual a 0");
    }

    private static Map<String, Object> registration(String email) {
        String username = unique("u");
        return obj("username", username, "email", email, "plainPassword", "secreto1", "displayName", "Nombre");
    }
}
