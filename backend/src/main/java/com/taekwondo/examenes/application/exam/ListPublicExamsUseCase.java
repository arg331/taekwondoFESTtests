package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.domain.port.ExamRepository;

import java.util.List;

/**
 * Caso de uso: listar exámenes públicos de OTROS profesores.
 *
 * Útil para que un profesor descubra exámenes compartidos por sus
 * colegas, los favorite o los importe como base para los suyos.
 *
 * Excluye los del propio solicitante: para ver los propios está
 * ListMyExamsUseCase.
 */
public class ListPublicExamsUseCase {

    private final ExamRepository examRepository;

    public ListPublicExamsUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public List<ExamView> execute(Long requesterOwnerId) {
        return examRepository.findAllPublic().stream()
                .filter(exam -> !exam.getOwnerId().equals(requesterOwnerId))
                .map(ExamView::from)
                .toList();
    }
}
