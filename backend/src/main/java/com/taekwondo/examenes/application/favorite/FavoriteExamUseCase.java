package com.taekwondo.examenes.application.favorite;

import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.ExamFavorite;
import com.taekwondo.examenes.domain.model.Visibility;
import com.taekwondo.examenes.domain.port.ExamFavoriteRepository;
import com.taekwondo.examenes.domain.port.ExamRepository;

/**
 * Caso de uso: un profesor marca como favorito el examen de otro profesor.
 *
 * Reglas de coordinación (no de entidad, porque requieren consulta):
 *  - El examen debe existir.
 *  - El examen debe ser PUBLIC.
 *  - El examen debe ser de OTRO profesor (no se favoritea uno mismo).
 *  - No puede favoritearse dos veces.
 */
public class FavoriteExamUseCase {

    private final ExamRepository examRepository;
    private final ExamFavoriteRepository favoriteRepository;

    public FavoriteExamUseCase(ExamRepository examRepository,
                                ExamFavoriteRepository favoriteRepository) {
        this.examRepository = examRepository;
        this.favoriteRepository = favoriteRepository;
    }

    public void execute(Long examId, Long professorId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un examen con id " + examId));

        if (exam.getVisibility() != Visibility.PUBLIC) {
            throw new BusinessRuleViolationException(
                    "Solo se pueden favoritear exámenes públicos");
        }

        if (exam.getOwnerId().equals(professorId)) {
            throw new BusinessRuleViolationException(
                    "No puedes añadir a favoritos tus propios exámenes");
        }

        if (favoriteRepository.existsByProfessorIdAndExamId(professorId, examId)) {
            throw new BusinessRuleViolationException(
                    "Este examen ya está en tus favoritos");
        }

        ExamFavorite favorite = ExamFavorite.createNew(professorId, examId);
        favoriteRepository.save(favorite);
    }
}
