package com.taekwondo.examenes.controller;

import com.taekwondo.examenes.dto.tag.CreateTagRequest;
import com.taekwondo.examenes.dto.tag.RenameTagRequest;
import com.taekwondo.examenes.dto.tag.TagResponse;
import com.taekwondo.examenes.service.TagService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse create(@Valid @RequestBody CreateTagRequest request, @AuthenticationPrincipal Long userId) {
        return tagService.create(request, userId);
    }

    @GetMapping
    public List<TagResponse> list(@AuthenticationPrincipal Long userId) {
        return tagService.list(userId);
    }

    @GetMapping("/{id}")
    public TagResponse get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return tagService.get(id, userId);
    }

    @PatchMapping("/{id}")
    public TagResponse rename(@PathVariable Long id, @Valid @RequestBody RenameTagRequest request,
                              @AuthenticationPrincipal Long userId) {
        return tagService.rename(id, request, userId);
    }
}
