package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: tags y banco de preguntas")
class QuestionBankCharacterizationTest extends ApiCharacterizationTest {

    private static final List<String> TAG_FIELDS = List.of("id", "name", "color", "createdAt");
    private static final List<String> QUESTION_FIELDS = List.of("id", "text", "options", "correctAnswers",
            "explanation", "grade", "type", "tags", "createdAt", "updatedAt", "sharedBy");

    // ───── Tags ─────

    @Test
    @DisplayName("Tags: crear devuelve 201 con id, nombre recortado, color y fecha")
    void createTag_valid_created() {
        Session teacher = newTeacher();
        String name = unique("Reglamento");

        HttpResult result = post("/api/tags", teacher.token(), obj("name", "  " + name + " ", "color", "#C62828"))
                .expectStatus(201);

        assertThat(result.asMap()).containsOnlyKeys(TAG_FIELDS);
        assertThat((String) result.read("$.name")).isEqualTo(name);
        assertThat((String) result.read("$.color")).isEqualTo("#C62828");
    }

    @Test
    @DisplayName("Tags: nombre repetido (sin distinguir mayúsculas) en el mismo profesor da 409; en otro profesor se permite")
    void createTag_duplicateName_conflictOnlyForSameOwner() {
        Session teacher = newTeacher();
        Session otherTeacher = newTeacher();
        String name = unique("Puntuacion");
        createTag(teacher.token(), name, "#112233");

        HttpResult sameOwner = post("/api/tags", teacher.token(), obj("name", name.toUpperCase(), "color", "#112233"));
        HttpResult otherOwner = post("/api/tags", otherTeacher.token(), obj("name", name, "color", "#112233"));

        assertThat(sameOwner.expectStatus(409).message())
                .isEqualTo("Ya existe un tag con el nombre '" + name.toUpperCase() + "'");
        otherOwner.expectStatus(201);
    }

    @Test
    @DisplayName("Tags: color que no es #RRGGBB devuelve 400")
    void createTag_invalidColor_badRequest() {
        HttpResult result = post("/api/tags", adminToken(), obj("name", unique("t"), "color", "rojo"));

        assertThat(result.expectStatus(400).message()).isEqualTo("color: debe ser un color hex válido (ej: #3B8BD4)");
    }

    @Test
    @DisplayName("Tags: el listado solo trae los propios, ordenados por nombre")
    void listTags_onlyOwn_sortedByName() {
        Session teacher = newTeacher();
        Session otherTeacher = newTeacher();
        createTag(teacher.token(), "Zeta", "#000000");
        createTag(teacher.token(), "Alfa", "#000000");
        createTag(otherTeacher.token(), "Ajeno", "#000000");

        List<Map<String, Object>> tags = get("/api/tags", teacher.token()).expectStatus(200).asList();

        assertThat(tags).extracting(t -> t.get("name")).containsExactly("Alfa", "Zeta");
    }

    @Test
    @DisplayName("Tags: ver o renombrar un tag ajeno responde como si no existiera (404)")
    void foreignTag_notFound() {
        Session owner = newTeacher();
        Session other = newTeacher();
        long tagId = createTag(owner.token(), unique("t"), "#000000");

        assertThat(get("/api/tags/" + tagId, other.token()).expectStatus(404).message())
                .isEqualTo("No existe un tag con id " + tagId);
        patch("/api/tags/" + tagId, other.token(), obj("newName", "x")).expectStatus(404);
        get("/api/tags/" + tagId, owner.token()).expectStatus(200);
    }

    @Test
    @DisplayName("Tags: renombrar; a un nombre de otro tag propio da 409; cambiar solo mayúsculas se permite")
    void renameTag_rules() {
        Session teacher = newTeacher();
        long tagId = createTag(teacher.token(), "Original", "#000000");
        createTag(teacher.token(), "Ocupado", "#000000");

        HttpResult toTaken = patch("/api/tags/" + tagId, teacher.token(), obj("newName", "ocupado"));
        HttpResult caseOnly = patch("/api/tags/" + tagId, teacher.token(), obj("newName", "ORIGINAL"));

        assertThat(toTaken.expectStatus(409).message()).isEqualTo("Ya existe un tag con el nombre 'ocupado'");
        assertThat((String) caseOnly.expectStatus(200).read("$.name")).isEqualTo("ORIGINAL");
    }

