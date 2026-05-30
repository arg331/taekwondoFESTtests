package com.taekwondo.examenes.application.favorite;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.ExamFavorite;
import com.taekwondo.examenes.domain.port.ExamFavoriteRepository;
import com.taekwondo.examenes.domain.port.ExamRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Caso de uso: listar los exámenes favoriteados por un profesor.
 *
 * Devolvemos ExamView (no ExamFavorite) porque al cliente le interesa
 * la información del examen, no la del registro de favorito.
 *
 * Si un examen favoriteado fue eliminado mientras tanto, simplemente
 * lo omitimos del listado (no rompemos toda la consulta por un dato roto).
 */
public class ListMyFavoriteExamsUseCase {

    private final ExamFavoriteRepository favoriteRepository;
    private final ExamRepository examRepository;

    public ListMyFavoriteExamsUseCase(ExamFavoriteRepository favoriteRepository,
                                        ExamRepository examRepository) {
        this.favoriteRepository = favoriteRepository;
        this.examRepository = examRepository;
    }

    public List<ExamView> execute(Long professorId) {
        List<ExamFavorite> favorites = favoriteRepository.findAllByProfessorId(professorId);

        List<ExamView> views = new ArrayList<>(favorites.size());
        for (ExamFavorite favorite : favorites) {
            examRepository.findById(favorite.getExamId())
                    .map(ExamView::from)
                    .ifPresent(views::add);
        }
        return views;
    }
}
