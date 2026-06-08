package com.taekwondo.examenes.application.exam;

import com.taekwondo.examenes.application.question.dto.QuestionView;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.port.Clock;
import com.taekwondo.examenes.domain.port.ExamRepository;
import com.taekwondo.examenes.domain.port.QuestionRepository;

import java.util.List;

/**
 * Caso de uso: obtener las preguntas de un examen accesible por código.
 *
 * Uso exclusivo del flujo exam-take (estudiante anónimo o registrado).
 * El endpoint que lo usa es público — por eso NO expone correctAnswer
 * ni explanation (eso lo hace PublicQuestionResponse en el controller).
 *
 * Valida que el examen esté accesible igual que GetExamByCodeUseCase.
 */
public class GetExamQuestionsByCodeUseCase {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    public GetExamQuestionsByCodeUseCase(ExamRepository examRepository,
                                          QuestionRepository questionRepository,
                                          Clock clock) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    public List<QuestionView> execute(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessRuleViolationException("El código no puede estar vacío");
        }

        Exam exam = examRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe ningún examen con código " + code));

        if (!exam.isAccessibleAt(clock)) {
            throw new BusinessRuleViolationException(
                    "Este examen no está disponible en este momento");
        }

        return exam.getQuestionIds().stream()
                .map(id -> questionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Pregunta " + id + " no encontrada")))
                .map(QuestionView::from)
                .toList();
    }
}
