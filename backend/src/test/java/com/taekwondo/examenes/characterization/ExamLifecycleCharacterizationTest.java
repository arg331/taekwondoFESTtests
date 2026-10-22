package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: creación, edición y ciclo de vida de exámenes")
class ExamLifecycleCharacterizationTest extends ApiCharacterizationTest {

    private static final List<String> EXAM_FIELDS = List.of("id", "title", "ownerId", "status", "visibility",
            "accessMode", "config", "questionIds", "generationTags", "code", "createdAt", "expiresAt");
    private static final List<String> CONFIG_FIELDS = List.of("numberOfQuestions", "timeLimitMinutes",
            "showScore", "randomizeOptions", "randomizeQuestionOrder");

    // ───── Creación ─────

    @Test
    @DisplayName("Borrador: se crea en DRAFT, privado, abierto, sin código ni preguntas")
    void createDraft_defaults() {
        Session teacher = newTeacher();

        HttpResult result = post("/api/exams/drafts", teacher.token(),
                draftBody("  Examen de prueba  ", 10, 30, true, true, false)).expectStatus(201);

        assertThat(result.asMap()).containsOnlyKeys(EXAM_FIELDS);
        assertThat((Map<String, Object>) result.read("$.config")).containsOnlyKeys(CONFIG_FIELDS);
        assertThat((String) result.read("$.title")).isEqualTo("Examen de prueba");
        assertThat((Integer) result.read("$.ownerId")).isEqualTo((int) teacher.id());
        assertThat((String) result.read("$.status")).isEqualTo("DRAFT");
        assertThat((String) result.read("$.visibility")).isEqualTo("PRIVATE");
        assertThat((String) result.read("$.accessMode")).isEqualTo("OPEN");
        assertThat((Object) result.read("$.code")).isNull();
        assertThat((Object) result.read("$.expiresAt")).isNull();
        assertThat((List<?>) result.read("$.questionIds")).isEmpty();
        assertThat((List<?>) result.read("$.generationTags")).isEmpty();
        assertThat((Map<String, Object>) result.read("$.config")).containsExactlyInAnyOrderEntriesOf(Map.of(
                "numberOfQuestions", 10, "timeLimitMinutes", 30, "showScore", true,
                "randomizeOptions", true, "randomizeQuestionOrder", false));
    }

    @Test
    @DisplayName("Borrador: tiempo límite 0 se guarda tal cual (0, no null)")
    void createDraft_zeroTimeLimit_storedAsZero() {
        HttpResult result = post("/api/exams/drafts", adminToken(),
                draftBody(unique("E"), 5, 0, true, false, false)).expectStatus(201);

        assertThat((Integer) result.read("$.config.timeLimitMinutes")).isZero();
    }

    @ParameterizedTest(name = "preguntas={0}, minutos={1}, título=''{2}''")
    @CsvSource({
            "4, 10, Válido, numberOfQuestions",
            "51, 10, Válido, numberOfQuestions",
            "10, 181, Válido, timeLimitMinutes",
            "10, -1, Válido, timeLimitMinutes",
            "10, 10, ' ', title"
    })
    @DisplayName("Borrador: límites de configuración devuelven 400")
    void createDraft_outOfRange_badRequest(int questions, int minutes, String title, String field) {
        HttpResult result = post("/api/exams/drafts", adminToken(),
                draftBody(title, questions, minutes, true, false, false));

        assertThat(result.expectStatus(400).message()).startsWith(field + ":");
    }

    @Test
    @DisplayName("Generar aleatorio: elige N preguntas propias, filtradas por tags, y guarda los tags usados")
    void preGenerate_picksOwnTaggedQuestions() {
        Session teacher = newTeacher();
        long tag = createTag(teacher.token(), unique("Combate"), "#000000");
        List<Long> tagged = List.of(
                createQuestion(teacher.token(), "T1", FOUR_OPTIONS, 0, List.of(tag)),
                createQuestion(teacher.token(), "T2", FOUR_OPTIONS, 0, List.of(tag)),
                createQuestion(teacher.token(), "T3", FOUR_OPTIONS, 0, List.of(tag)),
                createQuestion(teacher.token(), "T4", FOUR_OPTIONS, 0, List.of(tag)),
                createQuestion(teacher.token(), "T5", FOUR_OPTIONS, 0, List.of(tag)),
                createQuestion(teacher.token(), "T6", FOUR_OPTIONS, 0, List.of(tag)));
        createQuestions(teacher.token(), 3);

        Map<String, Object> body = draftBody(unique("Aleatorio"), 5, null, true, true, true);
        body.put("requiredAnyOfTagIds", List.of(tag));
        HttpResult result = post("/api/exams/drafts/pre-generated", teacher.token(), body).expectStatus(201);

        List<Integer> chosen = result.read("$.questionIds");
        assertThat(chosen).hasSize(5).doesNotHaveDuplicates()
                .allMatch(id -> tagged.contains(id.longValue()));
        assertThat((String) result.read("$.status")).isEqualTo("DRAFT");
        assertThat((List<Integer>) result.read("$.generationTags[*].id")).containsExactly((int) tag);
    }

