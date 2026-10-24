package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/** RF-08: cambiar el color de un tag y borrarlo. Borrarlo lo quita de las preguntas, que no se borran. */
@DisplayName("Caracterización: color y borrado de tags")
class TagManagementCharacterizationTest extends ApiCharacterizationTest {

    @Test
    @DisplayName("Cambiar el color: solo un color válido (400) y solo de un tag propio (404)")
    void changeColor() {
        Session teacher = newTeacher();
        Session other = newTeacher();
        long tag = createTag(teacher.token(), unique("Combate"), "#C62828");

        HttpResult changed = changeColor(tag, "#1565C0", teacher.token()).expectStatus(200);

        assertThat((String) changed.read("$.color")).isEqualTo("#1565C0");
        assertThat((String) get("/api/tags/" + tag, teacher.token()).read("$.color")).isEqualTo("#1565C0");
        changeColor(tag, "azul", teacher.token()).expectStatus(400);
        changeColor(tag, "#000000", other.token()).expectStatus(404);
    }

    @Test
    @DisplayName("Borrar un tag lo quita de las preguntas y de los exámenes generados con él; ellos siguen ahí")
    void delete_detachesFromQuestionsAndExams() {
        Session teacher = newTeacher();
        long tag = createTag(teacher.token(), unique("Combate"), "#C62828");
        long kept = createTag(teacher.token(), unique("Formas"), "#1565C0");
        long question = createQuestion(teacher.token(), unique("Con tags"), FOUR_OPTIONS, 0, List.of(tag, kept));
        for (int i = 0; i < 5; i++) {
            createQuestion(teacher.token(), unique("Con tag"), FOUR_OPTIONS, 0, List.of(tag));
        }
        Map<String, Object> body = draftBody(unique("Aleatorio"), 5, null, true, false, false);
        body.put("requiredAnyOfTagIds", List.of(tag));
        long examId = post("/api/exams/drafts/pre-generated", teacher.token(), body).expectStatus(201).id();

        delete("/api/tags/" + tag, teacher.token()).expectStatus(204);

        get("/api/tags/" + tag, teacher.token()).expectStatus(404);
        assertThat((List<Integer>) get("/api/questions/" + question, teacher.token()).expectStatus(200)
                .read("$.tags[*].id")).containsExactly((int) kept);
        HttpResult exam = get("/api/exams/" + examId, teacher.token()).expectStatus(200);
        assertThat((List<?>) exam.read("$.generationTags")).isEmpty();
        assertThat((List<?>) exam.read("$.questionIds")).hasSize(5);
    }

    @Test
    @DisplayName("Solo se borra un tag propio (404)")
    void delete_onlyOwn() {
        Session teacher = newTeacher();
        Session other = newTeacher();
        long tag = createTag(teacher.token(), unique("Combate"), "#C62828");

        delete("/api/tags/" + tag, other.token()).expectStatus(404);
        get("/api/tags/" + tag, teacher.token()).expectStatus(200);
    }

    private HttpResult changeColor(long tagId, String color, String token) {
        return patch("/api/tags/" + tagId + "/color", token, obj("newColor", color));
    }
}
