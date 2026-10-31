package com.taekwondo.examenes.favorite;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.exam.Exam;
import com.taekwondo.examenes.exam.ExamRepository;
import com.taekwondo.examenes.exam.Visibility;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Favoritos: un profesor guarda exámenes públicos de otros profesores. El listado está en CommunityService (RF-31).
 */
@Service
@Transactional
public class FavoriteService {

    private final ExamFavoriteRepository favoriteRepository;
    private final ExamRepository examRepository;

    public FavoriteService(ExamFavoriteRepository favoriteRepository, ExamRepository examRepository) {
        this.favoriteRepository = favoriteRepository;
        this.examRepository = examRepository;
    }

    public void add(Long examId, Long professorId) {
        Exam exam = examRepository.findById(examId)
                .filter(e -> e.getVisibility() == Visibility.PUBLIC || e.isOwnedBy(professorId))
                .orElseThrow(() -> new ResourceNotFoundException("No existe un examen con id " + examId));
        if (exam.isOwnedBy(professorId)) {
            throw new BusinessRuleException("No puedes añadir a favoritos tus propios exámenes");
        }
        if (favoriteRepository.existsByProfessorIdAndExamId(professorId, examId)) {
            throw new BusinessRuleException("Este examen ya está en tus favoritos");
        }
        favoriteRepository.save(new ExamFavorite(professorId, examId));
    }

    public void remove(Long examId, Long professorId) {
        if (favoriteRepository.deleteByProfessorIdAndExamId(professorId, examId) == 0) {
            throw new ResourceNotFoundException("Este examen no está en tus favoritos");
        }
    }
}
