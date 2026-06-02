package com.taekwondo.examenes.infrastructure.web.controller;

import com.taekwondo.examenes.application.tag.CreateTagUseCase;
import com.taekwondo.examenes.application.tag.GetTagUseCase;
import com.taekwondo.examenes.application.tag.ListTagsUseCase;
import com.taekwondo.examenes.application.tag.RenameTagUseCase;
import com.taekwondo.examenes.application.tag.dto.CreateTagInput;
import com.taekwondo.examenes.application.tag.dto.RenameTagInput;
import com.taekwondo.examenes.application.tag.dto.TagView;
import com.taekwondo.examenes.infrastructure.security.AuthenticatedUser;
import com.taekwondo.examenes.infrastructure.web.dto.CreateTagRequest;
import com.taekwondo.examenes.infrastructure.web.dto.RenameTagRequest;
import com.taekwondo.examenes.infrastructure.web.dto.TagResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para Tag.
 *
 * El ownerId se obtiene del usuario autenticado (JWT) vía AuthenticatedUser.
 * Sin token válido, JwtAuthenticationFilter + SecurityConfig rechazan
 * la petición antes de llegar aquí.
 */
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final CreateTagUseCase createTagUseCase;
    private final GetTagUseCase getTagUseCase;
    private final ListTagsUseCase listTagsUseCase;
    private final RenameTagUseCase renameTagUseCase;

    public TagController(CreateTagUseCase createTagUseCase,
                          GetTagUseCase getTagUseCase,
                          ListTagsUseCase listTagsUseCase,
                          RenameTagUseCase renameTagUseCase) {
        this.createTagUseCase = createTagUseCase;
        this.getTagUseCase = getTagUseCase;
        this.listTagsUseCase = listTagsUseCase;
        this.renameTagUseCase = renameTagUseCase;
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@RequestBody CreateTagRequest request) {
        CreateTagInput input = new CreateTagInput(
                request.name(),
                request.color(),
                AuthenticatedUser.currentUserId()
        );
        TagView view = createTagUseCase.execute(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(TagResponse.from(view));
    }

    @GetMapping("/{id}")
    public TagResponse get(@PathVariable Long id) {
        TagView view = getTagUseCase.execute(id, AuthenticatedUser.currentUserId());
        return TagResponse.from(view);
    }

    @GetMapping
    public List<TagResponse> list() {
        return listTagsUseCase.execute(AuthenticatedUser.currentUserId()).stream()
                .map(TagResponse::from)
                .toList();
    }

    @PatchMapping("/{id}")
    public TagResponse rename(@PathVariable Long id, @RequestBody RenameTagRequest request) {
        RenameTagInput input = new RenameTagInput(
                id, request.newName(), AuthenticatedUser.currentUserId());
        TagView view = renameTagUseCase.execute(input);
        return TagResponse.from(view);
    }
}
