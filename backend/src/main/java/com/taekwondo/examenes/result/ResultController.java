package com.taekwondo.examenes.result;

import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Entregas de exámenes.
 * POST es público (alumnos anónimos); /me es para cualquier usuario con sesión;
 * el resto solo para el profesor dueño del examen.
 */
@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;
    private final ResultReportService reportService;

    public ResultController(ResultService resultService, ResultReportService reportService) {
        this.resultService = resultService;
        this.reportService = reportService;
    }

    /** Pública: el token del intento dice quién la hace. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultResponse submit(@Valid @RequestBody SubmitExamRequest request) {
        return resultService.submit(request);
    }

    @GetMapping("/me")
    public List<ResultResponse> listMine(@AuthenticationPrincipal Long userId) {
        return resultService.listMine(userId);
    }

    @GetMapping("/exam/{examId}")
    public List<ResultResponse> listByExam(@PathVariable Long examId, @AuthenticationPrincipal Long userId) {
        return resultService.listByExam(examId, userId);
    }

    /** Acierto de cada pregunta del examen (RF-28). */
    @GetMapping("/exam/{examId}/questions")
    public List<QuestionStatsResponse> questionStats(@PathVariable Long examId, @AuthenticationPrincipal Long userId) {
        return reportService.questionStats(examId, userId);
    }

    /** Resultados de un usuario (RF-55): el profesor, los de sus exámenes; el administrador, todos. */
    @GetMapping("/user/{studentUserId}")
    public UserResultsResponse userResults(@PathVariable Long studentUserId, @AuthenticationPrincipal Long userId) {
        return reportService.userResults(studentUserId, userId);
    }

    @GetMapping("/exam/{examId}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long examId, @AuthenticationPrincipal Long userId) {
        ExportedFile file = resultService.exportByExam(examId, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.name()).build().toString())
                .body(file.content());
    }

    @GetMapping("/exam/{examId}/statistics")
    public ExamStatisticsResponse statistics(@PathVariable Long examId, @AuthenticationPrincipal Long userId) {
        return resultService.statistics(examId, userId);
    }
}