    // ───── Preguntas ─────

    @Test
    @DisplayName("Preguntas: crear devuelve la pregunta completa, con opciones recortadas y sus tags")
    void createQuestion_valid_returnsFullQuestion() {
        Session teacher = newTeacher();
        long tagId = createTag(teacher.token(), unique("Penalizaciones"), "#FF0000");

        HttpResult result = post("/api/questions", teacher.token(), obj(
                "text", "  ¿Qué es un kyong-go?  ",
                "options", List.of(" Amonestación ", "Punto", "Descalificación"),
                "correctAnswers", List.of(0),
                "explanation", "Media penalización",
                "grade", "AMARILLO_VERDE",
                "type", "C",
                "tagIds", List.of(tagId))).expectStatus(201);

        assertThat(result.asMap()).containsOnlyKeys(QUESTION_FIELDS);
        assertThat((String) result.read("$.text")).isEqualTo("¿Qué es un kyong-go?");
        assertThat((List<String>) result.read("$.options")).containsExactly("Amonestación", "Punto", "Descalificación");
        assertThat((List<Integer>) result.read("$.correctAnswers")).containsExactly(0);
        assertThat((String) result.read("$.grade")).isEqualTo("AMARILLO_VERDE");
        assertThat((String) result.read("$.type")).isEqualTo("C");
        assertThat((List<Integer>) result.read("$.tags[*].id")).containsExactly((int) tagId);
        assertThat((String) result.read("$.createdAt")).isEqualTo(result.read("$.updatedAt"));
    }

    @Test
    @DisplayName("Preguntas: menos de 2 o más de 20 opciones, o correcta fuera de rango, devuelven 400")
    void createQuestion_invalidOptions_badRequest() {
        String token = adminToken();

        HttpResult oneOption = post("/api/questions", token, obj("text", "x", "options", List.of("A"),
                "correctAnswers", List.of(0), "grade", "BLANCO"));
        HttpResult tooMany = post("/api/questions", token, obj("text", "x",
                "options", options(21), "correctAnswers", List.of(0), "grade", "BLANCO"));
        HttpResult outOfRange = post("/api/questions", token, obj("text", "x", "options", List.of("A", "B", "C"),
                "correctAnswers", List.of(3), "grade", "BLANCO"));
        HttpResult blankOption = post("/api/questions", token, obj("text", "x", "options", List.of("A", " "),
                "correctAnswers", List.of(0), "grade", "BLANCO"));

        assertThat(oneOption.expectStatus(400).message()).isEqualTo("options: debe tener entre 2 y 20 opciones");
        assertThat(tooMany.expectStatus(400).message()).isEqualTo("options: debe tener entre 2 y 20 opciones");
        assertThat(outOfRange.expectStatus(400).message()).isEqualTo("correctAnswersValid: debe señalar una o más opciones de la pregunta, sin repetir");
        assertThat(blankOption.expectStatus(400).message()).startsWith("options[1]");
    }

    @Test
    @DisplayName("Preguntas: sin máximo práctico de opciones (RF-09): 7 opciones valen")
    void createQuestion_manyOptions_created() {
        HttpResult result = post("/api/questions", adminToken(), obj("text", "¿Cuál es el séptimo?",
                "options", options(7), "correctAnswers", List.of(6), "grade", "AZUL")).expectStatus(201);

        assertThat((List<String>) result.read("$.options")).hasSize(7);
        assertThat((List<Integer>) result.read("$.correctAnswers")).containsExactly(6);
    }

