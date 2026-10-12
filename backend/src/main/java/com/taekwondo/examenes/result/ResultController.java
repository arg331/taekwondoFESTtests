package com.taekwondo.examenes.result;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /** userId es null si el alumno no ha iniciado sesión. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultResponse submit(@Valid @RequestBody SubmitExamRequest request, @AuthenticationPrincipal Long userId) {
        return resultService.submit(request, userId);
    }

    @GetMapping("/me")
    public List<ResultResponse> listMine(@AuthenticationPrincipal Long userId) {
        return resultService.listMine(userId);
    }

    @GetMapping("/{id}")
    public ResultResponse get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return resultService.get(id, userId);
    }

    @GetMapping("/exam/{examId}")
    public List<ResultResponse> listByExam(@PathVariable Long examId, @AuthenticationPrincipal Long userId) {
        return resultService.listByExam(examId, userId);
    }

    @GetMapping("/exam/{examId}/statistics")
    public ExamStatisticsResponse statistics(@PathVariable Long examId, @AuthenticationPrincipal Long userId) {
        return resultService.statistics(examId, userId);
    }
}
