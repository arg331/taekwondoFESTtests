package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: favoritos entre profesores")
class FavoritesCharacterizationTest extends ApiCharacterizationTest {

    @Test
    @DisplayName("Favoritos: marcar un examen público ajeno (204) y verlo en el listado")
    void addFavorite_foreignPublicExam_listed() {
        Session owner = newTeacher();
        Session fan = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);

        post("/api/exams/" + exam.id() + "/favorite", fan.token(), null).expectStatus(204);
        List<Map<String, Object>> favorites = get("/api/favorites", fan.token()).expectStatus(200).asList();

        assertThat(favorites).extracting(e -> ((Number) e.get("id")).longValue()).containsExactly(exam.id());
        assertThat(favorites.get(0).get("code")).isEqualTo(exam.code());
    }

    @Test
    @DisplayName("Favoritos: propio 409, repetido 409, privado ajeno 404")
    void addFavorite_rules() {
        Session owner = newTeacher();
        Session fan = newTeacher();
        PublishedExam publicExam = publishedExam(owner.token(), 5, null, true);
        long privateDraft = createDraft(owner.token(), 5, null, true);
        post("/api/exams/" + publicExam.id() + "/favorite", fan.token(), null).expectStatus(204);

        HttpResult own = post("/api/exams/" + publicExam.id() + "/favorite", owner.token(), null);
        HttpResult repeated = post("/api/exams/" + publicExam.id() + "/favorite", fan.token(), null);
        HttpResult foreignPrivate = post("/api/exams/" + privateDraft + "/favorite", fan.token(), null);

        assertThat(own.expectStatus(409).message()).isEqualTo("No puedes añadir a favoritos tus propios exámenes");
        assertThat(repeated.expectStatus(409).message()).isEqualTo("Este examen ya está en tus favoritos");
        assertThat(foreignPrivate.expectStatus(404).message()).isEqualTo("No existe un examen con id " + privateDraft);
    }

    @Test
    @DisplayName("Favoritos: si el examen pasa a privado deja de listarse; al volver a público reaparece")
    void favorite_hiddenWhilePrivate() {
        Session owner = newTeacher();
        Session fan = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        post("/api/exams/" + exam.id() + "/favorite", fan.token(), null).expectStatus(204);

        patch("/api/exams/" + exam.id() + "/visibility", owner.token(), obj("newVisibility", "PRIVATE"));
        List<Map<String, Object>> whilePrivate = get("/api/favorites", fan.token()).asList();
        patch("/api/exams/" + exam.id() + "/visibility", owner.token(), obj("newVisibility", "PUBLIC"));
        List<Map<String, Object>> backToPublic = get("/api/favorites", fan.token()).asList();

        assertThat(whilePrivate).isEmpty();
        assertThat(backToPublic).hasSize(1);
    }

    @Test
    @DisplayName("Favoritos: quitar (204); quitar uno que no estaba (404)")
    void removeFavorite() {
        Session owner = newTeacher();
        Session fan = newTeacher();
        PublishedExam exam = publishedExam(owner.token(), 5, null, true);
        post("/api/exams/" + exam.id() + "/favorite", fan.token(), null).expectStatus(204);

        delete("/api/exams/" + exam.id() + "/favorite", fan.token()).expectStatus(204);
        HttpResult again = delete("/api/exams/" + exam.id() + "/favorite", fan.token());

        assertThat(again.expectStatus(404).message()).isEqualTo("Este examen no está en tus favoritos");
        assertThat(get("/api/favorites", fan.token()).asList()).isEmpty();
    }
}
