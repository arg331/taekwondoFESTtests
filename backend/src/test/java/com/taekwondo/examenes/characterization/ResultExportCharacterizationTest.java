package com.taekwondo.examenes.characterization;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Caracterización: exportar resultados a Excel")
class ResultExportCharacterizationTest extends ApiCharacterizationTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Test
    @DisplayName("Exportar: una fila por entrega, ordenadas por club y alumno; sin club, al final (RF-27)")
    void export_sortedByClub() throws IOException {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);
        submitAs(exam, "Zoe", "Club Badajoz", allCorrect(5));
        submitAs(exam, "Ana", null, allCorrect(5));
        submitAs(exam, "Luis", "Ávila Taekwondo", wrongFrom(5, 2));
        submitAs(exam, "Bea", "Club Badajoz", allCorrect(5));

        MockHttpServletResponse response = download("/api/results/exam/" + exam.id() + "/export", teacher.token());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo(XLSX);
        assertThat(response.getHeader("Content-Disposition")).startsWith("attachment").endsWith(".xlsx\"");
        List<List<String>> rows = rows(response.getContentAsByteArray());
        assertThat(rows.get(0)).containsExactly("Club", "Alumno", "Email", "Nota", "Aciertos", "Preguntas",
                "Aprobado", "Tiempo", "Fecha");
        assertThat(rows.subList(1, rows.size())).extracting(r -> r.get(0) + " / " + r.get(1)).containsExactly(
                "Ávila Taekwondo / Luis", "Club Badajoz / Bea", "Club Badajoz / Zoe", "Sin club / Ana");
        assertThat(rows.get(1).subList(3, 7)).containsExactly("40", "2", "5", "No");
        assertThat(rows.get(2).subList(3, 7)).containsExactly("100", "5", "5", "Sí");
    }

    @Test
    @DisplayName("Exportar: sin entregas devuelve un Excel solo con la cabecera")
    void export_noResults_onlyHeader() throws IOException {
        Session teacher = newTeacher();
        PublishedExam exam = publishedExam(teacher.token(), 5, null, true);

        MockHttpServletResponse response = download("/api/results/exam/" + exam.id() + "/export", teacher.token());

        assertThat(rows(response.getContentAsByteArray())).hasSize(1);
    }

    @Test
    @DisplayName("Exportar: los resultados de un examen ajeno no se pueden exportar (404)")
    void export_foreignExam_notFound() {
        PublishedExam foreign = publishedExam(newTeacher().token(), 5, null, true);

        MockHttpServletResponse response = download("/api/results/exam/" + foreign.id() + "/export", newTeacher().token());

        assertThat(response.getStatus()).isEqualTo(404);
    }

    private void submitAs(PublishedExam exam, String name, String club, List<Integer> chosen) {
        post("/api/results", null, obj(
                "examCode", exam.code(),
                "attemptToken", startAttempt(exam.code(), null),
                "studentName", name,
                "studentClub", club,
                "studentEmail", null,
                "answers", answers(exam.questionIds(), chosen))).expectStatus(201);
    }

    /** Acierta las primeras «right» preguntas y falla el resto. */
    private static List<Integer> wrongFrom(int n, int right) {
        List<Integer> chosen = new ArrayList<>(allCorrect(n));
        for (int i = right; i < n; i++) {
            chosen.set(i, (chosen.get(i) + 1) % 4);
        }
        return chosen;
    }

    private static List<List<String>> rows(byte[] xlsx) throws IOException {
        DataFormatter formatter = new DataFormatter();
        List<List<String>> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    cells.add(formatter.formatCellValue(cell));
                }
                rows.add(cells);
            }
        }
        return rows;
    }
}
