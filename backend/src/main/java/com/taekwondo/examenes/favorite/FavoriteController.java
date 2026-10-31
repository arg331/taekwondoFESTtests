package com.taekwondo.examenes.favorite;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/api/exams/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        favoriteService.add(id, userId);
    }

    @DeleteMapping("/api/exams/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        favoriteService.remove(id, userId);
    }
}
