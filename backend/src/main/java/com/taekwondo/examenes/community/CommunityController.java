package com.taekwondo.examenes.community;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Exámenes de prueba de otros profesores (RF-30), favoritos (RF-31) y copiar sus preguntas (DE-12). Solo profesores. */
@RestController
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping("/api/community/exams")
    public List<CommunityExamResponse> list(@AuthenticationPrincipal Long userId) {
        return communityService.list(userId);
    }

    @GetMapping("/api/favorites")
    public List<CommunityExamResponse> favorites(@AuthenticationPrincipal Long userId) {
        return communityService.favorites(userId);
    }

    @GetMapping("/api/community/exams/{id}/questions")
    public List<CommunityQuestionResponse> questions(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return communityService.questions(id, userId);
    }

    @PostMapping("/api/community/exams/{id}/copy-questions")
    public CopyQuestionsResponse copyQuestions(@PathVariable Long id, @Valid @RequestBody CopyQuestionsRequest request,
                                               @AuthenticationPrincipal Long userId) {
        return communityService.copyQuestions(id, request.questionIds(), userId);
    }
}