    @Test
    @DisplayName("Generar aleatorio: sin preguntas suficientes devuelve 409 con el recuento")
    void preGenerate_notEnoughQuestions_conflict() {
        Session teacher = newTeacher();
        createQuestions(teacher.token(), 3);

        HttpResult result = post("/api/exams/drafts/pre-generated", teacher.token(),
                draftBody(unique("E"), 5, null, true, false, false));

        assertThat(result.expectStatus(409).message())
                .isEqualTo("No hay suficientes preguntas con los tags solicitados: se necesitan 5 y solo hay 3");
    }

    // ───── Edición ─────

    @Test
    @DisplayName("Edición de borrador: título, configuración y preguntas")
    void editDraft_titleConfigAndQuestions() {
        Session teacher = newTeacher();
        List<Long> questionIds = createQuestions(teacher.token(), 5);
        long examId = createDraft(teacher.token(), 5, null, true);

        HttpResult renamed = patch("/api/exams/" + examId + "/title", teacher.token(), obj("newTitle", "  Nuevo  "));
        HttpResult reconfigured = patch("/api/exams/" + examId + "/config", teacher.token(), obj(
                "numberOfQuestions", 6, "timeLimitMinutes", 20, "showScore", false,
                "randomizeOptions", true, "randomizeQuestionOrder", true));
        HttpResult withQuestions = put("/api/exams/" + examId + "/questions", teacher.token(),
                obj("questionIds", questionIds));

        assertThat((String) renamed.expectStatus(200).read("$.title")).isEqualTo("Nuevo");
        assertThat((Integer) reconfigured.expectStatus(200).read("$.config.numberOfQuestions")).isEqualTo(6);
        assertThat((List<Integer>) withQuestions.expectStatus(200).read("$.questionIds"))
                .containsExactlyElementsOf(questionIds.stream().map(Long::intValue).toList());
    }

    @Test
    @DisplayName("Edición de preguntas: duplicadas o ajenas devuelven 409")
    void updateQuestions_duplicatesOrForeign_conflict() {
        Session teacher = newTeacher();
        Session other = newTeacher();
        long own = createQuestions(teacher.token(), 1).get(0);
        long foreign = createQuestions(other.token(), 1).get(0);
        long examId = createDraft(teacher.token(), 5, null, true);

        HttpResult duplicated = put("/api/exams/" + examId + "/questions", teacher.token(),
                obj("questionIds", List.of(own, own)));
        HttpResult withForeign = put("/api/exams/" + examId + "/questions", teacher.token(),
                obj("questionIds", List.of(own, foreign)));

        assertThat(duplicated.expectStatus(409).message()).isEqualTo("Hay preguntas duplicadas en la lista");
        assertThat(withForeign.expectStatus(409).message()).isEqualTo("Alguna de las preguntas indicadas no existe");
    }

