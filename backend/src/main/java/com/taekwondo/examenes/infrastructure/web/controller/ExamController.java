package com.taekwondo.examenes.infrastructure.web.controller;

import com.taekwondo.examenes.application.exam.*;
import com.taekwondo.examenes.application.exam.dto.*;
import com.taekwondo.examenes.infrastructure.security.AuthenticatedUser;
import com.taekwondo.examenes.infrastructure.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para Exam.
 *
 * Endpoints autenticados (requieren JWT):
 *  - Crear drafts, editar, publicar, listar mis exámenes, etc.
 *
 * Endpoints públicos (sin token):
 *  - GET /api/exams/by-code/{code}: el estudiante anónimo accede al examen
 *    desde el QR. El servidor solo necesita el código para devolver el
 *    examen accesible. Los datos del estudiante (nombre/club/email) se
 *    piden DESPUÉS, en el submit del Result.
 *
 * El ownerId/requesterOwnerId se obtiene siempre del JWT vía
 * AuthenticatedUser.currentUserId() en todos los endpoints autenticados.
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final CreateExamDraftUseCase createExamDraftUseCase;
    private final PreGenerateExamDraftUseCase preGenerateExamDraftUseCase;
    private final RenameExamUseCase renameExamUseCase;
    private final ChangeExamConfigUseCase changeExamConfigUseCase;
    private final UpdateExamQuestionsUseCase updateExamQuestionsUseCase;
    private final ChangeExamVisibilityUseCase changeExamVisibilityUseCase;
    private final PublishExamUseCase publishExamUseCase;
    private final CloseExamUseCase closeExamUseCase;
    private final ReopenExamUseCase reopenExamUseCase;
    private final ExtendExamExpirationUseCase extendExamExpirationUseCase;
    private final DeleteExamDraftUseCase deleteExamDraftUseCase;
    private final GetExamUseCase getExamUseCase;
    private final GetExamByCodeUseCase getExamByCodeUseCase;
    private final ListMyExamsUseCase listMyExamsUseCase;
    private final ListPublicExamsUseCase listPublicExamsUseCase;

    public ExamController(
            CreateExamDraftUseCase createExamDraftUseCase,
            PreGenerateExamDraftUseCase preGenerateExamDraftUseCase,
            RenameExamUseCase renameExamUseCase,
            ChangeExamConfigUseCase changeExamConfigUseCase,
            UpdateExamQuestionsUseCase updateExamQuestionsUseCase,
            ChangeExamVisibilityUseCase changeExamVisibilityUseCase,
            PublishExamUseCase publishExamUseCase,
            CloseExamUseCase closeExamUseCase,
            ReopenExamUseCase reopenExamUseCase,
            ExtendExamExpirationUseCase extendExamExpirationUseCase,
            DeleteExamDraftUseCase deleteExamDraftUseCase,
            GetExamUseCase getExamUseCase,
            GetExamByCodeUseCase getExamByCodeUseCase,
            ListMyExamsUseCase listMyExamsUseCase,
            ListPublicExamsUseCase listPublicExamsUseCase) {
        this.createExamDraftUseCase = createExamDraftUseCase;
        this.preGenerateExamDraftUseCase = preGenerateExamDraftUseCase;
        this.renameExamUseCase = renameExamUseCase;
        this.changeExamConfigUseCase = changeExamConfigUseCase;
        this.updateExamQuestionsUseCase = updateExamQuestionsUseCase;
        this.changeExamVisibilityUseCase = changeExamVisibilityUseCase;
        this.publishExamUseCase = publishExamUseCase;
        this.closeExamUseCase = closeExamUseCase;
        this.reopenExamUseCase = reopenExamUseCase;
        this.extendExamExpirationUseCase = extendExamExpirationUseCase;
        this.deleteExamDraftUseCase = deleteExamDraftUseCase;
        this.getExamUseCase = getExamUseCase;
        this.getExamByCodeUseCase = getExamByCodeUseCase;
        this.listMyExamsUseCase = listMyExamsUseCase;
        this.listPublicExamsUseCase = listPublicExamsUseCase;
    }

    // ───── Creación ─────

    @PostMapping("/drafts")
    public ResponseEntity<ExamResponse> createDraft(@RequestBody CreateExamDraftRequest request) {
        CreateExamDraftInput input = new CreateExamDraftInput(
                request.title(),
                request.numberOfQuestions(),
                request.timeLimitMinutes(),
                request.showScore(),
                request.randomizeOptions(),
                request.randomizeQuestionOrder(),
                AuthenticatedUser.currentUserId()
        );
        ExamView view = createExamDraftUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExamResponse.from(view));
    }

    @PostMapping("/drafts/pre-generated")
    public ResponseEntity<ExamResponse> preGenerateDraft(
            @RequestBody PreGenerateExamDraftRequest request) {
        PreGenerateExamDraftInput input = new PreGenerateExamDraftInput(
                request.title(),
                request.numberOfQuestions(),
                request.timeLimitMinutes(),
                request.showScore(),
                request.randomizeOptions(),
                request.randomizeQuestionOrder(),
                request.requiredAnyOfTagIds(),
                AuthenticatedUser.currentUserId()
        );
        ExamView view = preGenerateExamDraftUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExamResponse.from(view));
    }

    // ───── Edición ─────

    @PatchMapping("/{id}/title")
    public ExamResponse renameExam(@PathVariable Long id,
                                    @RequestBody RenameExamRequest request) {
        RenameExamInput input = new RenameExamInput(
                id, request.newTitle(), AuthenticatedUser.currentUserId());
        return ExamResponse.from(renameExamUseCase.execute(input));
    }

    @PatchMapping("/{id}/config")
    public ExamResponse changeConfig(@PathVariable Long id,
                                      @RequestBody ChangeExamConfigRequest request) {
        ChangeExamConfigInput input = new ChangeExamConfigInput(
                id,
                request.numberOfQuestions(),
                request.timeLimitMinutes(),
                request.showScore(),
                request.randomizeOptions(),
                request.randomizeQuestionOrder(),
                AuthenticatedUser.currentUserId()
        );
        return ExamResponse.from(changeExamConfigUseCase.execute(input));
    }

    @PutMapping("/{id}/questions")
    public ExamResponse updateQuestions(@PathVariable Long id,
                                         @RequestBody UpdateExamQuestionsRequest request) {
        UpdateExamQuestionsInput input = new UpdateExamQuestionsInput(
                id, request.questionIds(), AuthenticatedUser.currentUserId());
        return ExamResponse.from(updateExamQuestionsUseCase.execute(input));
    }

    @PatchMapping("/{id}/visibility")
    public ExamResponse changeVisibility(@PathVariable Long id,
                                          @RequestBody ChangeExamVisibilityRequest request) {
        ChangeExamVisibilityInput input = new ChangeExamVisibilityInput(
                id, request.newVisibility(), AuthenticatedUser.currentUserId());
        return ExamResponse.from(changeExamVisibilityUseCase.execute(input));
    }

    // ───── Transiciones de estado ─────

    @PostMapping("/{id}/publish")
    public ExamResponse publishExam(@PathVariable Long id,
                                     @RequestBody PublishExamRequest request) {
        PublishExamInput input = new PublishExamInput(
                id,
                request.visibility(),
                request.expiresAt(),
                AuthenticatedUser.currentUserId()
        );
        return ExamResponse.from(publishExamUseCase.execute(input));
    }

    @PostMapping("/{id}/close")
    public ExamResponse closeExam(@PathVariable Long id) {
        return ExamResponse.from(
                closeExamUseCase.execute(id, AuthenticatedUser.currentUserId()));
    }

    @PostMapping("/{id}/reopen")
    public ExamResponse reopenExam(@PathVariable Long id,
                                    @RequestBody ReopenExamRequest request) {
        ReopenExamInput input = new ReopenExamInput(
                id, request.newExpiresAt(), AuthenticatedUser.currentUserId());
        return ExamResponse.from(reopenExamUseCase.execute(input));
    }

    @PatchMapping("/{id}/expiration")
    public ExamResponse extendExpiration(@PathVariable Long id,
                                          @RequestBody ExtendExamExpirationRequest request) {
        ExtendExamExpirationInput input = new ExtendExamExpirationInput(
                id, request.newExpiresAt(), AuthenticatedUser.currentUserId());
        return ExamResponse.from(extendExamExpirationUseCase.execute(input));
    }

    // ───── Borrado ─────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        deleteExamDraftUseCase.execute(id, AuthenticatedUser.currentUserId());
        return ResponseEntity.noContent().build();
    }

    // ───── Consultas (autenticadas) ─────

    @GetMapping("/{id}")
    public ExamResponse getMyExam(@PathVariable Long id) {
        return ExamResponse.from(
                getExamUseCase.execute(id, AuthenticatedUser.currentUserId()));
    }

    @GetMapping("/mine")
    public List<ExamResponse> listMyExams() {
        return listMyExamsUseCase.execute(AuthenticatedUser.currentUserId()).stream()
                .map(ExamResponse::from)
                .toList();
    }

    @GetMapping("/public")
    public List<ExamResponse> listPublicExams() {
        return listPublicExamsUseCase.execute(AuthenticatedUser.currentUserId()).stream()
                .map(ExamResponse::from)
                .toList();
    }

    // ───── Consulta pública (estudiante con QR) ─────

    @GetMapping("/by-code/{code}")
    public ExamResponse getExamByCode(@PathVariable String code) {
        return ExamResponse.from(getExamByCodeUseCase.execute(code));
    }
}
