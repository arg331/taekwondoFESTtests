package com.taekwondo.examenes.result;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Hoja de cálculo (.xlsx) con los resultados de un examen: una fila por entrega,
 * ordenadas por club y alumno, y las entregas sin club al final (RF-27).
 */
final class ResultSpreadsheet {

    static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String SHEET_NAME = "Resultados";
    private static final String NO_CLUB = "Sin club";
    private static final List<String> HEADERS = List.of("Club", "Alumno", "Email", "Nota", "Aciertos", "Preguntas",
            "Aprobado", "Tiempo", "Fecha");
    /** Anchos de columna en caracteres, en el orden de HEADERS. */
    private static final int[] COLUMN_WIDTHS = {28, 30, 30, 8, 10, 11, 10, 9, 18};
    private static final String DATE_FORMAT = "dd/mm/yyyy hh:mm";

    private ResultSpreadsheet() {
    }

    static byte[] build(List<Result> results) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeHeader(workbook, sheet);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(DATE_FORMAT));

            List<Result> sorted = results.stream().sorted(byClubThenStudent()).toList();
            for (int i = 0; i < sorted.size(); i++) {
                writeRow(sheet.createRow(i + 1), sorted.get(i), dateStyle);
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(sorted.size(), 1), 0, HEADERS.size() - 1));
            for (int col = 0; col < COLUMN_WIDTHS.length; col++) {
                sheet.setColumnWidth(col, COLUMN_WIDTHS[col] * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo generar el Excel de resultados", ex);
        }
    }

    /** Orden alfabético en español (ignora mayúsculas y tildes); sin club, al final. */
    private static Comparator<Result> byClubThenStudent() {
        Collator spanish = Collator.getInstance(Locale.forLanguageTag("es"));
        spanish.setStrength(Collator.PRIMARY);
        return Comparator.comparing((Result r) -> club(r) == null)
                .thenComparing(r -> club(r) == null ? "" : club(r), spanish)
                .thenComparing(Result::getStudentName, spanish)
                .thenComparing(Result::getCompletedAt);
    }

    private static String club(Result result) {
        String club = result.getStudentClub();
        return club == null || club.isBlank() ? null : club.trim();
    }

    private static void writeHeader(XSSFWorkbook workbook, Sheet sheet) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(bold);
        Row header = sheet.createRow(0);
        for (int col = 0; col < HEADERS.size(); col++) {
            Cell cell = header.createCell(col);
            cell.setCellValue(HEADERS.get(col));
            cell.setCellStyle(headerStyle);
        }
    }

    private static void writeRow(Row row, Result result, CellStyle dateStyle) {
        String club = club(result);
        row.createCell(0).setCellValue(club == null ? NO_CLUB : club);
        row.createCell(1).setCellValue(result.getStudentName());
        row.createCell(2).setCellValue(result.getStudentEmail() == null ? "" : result.getStudentEmail());
        row.createCell(3).setCellValue(result.getScore());
        row.createCell(4).setCellValue(result.getCorrectAnswers());
        row.createCell(5).setCellValue(result.getTotalQuestions());
        row.createCell(6).setCellValue(result.isPassed() ? "Sí" : "No");
        row.createCell(7).setCellValue(formatDuration(result.getTimeSpentSeconds()));
        Cell date = row.createCell(8);
        date.setCellValue(result.getCompletedAt());
        date.setCellStyle(dateStyle);
    }

    /** 125 segundos → «2:05». */
    private static String formatDuration(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