    @Test
    @DisplayName("Preguntas: el grado y el tipo son opcionales; uno que no existe da 400 (RF-43, RF-44)")
    void createQuestion_gradeAndTypeOptional() {
        String token = adminToken();

        HttpResult noGrade = post("/api/questions", token, obj("text", "Sin grado", "options", FOUR_OPTIONS,
                "correctAnswers", List.of(0))).expectStatus(201);
        HttpResult badGrade = post("/api/questions", token, obj("text", "x", "options", FOUR_OPTIONS,
                "correctAnswers", List.of(0), "grade", "PURPURA"));
        HttpResult noType = post("/api/questions", token, obj("text", "Sin tipo", "options", FOUR_OPTIONS,
                "correctAnswers", List.of(0), "grade", "BLANCO")).expectStatus(201);
        HttpResult typeD = post("/api/questions", token, obj("text", "Tipo D", "options", FOUR_OPTIONS,
                "correctAnswers", List.of(0), "grade", "BLANCO", "type", "D")).expectStatus(201);
        HttpResult badType = post("/api/questions", token, obj("text", "x", "options", FOUR_OPTIONS,
                "correctAnswers", List.of(0), "grade", "BLANCO", "type", "E"));

        assertThat((Object) noGrade.read("$.grade")).isNull();
        badGrade.expectStatus(400);
        assertThat((Object) noType.read("$.type")).isNull();
        assertThat((String) typeD.read("$.type")).isEqualTo("D");
        badType.expectStatus(400);
    }

    @Test
    @DisplayName("Preguntas: usar un tag de otro profesor da 409 como si no existiera")
    void createQuestion_foreignTag_conflict() {
        Session owner = newTeacher();
        Session other = newTeacher();
        long foreignTag = createTag(owner.token(), unique("t"), "#000000");

        HttpResult result = post("/api/questions", other.token(), obj("text", "x", "options", FOUR_OPTIONS,
                "correctAnswers", List.of(0), "grade", "BLANCO", "tagIds", List.of(foreignTag)));

        assertThat(result.expectStatus(409).message()).isEqualTo("Alguno de los tags indicados no existe");
    }

    @Test
    @DisplayName("Preguntas: editar reemplaza todos los campos y los tags")
    void updateQuestion_replacesEverything() {
        Session teacher = newTeacher();
        long tagA = createTag(teacher.token(), unique("A"), "#000000");
        long tagB = createTag(teacher.token(), unique("B"), "#000000");
        long questionId = createQuestion(teacher.token(), "Original", FOUR_OPTIONS, 1, List.of(tagA));

        HttpResult result = put("/api/questions/" + questionId, teacher.token(), obj(
                "text", "Editada", "options", List.of("Sí", "No"), "correctAnswers", List.of(1),
                "explanation", null, "grade", "NEGRO", "tagIds", List.of(tagB))).expectStatus(200);

        assertThat((String) result.read("$.text")).isEqualTo("Editada");
        assertThat((List<String>) result.read("$.options")).containsExactly("Sí", "No");
        assertThat((Object) result.read("$.explanation")).isNull();
        assertThat((String) result.read("$.grade")).isEqualTo("NEGRO");
        assertThat((Object) result.read("$.type")).isNull();
        assertThat((List<Integer>) result.read("$.tags[*].id")).containsExactly((int) tagB);
    }

    @Test
    @DisplayName("Preguntas: una pregunta ajena no se ve, ni se edita, ni se borra (404)")
    void foreignQuestion_notFound() {
        Session owner = newTeacher();
        Session other = newTeacher();
        long questionId = createQuestion(owner.token(), "Ajena", FOUR_OPTIONS, 0, List.of());

        assertThat(get("/api/questions/" + questionId, other.token()).expectStatus(404).message())
                .isEqualTo("No existe una pregunta con id " + questionId);
        put("/api/questions/" + questionId, other.token(), obj("text", "x", "options", FOUR_OPTIONS,
                "correctAnswers", List.of(0), "grade", "BLANCO")).expectStatus(404);
        delete("/api/questions/" + questionId, other.token()).expectStatus(404);
    }

