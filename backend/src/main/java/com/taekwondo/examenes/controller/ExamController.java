package com.taekwondo.examenes.controller;

import com.taekwondo.examenes.dto.exam.*;
import com.taekwondo.examenes.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exámenes. Todo es del profesor (ADMIN) salvo:
 * <ul>
 *   <li>GET /public: cualquier usuario con sesión.</li>
 *   <li>GET /by-code/{code} y POST /by-code/{code}/attempts: públicos, flujo del alumno desde el QR.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    // ───── Creación y edición ─────

    @PostMapping("/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse createDraft(@Valid @RequestBody ExamDraftRequest request, @AuthenticationPrincipal Long userId) {
        return examService.createDraft(request, userId);
    }

    @PostMapping("/drafts/pre-generated")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse preGenerateDraft(@Valid @RequestBody ExamDraftRequest request,
                                         @AuthenticationPrincipal Long userId) {
        return examService.preGenerateDraft(request, userId);
    }

    @PatchMapping("/{id}/title")
    public ExamResponse rename(@PathVariable Long id, @Valid @RequestBody RenameExamRequest request,
                               @AuthenticationPrincipal Long userId) {
        return examService.rename(id, request.newTitle(), userId);
    }

    @PatchMapping("/{id}/config")
    public ExamResponse changeConfig(@PathVariable Long id, @Valid @RequestBody ExamConfigRequest request,
                                     @AuthenticationPrincipal Long userId) {
        return examService.changeConfig(id, request, userId);
    }

    @PutMapping("/{id}/questions")
    public ExamResponse updateQuestions(@PathVariable Long id, @Valid @RequestBody UpdateExamQuestionsRequest request,
                                        @AuthenticationPrincipal Long userId) {
        return examService.updateQuestions(id, request.questionIds(), userId);
    }

    @PatchMapping("/{id}/visibility")
    public ExamResponse changeVisibility(@PathVariable Long id, @Valid @RequestBody ChangeExamVisibilityRequest request,
                                         @AuthenticationPrincipal Long userId) {
        return examService.changeVisibility(id, request.newVisibility(), userId);
    }

    // ───── Ciclo de vida ─────

    @PostMapping("/{id}/publish")
    public ExamResponse publish(@PathVariable Long id, @Valid @RequestBody PublishExamRequest request,
                                @AuthenticationPrincipal Long userId) {
        return examService.publish(id, request, userId);
    }

    @PostMapping("/{id}/close")
    public ExamResponse close(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return examService.close(id, userId);
    }

    @PostMapping("/{id}/reopen")
    public ExamResponse reopen(@PathVariable Long id, @RequestBody ExpirationRequest request,
                               @AuthenticationPrincipal Long userId) {
        return examService.reopen(id, request.newExpiresAt(), userId);
    }

    @PatchMapping("/{id}/expiration")
    public ExamResponse changeExpiration(@PathVariable Long id, @RequestBody ExpirationRequest request,
                                         @AuthenticationPrincipal Long userId) {
        return examService.changeExpiration(id, request.newExpiresAt(), userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        examService.deleteDraft(id, userId);
    }

    // ───── Consultas ─────

    @GetMapping("/{id}")
    public ExamResponse get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return examService.get(id, userId);
    }

    @GetMapping("/mine")
    public List<ExamResponse> listMine(@AuthenticationPrincipal Long userId) {
        return examService.listMine(userId);
    }

    @GetMapping("/public")
    public List<ExamResponse> listPublic(@AuthenticationPrincipal Long userId) {
        return examService.listPublic(userId);
    }

    // ───── Flujo del alumno (público) ─────

    @GetMapping("/by-code/{code}")
    public ExamResponse getByCode(@PathVariable String code) {
        return examService.getAccessibleByCode(code);
    }

    /** userId es null si el alumno no ha iniciado sesión. */
    @PostMapping("/by-code/{code}/attempts")
    public ExamAttemptResponse startAttempt(@PathVariable String code, @AuthenticationPrincipal Long userId) {
        return examService.startAttempt(code, userId);
    }
}