    @Test
    @DisplayName("Examen publicado: el título se puede cambiar, la configuración y las preguntas no")
    void publishedExam_onlyTitleEditable() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);

        HttpResult rename = patch("/api/exams/" + exam.id() + "/title", teacher.token(), obj("newTitle", "Otro"));
        HttpResult config = patch("/api/exams/" + exam.id() + "/config", teacher.token(), obj(
                "numberOfQuestions", 5, "timeLimitMinutes", null, "showScore", false,
                "randomizeOptions", false, "randomizeQuestionOrder", false));
        HttpResult questions = put("/api/exams/" + exam.id() + "/questions", teacher.token(),
                obj("questionIds", exam.questionIds()));

        rename.expectStatus(200);
        assertThat(config.expectStatus(409).message())
                .isEqualTo("No se puede cambiar la configuración: el examen ya no está en borrador (estado actual: PUBLISHED)");
        assertThat(questions.expectStatus(409).message())
                .isEqualTo("No se puede cambiar las preguntas: el examen ya no está en borrador (estado actual: PUBLISHED)");
    }

    @Test
    @DisplayName("Un examen ajeno responde como inexistente (404) en todas las operaciones")
    void foreignExam_notFound() {
        Session owner = newTeacher();
        Session other = newTeacher();
        long examId = createDraft(owner.token(), 5, null, true);

        assertThat(get("/api/exams/" + examId, other.token()).expectStatus(404).message())
                .isEqualTo("No existe un examen con id " + examId);
        patch("/api/exams/" + examId + "/title", other.token(), obj("newTitle", "x")).expectStatus(404);
        delete("/api/exams/" + examId, other.token()).expectStatus(404);
        post("/api/exams/" + examId + "/publish", other.token(), obj("visibility", "PUBLIC")).expectStatus(404);
    }

    // ───── Publicación y estados ─────

    @Test
    @DisplayName("Publicar: sin fecha expira en 1 h, genera código EXM-XXXXXXXX y acceso OPEN por defecto")
    void publish_defaults() {
        Session teacher = newTeacher();
        List<Long> questionIds = createQuestions(teacher.token(), 5);
        long examId = createDraft(teacher.token(), 5, null, true);
        setQuestions(teacher.token(), examId, questionIds);

        HttpResult result = publish(teacher.token(), examId, "PRIVATE", null, null).expectStatus(200);

        assertThat((String) result.read("$.status")).isEqualTo("PUBLISHED");
        assertThat((String) result.read("$.visibility")).isEqualTo("PRIVATE");
        assertThat((String) result.read("$.accessMode")).isEqualTo("OPEN");
        assertThat((String) result.read("$.code")).matches("EXM-[0-9A-F]{8}");
        assertThat(LocalDateTime.parse(result.read("$.expiresAt"))).isEqualTo(now().plusHours(1));
    }

    @Test
    @DisplayName("Publicar: con un número de preguntas distinto al configurado devuelve 409")
    void publish_wrongQuestionCount_conflict() {
        Session teacher = newTeacher();
        long examId = createDraft(teacher.token(), 5, null, true);
        setQuestions(teacher.token(), examId, createQuestions(teacher.token(), 4));

        HttpResult result = publish(teacher.token(), examId, "PUBLIC", null, null);

        assertThat(result.expectStatus(409).message())
                .isEqualTo("El examen debe tener 5 preguntas para publicarse (tiene 4)");
    }

    @Test
    @DisplayName("Publicar: fecha de expiración en el pasado devuelve 409; sin visibilidad, 400")
    void publish_pastExpirationOrMissingVisibility() {
        Session teacher = newTeacher();
        long examId = createDraft(teacher.token(), 5, null, true);
        setQuestions(teacher.token(), examId, createQuestions(teacher.token(), 5));

        HttpResult past = publish(teacher.token(), examId, "PUBLIC", null, now().minusMinutes(1));
        HttpResult noVisibility = post("/api/exams/" + examId + "/publish", teacher.token(), obj("visibility", null));

        assertThat(past.expectStatus(409).message())
                .isEqualTo("La fecha de expiración debe ser posterior al momento actual");
        assertThat(noVisibility.expectStatus(400).message()).startsWith("visibility:");
    }

    @Test
    @DisplayName("Publicar dos veces devuelve 409")
    void publish_twice_conflict() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);

        HttpResult result = publish(teacher.token(), exam.id(), "PUBLIC", null, null);

        assertThat(result.expectStatus(409).message())
                .isEqualTo("No se puede publicar: el examen ya no está en borrador (estado actual: PUBLISHED)");
    }

    @Test
    @DisplayName("Cerrar, reabrir y cambiar plazo siguen la máquina de estados")
    void closeReopenAndExtend_stateMachine() {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);
        String base = "/api/exams/" + exam.id();
        LocalDateTime later = now().plusDays(2);

        HttpResult extended = patch(base + "/expiration", teacher.token(), obj("newExpiresAt", later.toString()));
        HttpResult reopenWhilePublished = post(base + "/reopen", teacher.token(), obj("newExpiresAt", null));
        HttpResult closed = post(base + "/close", teacher.token(), null);
        HttpResult closeAgain = post(base + "/close", teacher.token(), null);
        HttpResult extendWhileClosed = patch(base + "/expiration", teacher.token(), obj("newExpiresAt", null));
        HttpResult reopened = post(base + "/reopen", teacher.token(), obj("newExpiresAt", null));

        assertThat(LocalDateTime.parse(extended.expectStatus(200).read("$.expiresAt"))).isEqualTo(later);
        assertThat(reopenWhilePublished.expectStatus(409).message())
                .isEqualTo("Solo se pueden reabrir exámenes cerrados (estado actual: PUBLISHED)");
        assertThat((String) closed.expectStatus(200).read("$.status")).isEqualTo("EXPIRED");
        assertThat(closeAgain.expectStatus(409).message())
                .isEqualTo("Solo se pueden cerrar exámenes publicados (estado actual: EXPIRED)");
        assertThat(extendWhileClosed.expectStatus(409).message())
                .isEqualTo("Solo se puede cambiar la expiración de exámenes publicados (estado actual: EXPIRED)");
        assertThat((String) reopened.expectStatus(200).read("$.status")).isEqualTo("PUBLISHED");
        assertThat((Object) reopened.read("$.expiresAt")).isNull();
        assertThat((String) reopened.read("$.code")).isEqualTo(exam.code());
    }

    @Test
    @DisplayName("Cerrar un borrador o reabrir con fecha pasada devuelven 409")
    void closeDraftOrReopenInPast_conflict() {
        Session teacher = newTeacher();
        long draftId = createDraft(teacher.token(), 5, null, true);
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);
        post("/api/exams/" + exam.id() + "/close", teacher.token(), null).expectStatus(200);

        HttpResult closeDraft = post("/api/exams/" + draftId + "/close", teacher.token(), null);
        HttpResult reopenPast = post("/api/exams/" + exam.id() + "/reopen", teacher.token(),
                obj("newExpiresAt", now().minusHours(1).toString()));

        assertThat(closeDraft.expectStatus(409).message())
                .isEqualTo("Solo se pueden cerrar exámenes publicados (estado actual: DRAFT)");
        assertThat(reopenPast.expectStatus(409).message())
                .isEqualTo("La fecha de expiración debe ser posterior al momento actual");
    }

    @Test
    @DisplayName("Visibilidad: no aplica a borradores (409); en publicados cambia")
    void changeVisibility_rules() {
        Session teacher = newTeacher();
        long draftId = createDraft(teacher.token(), 5, null, true);
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);

        HttpResult onDraft = patch("/api/exams/" + draftId + "/visibility", teacher.token(),
                obj("newVisibility", "PUBLIC"));
        HttpResult onPublished = patch("/api/exams/" + exam.id() + "/visibility", teacher.token(),
                obj("newVisibility", "PRIVATE"));

        assertThat(onDraft.expectStatus(409).message()).isEqualTo("Un borrador no tiene visibilidad pública");
        assertThat((String) onPublished.expectStatus(200).read("$.visibility")).isEqualTo("PRIVATE");
    }

    @Test
    @DisplayName("Borrar: solo borradores (204); un publicado devuelve 409")
    void deleteExam_onlyDrafts() {
        Session teacher = newTeacher();
        long draftId = createDraft(teacher.token(), 5, null, true);
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);

        delete("/api/exams/" + draftId, teacher.token()).expectStatus(204);
        HttpResult deletePublished = delete("/api/exams/" + exam.id(), teacher.token());

        get("/api/exams/" + draftId, teacher.token()).expectStatus(404);
        assertThat(deletePublished.expectStatus(409).message())
                .isEqualTo("Solo se pueden eliminar exámenes en borrador (estado actual: PUBLISHED)");
    }

    // ───── Listados ─────

    @Test
    @DisplayName("Mis exámenes: solo los propios, del más reciente al más antiguo")
    void listMine_ownNewestFirst() {
        Session teacher = newTeacher();
        Session other = newTeacher();
        long first = createDraft(teacher.token(), 5, null, true);
        long second = createDraft(teacher.token(), 5, null, true);
        createDraft(other.token(), 5, null, true);

        List<Map<String, Object>> mine = get("/api/exams/mine", teacher.token()).expectStatus(200).asList();

        assertThat(mine).extracting(e -> ((Number) e.get("id")).longValue()).containsExactly(second, first);
    }

    @Test
    @DisplayName("Públicos: solo publicados, públicos, abiertos ahora y de otros profesores")
    void listPublic_filters() {
        Session teacher = newTeacher();
        Session viewer = newTeacher();
        PublishedExam open = publishedExam(teacher.token(), 5, null, true);
        PublishedExam privateExam = publishedExam(teacher.token(), 5, null, true);
        patch("/api/exams/" + privateExam.id() + "/visibility", teacher.token(), obj("newVisibility", "PRIVATE"));
        PublishedExam closed = publishedExam(teacher.token(), 5, null, true);
        post("/api/exams/" + closed.id() + "/close", teacher.token(), null);
        long draft = createDraft(teacher.token(), 5, null, true);
        PublishedExam ownPublic = publishedExam(viewer.token(), 5, null, true);

        List<Long> visible = get("/api/exams/public", viewer.token()).expectStatus(200).asList().stream()
                .map(e -> ((Number) e.get("id")).longValue()).toList();

        assertThat(visible).contains(open.id())
                .doesNotContain(privateExam.id(), closed.id(), draft, ownPublic.id());
    }

    @Test
    @DisplayName("Públicos: un examen cuya fecha de expiración ha pasado deja de listarse (sigue PUBLISHED)")
    void listPublic_expiredByTime_hidden() {
        Session teacher = newTeacher();
        Session viewer = newStudent();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);

        clock.advance(Duration.ofHours(2));

        List<Long> visible = get("/api/exams/public", viewer.token()).asList().stream()
                .map(e -> ((Number) e.get("id")).longValue()).toList();
        assertThat(visible).doesNotContain(exam.id());
        assertThat((String) get("/api/exams/" + exam.id(), teacher.token()).read("$.status")).isEqualTo("PUBLISHED");
    }
}
