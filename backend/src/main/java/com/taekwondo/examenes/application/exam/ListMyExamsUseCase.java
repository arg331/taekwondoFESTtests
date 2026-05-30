package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.exam.dto.ExamView;
import com.taekwondo.examenes.domain.port.ExamRepository;

import java.util.List;

/**
 * Caso de uso: listar todos los exámenes de un profesor.
 *
 * Incluye drafts, publicados y expirados. El filtrado por estado se hace
 * en el cliente (o en un caso de uso más específico si lo necesitamos).
 */
public class ListMyExamsUseCase {

    private final ExamRepository examRepository;

    public ListMyExamsUseCase(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    public List<ExamView> execute(Long ownerId) {
        return examRepository.findAllByOwnerId(ownerId).stream()
                .map(ExamView::from)
                .toList();
    }
}
