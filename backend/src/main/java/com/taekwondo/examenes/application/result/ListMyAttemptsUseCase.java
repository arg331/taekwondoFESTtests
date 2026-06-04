package com.taekwondo.examenes.application.result;

import com.taekwondo.examenes.application.result.dto.ResultView;
import com.taekwondo.examenes.domain.port.ResultRepository;

import java.util.List;

/**
 * Caso de uso: el estudiante registrado lista su historial de exámenes.
 *
 * Devuelve únicamente los resultados que el estudiante hizo con su cuenta
 * (studentUserId no nulo). Si el mismo nombre hizo otros exámenes de
 * forma anónima antes de registrarse, NO aparecen aquí: el sistema no
 * puede asociarlos a su cuenta de forma fiable.
 *
 * Este caso de uso lo invoca el endpoint "/me/attempts" del frontend
 * cuando el estudiante consulta su historial. El controller obtiene
 * el studentUserId del JWT y lo pasa aquí.
 */
public class ListMyAttemptsUseCase {

    private final ResultRepository resultRepository;

    public ListMyAttemptsUseCase(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public List<ResultView> execute(Long studentUserId) {
        return resultRepository.findAllByStudentUserId(studentUserId).stream()
                .map(ResultView::from)
                .toList();
    }
}
