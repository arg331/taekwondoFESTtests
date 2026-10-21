package com.taekwondo.examenes.share;

import com.taekwondo.examenes.exam.ExamResponse;
import com.taekwondo.examenes.question.QuestionResponse;
import com.taekwondo.examenes.share.ShareDtos.ExamShareResponse;
import com.taekwondo.examenes.share.ShareDtos.InvitationResponse;
import com.taekwondo.examenes.share.ShareDtos.InviteRequest;
import com.taekwondo.examenes.share.ShareDtos.ShareCandidateResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Exámenes compartidos entre profesores (RF-51).
 * <ul>
 *   <li>/api/exams/{id}/shares: el dueño invita, ve con quién lo comparte y retira el acceso;
 *       /shares/me: el invitado deja de verlo.</li>
 *   <li>/api/shares/invitations: el invitado ve sus invitaciones y las acepta o rechaza.</li>
 *   <li>/api/shares/questions: las preguntas de los exámenes que me han compartido (RF-57).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class ExamShareController {

    private final ExamShareService shareService;

    public ExamShareController(ExamShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping("/exams/{examId}/shares")
    @ResponseStatus(HttpStatus.CREATED)
    public ExamShareResponse invite(@PathVariable Long examId, @Valid @RequestBody InviteRequest request,
                                    @AuthenticationPrincipal Long userId) {
        return shareService.invite(examId, userId, request.teacherId());
    }

    @GetMapping("/exams/{examId}/shares")
    public List<ExamShareResponse> list(@PathVariable Long examId, @AuthenticationPrincipal Long userId) {
        return shareService.list(examId, userId);
    }

    @GetMapping("/exams/{examId}/shares/candidates")
    public List<ShareCandidateResponse> candidates(@PathVariable Long examId,
                                                   @RequestParam(defaultValue = "") String query,
                                                   @AuthenticationPrincipal Long userId) {
        return shareService.candidates(examId, userId, query);
    }

    @DeleteMapping("/exams/{examId}/shares/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long examId, @AuthenticationPrincipal Long userId) {
        shareService.leave(examId, userId);
    }

    @DeleteMapping("/exams/{examId}/shares/{teacherId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long examId, @PathVariable Long teacherId, @AuthenticationPrincipal Long userId) {
        shareService.revoke(examId, userId, teacherId);
    }

    @GetMapping("/shares/invitations")
    public List<InvitationResponse> invitations(@AuthenticationPrincipal Long userId) {
        return shareService.invitations(userId);
    }

    @PostMapping("/shares/invitations/{invitationId}/accept")
    public ExamResponse accept(@PathVariable Long invitationId, @AuthenticationPrincipal Long userId) {
        return shareService.accept(invitationId, userId);
    }

    /** Preguntas de los exámenes que me han compartido (RF-57), para consultarlas. */
    @GetMapping("/shares/questions")
    public List<QuestionResponse> sharedQuestions(@AuthenticationPrincipal Long userId) {
        return shareService.sharedQuestions(userId);
    }

    @DeleteMapping("/shares/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable Long invitationId, @AuthenticationPrincipal Long userId) {
        shareService.reject(invitationId, userId);
    }
}
