package com.taekwondo.examenes.application.favorite;

import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.port.ExamFavoriteRepository;

/**
 * Caso de uso: un profesor quita un examen de sus favoritos.
 *
 * Decisión: si intenta quitar algo que no estaba en favoritos, lanzamos
 * ResourceNotFoundException. Es preferible a fallar silenciosamente
 * (el cliente debe saber que la operación no tenía efecto).
 */
public class UnfavoriteExamUseCase {

    private final ExamFavoriteRepository favoriteRepository;

    public UnfavoriteExamUseCase(ExamFavoriteRepository favoriteRepository) {
        this.favoriteRepository = favoriteRepository;
    }

    public void execute(Long examId, Long professorId) {
        if (!favoriteRepository.existsByProfessorIdAndExamId(professorId, examId)) {
            throw new ResourceNotFoundException(
                    "Este examen no está en tus favoritos");
        }

        favoriteRepository.deleteByProfessorIdAndExamId(professorId, examId);
    }
}
