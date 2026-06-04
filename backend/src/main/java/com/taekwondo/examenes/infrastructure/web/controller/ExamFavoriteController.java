package com.taekwondo.examenes.infrastructure.web.controller;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.application.favorite.FavoriteExamUseCase;
import com.taekwondo.examenes.application.favorite.ListMyFavoriteExamsUseCase;
import com.taekwondo.examenes.application.favorite.UnfavoriteExamUseCase;
import com.taekwondo.examenes.infrastructure.security.AuthenticatedUser;
import com.taekwondo.examenes.infrastructure.web.dto.ExamResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ExamFavoriteController {

    private final FavoriteExamUseCase favoriteExamUseCase;
    private final UnfavoriteExamUseCase unfavoriteExamUseCase;
    private final ListMyFavoriteExamsUseCase listMyFavoriteExamsUseCase;

    public ExamFavoriteController(FavoriteExamUseCase favoriteExamUseCase,
                                    UnfavoriteExamUseCase unfavoriteExamUseCase,
                                    ListMyFavoriteExamsUseCase listMyFavoriteExamsUseCase) {
        this.favoriteExamUseCase = favoriteExamUseCase;
        this.unfavoriteExamUseCase = unfavoriteExamUseCase;
        this.listMyFavoriteExamsUseCase = listMyFavoriteExamsUseCase;
    }

    @PostMapping("/api/exams/{id}/favorite")
    public ResponseEntity<Void> favorite(@PathVariable Long id) {
        favoriteExamUseCase.execute(id, AuthenticatedUser.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/api/exams/{id}/favorite")
    public ResponseEntity<Void> unfavorite(@PathVariable Long id) {
        unfavoriteExamUseCase.execute(id, AuthenticatedUser.currentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/favorites")
    public List<ExamResponse> listMyFavorites() {
        List<ExamView> views = listMyFavoriteExamsUseCase
                .execute(AuthenticatedUser.currentUserId());
        return views.stream()
                .map(ExamResponse::from)
                .toList();
    }
}
