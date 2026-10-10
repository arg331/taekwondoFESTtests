package com.taekwondo.examenes.question;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionResponse create(@Valid @RequestBody QuestionRequest request, @AuthenticationPrincipal Long userId) {
        return questionService.create(request, userId);
    }

    @PutMapping("/{id}")
    public QuestionResponse update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request,
                                   @AuthenticationPrincipal Long userId) {
        return questionService.update(id, request, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        questionService.delete(id, userId);
    }

    @GetMapping("/{id}")
    public QuestionResponse get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return questionService.get(id, userId);
    }

    @GetMapping
    public List<QuestionResponse> list(@AuthenticationPrincipal Long userId) {
        return questionService.list(userId);
    }

    @GetMapping("/search")
    public List<QuestionResponse> search(@RequestParam(required = false) Set<Long> tagIds,
                                         @RequestParam(required = false) String textContains,
                                         @AuthenticationPrincipal Long userId) {
        return questionService.search(tagIds, textContains, userId);
    }
}
