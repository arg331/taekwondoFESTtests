package com.taekwondo.examenes.infrastructure.web.controller;

import com.taekwondo.examenes.application.result.*;
import com.taekwondo.examenes.application.result.dto.AnswerSubmission;
import com.taekwondo.examenes.application.result.dto.ExamStatisticsView;
import com.taekwondo.examenes.application.result.dto.ResultView;
import com.taekwondo.examenes.application.result.dto.SubmitExamInput;
import com.taekwondo.examenes.infrastructure.security.AuthenticatedUser;
import com.taekwondo.examenes.infrastructure.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para Result.
 *
 * Endpoints:
 *  - POST /api/results                                 (PÚBLICO)
 *  - GET  /api/results/{id}                            (autenticado, profesor dueño del examen)
 *  - GET  /api/results/exam/{examId}                   (autenticado, profesor dueño del examen)
 *  - GET  /api/results/exam/{examId}/statistics        (autenticado, profesor dueño del examen)
 *  - GET  /api/results/me                              (autenticado, estudiante)
 *
 * POST /api/results es público porque el estudiante puede ser anónimo
 * en exámenes OPEN. Si hay JWT, el resultado se asocia al usuario;
 * si no, queda como anónimo. El caso de uso valida si el examen es
 * REGISTERED_ONLY y rechaza si studentUserId es null en ese caso.
 */
@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final SubmitExamUseCase submitExamUseCase;
    private final GetResultUseCase getResultUseCase;
    private final ListResultsByExamUseCase listResultsByExamUseCase;
    private final GetExamStatisticsUseCase getExamStatisticsUseCase;
    private final ListMyAttemptsUseCase listMyAttemptsUseCase;

    public ResultController(SubmitExamUseCase submitExamUseCase,
                             GetResultUseCase getResultUseCase,
                             ListResultsByExamUseCase listResultsByExamUseCase,
                             GetExamStatisticsUseCase getExamStatisticsUseCase,
                             ListMyAttemptsUseCase listMyAttemptsUseCase) {
        this.submitExamUseCase = submitExamUseCase;
        this.getResultUseCase = getResultUseCase;
        this.listResultsByExamUseCase = listResultsByExamUseCase;
        this.getExamStatisticsUseCase = getExamStatisticsUseCase;
        this.listMyAttemptsUseCase = listMyAttemptsUseCase;
    }

    // ───── Submit (público) ─────

    @PostMapping
    public ResponseEntity<ResultResponse> submit(@RequestBody SubmitExamRequest request) {
        List<AnswerSubmission> answers = request.answers().stream()
                .map(a -> new AnswerSubmission(a.questionId(), a.chosenOption()))
                .toList();

        SubmitExamInput input = new SubmitExamInput(
                request.examCode(),
                request.studentName(),
                request.studentClub(),
                request.studentEmail(),
                answers,
                request.timeSpentSeconds()
        );

        Long studentUserId = AuthenticatedUser.currentUserIdOrNull();
        ResultView view = submitExamUseCase.execute(input, studentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResultResponse.from(view));
    }

    // ───── Consultas autenticadas ─────

    @GetMapping("/{id}")
    public ResultResponse getResult(@PathVariable Long id) {
        ResultView view = getResultUseCase.execute(id, AuthenticatedUser.currentUserId());
        return ResultResponse.from(view);
    }

    @GetMapping("/exam/{examId}")
    public List<ResultResponse> listResultsByExam(@PathVariable Long examId) {
        return listResultsByExamUseCase
                .execute(examId, AuthenticatedUser.currentUserId())
                .stream()
                .map(ResultResponse::from)
                .toList();
    }

    @GetMapping("/exam/{examId}/statistics")
    public ExamStatisticsResponse getStatistics(@PathVariable Long examId) {
        ExamStatisticsView view = getExamStatisticsUseCase
                .execute(examId, AuthenticatedUser.currentUserId());
        return ExamStatisticsResponse.from(view);
    }

    @GetMapping("/me")
    public List<ResultResponse> listMyAttempts() {
        return listMyAttemptsUseCase
                .execute(AuthenticatedUser.currentUserId())
                .stream()
                .map(ResultResponse::from)
                .toList();
    }
}
