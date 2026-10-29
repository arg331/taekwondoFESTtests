package com.taekwondo.examenes.exam;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exámenes. Todo es del profesor (ADMIN) salvo:
 * <ul>
 *   <li>GET /public: cualquier usuario con sesión.</li>
 *   <li>GET /by-code/{code} y /by-code/{code}/attempts/**: públicos, flujo del alumno desde el QR.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;
    private final AttemptService attemptService;

    public ExamController(ExamService examService, AttemptService attemptService) {
        this.examService = examService;
        this.attemptService = attemptService;
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

    @PostMapping("/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse copy(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return examService.copy(id, userId);
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

    /** Subir o bajar los intentos por alumno de un examen publicado (RF-52). */
    @PatchMapping("/{id}/attempts")
    public ExamResponse changeAttempts(@PathVariable Long id, @Valid @RequestBody ChangeAttemptsRequest request,
                                       @AuthenticationPrincipal Long userId) {
        return examService.changeAttempts(id, request, userId);
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

    /** Las preguntas tal como se publicaron, aunque se hayan editado después en el banco (DE-03). */
    @GetMapping("/{id}/published-questions")
    public List<PublishedQuestionResponse> publishedQuestions(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return examService.publishedQuestions(id, userId);
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
    public PublicExamResponse getByCode(@PathVariable String code) {
        return examService.getAccessibleByCode(code);
    }

    /** userId es null si el alumno no ha iniciado sesión. */
    @PostMapping("/by-code/{code}/attempts")
    public ExamAttemptResponse startAttempt(@PathVariable String code, @Valid @RequestBody StartAttemptRequest request,
                                            @AuthenticationPrincipal Long userId) {
        return attemptService.start(code, request, userId);
    }

    /** Recuperar el intento tras recargar o perder la conexión (RF-24). */
    @PostMapping("/by-code/{code}/attempts/resume")
    public ExamAttemptResponse resumeAttempt(@PathVariable String code, @Valid @RequestBody ResumeAttemptRequest request) {
        return attemptService.resume(code, request);
    }

    /** Guardar lo marcado en una pregunta al contestarla (DE-05). */
    @PutMapping("/by-code/{code}/attempts/answers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveAnswer(@PathVariable String code, @Valid @RequestBody SaveAnswerRequest request) {
        attemptService.saveAnswer(code, request);
    }
}