    @Test
    @DisplayName("Preguntas: el listado solo trae las del profesor")
    void listQuestions_onlyOwn() {
        Session teacher = newTeacher();
        Session other = newTeacher();
        List<Long> own = createQuestions(teacher.token(), 2);
        createQuestions(other.token(), 1);

        List<Map<String, Object>> questions = get("/api/questions", teacher.token()).expectStatus(200).asList();

        assertThat(questions).extracting(q -> ((Number) q.get("id")).longValue())
                .containsExactlyInAnyOrderElementsOf(own);
    }

    @Test
    @DisplayName("Preguntas: búsqueda por texto (sin mayúsculas) y por cualquiera de los tags")
    void searchQuestions_byTextAndTags() {
        Session teacher = newTeacher();
        long tagA = createTag(teacher.token(), unique("A"), "#000000");
        long tagB = createTag(teacher.token(), unique("B"), "#000000");
        long withA = createQuestion(teacher.token(), "Patada GIRATORIA", FOUR_OPTIONS, 0, List.of(tagA));
        long withB = createQuestion(teacher.token(), "Puño al peto", FOUR_OPTIONS, 0, List.of(tagB));
        long withoutTags = createQuestion(teacher.token(), "Patada frontal", FOUR_OPTIONS, 0, List.of());

        List<Map<String, Object>> byText = get("/api/questions/search?textContains=giratoria", teacher.token())
                .expectStatus(200).asList();
        List<Map<String, Object>> byTags = get("/api/questions/search?tagIds=" + tagA + "," + tagB, teacher.token())
                .expectStatus(200).asList();
        List<Map<String, Object>> byBoth = get("/api/questions/search?textContains=patada&tagIds=" + tagA,
                teacher.token()).expectStatus(200).asList();
        List<Map<String, Object>> noFilter = get("/api/questions/search", teacher.token()).expectStatus(200).asList();

        assertThat(ids(byText)).containsExactly(withA);
        assertThat(ids(byTags)).containsExactlyInAnyOrder(withA, withB);
        assertThat(ids(byBoth)).containsExactly(withA);
        assertThat(ids(noFilter)).containsExactlyInAnyOrder(withA, withB, withoutTags);
    }

    @Test
    @DisplayName("Preguntas: borrar una pregunta la quita de los borradores que la usaban")
    void deleteQuestion_usedInDraft_removedFromDraft() {
        Session teacher = newTeacher();
        List<Long> questionIds = createQuestions(teacher.token(), 2);
        long draftId = createDraft(teacher.token(), 5, null, true);
        setQuestions(teacher.token(), draftId, questionIds);

        delete("/api/questions/" + questionIds.get(0), teacher.token()).expectStatus(204);

        List<Integer> remaining = get("/api/exams/" + draftId, teacher.token()).expectStatus(200).read("$.questionIds");
        assertThat(remaining).containsExactly(questionIds.get(1).intValue());
        get("/api/questions/" + questionIds.get(0), teacher.token()).expectStatus(404);
    }

    @Test
    @DisplayName("Preguntas: no se puede borrar una pregunta de un examen publicado (409 con su título)")
    void deleteQuestion_usedInPublishedExam_conflict() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);
        String title = get("/api/exams/" + exam.id(), teacher.token()).read("$.title");

        HttpResult result = delete("/api/questions/" + exam.questionIds().get(0), teacher.token());

        assertThat(result.expectStatus(409).message()).isEqualTo("La pregunta se usa en exámenes ya publicados: " + title);
    }

    private static List<Long> ids(List<Map<String, Object>> items) {
        return items.stream().map(i -> ((Number) i.get("id")).longValue()).toList();
    }

    private static List<String> options(int n) {
        return java.util.stream.IntStream.rangeClosed(1, n).mapToObj(i -> "Opción " + i).toList();
    }
}
