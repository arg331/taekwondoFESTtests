package com.taekwondo.examenes.application.result;

import com.taekwondo.examenes.application.result.dto.AnswerSubmission;
import com.taekwondo.examenes.application.result.dto.ResultView;
import com.taekwondo.examenes.application.result.dto.SubmitExamInput;
import com.taekwondo.examenes.application.shared.exception.BusinessRuleViolationException;
import com.taekwondo.examenes.application.shared.exception.ResourceNotFoundException;
import com.taekwondo.examenes.domain.model.Answer;
import com.taekwondo.examenes.domain.model.Exam;
import com.taekwondo.examenes.domain.model.Question;
import com.taekwondo.examenes.domain.model.Result;
import com.taekwondo.examenes.domain.port.Clock;
import com.taekwondo.examenes.domain.port.ExamRepository;
import com.taekwondo.examenes.domain.port.QuestionRepository;
import com.taekwondo.examenes.domain.port.ResultRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Caso de uso: un estudiante envía sus respuestas y se calcula el resultado.
 *
 * Es el caso de uso más complejo del sistema porque orquesta tres agregados:
 *  - Exam (verificar que existe y es accesible)
 *  - Question (verificar respuestas correctas en este momento)
 *  - Result (crear el snapshot histórico)
 *
 * Pasos:
 *  1. Resolver el examen por código.
 *  2. Verificar que es accesible (publicado y dentro de fecha).
 *  3. Comprobar que el estudiante no lo ha hecho ya.
 *  4. Validar que las respuestas cubren TODAS las preguntas del examen.
 *  5. Para cada respuesta: cargar la pregunta y construir un Answer
 *     con la respuesta correcta del momento.
 *  6. Crear el Result y persistirlo.
 */
public class SubmitExamUseCase {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ResultRepository resultRepository;
    private final Clock clock;

    public SubmitExamUseCase(ExamRepository examRepository,
                              QuestionRepository questionRepository,
                              ResultRepository resultRepository,
                              Clock clock) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.resultRepository = resultRepository;
        this.clock = clock;
    }

    public ResultView execute(SubmitExamInput input) {
        // 1. Resolver examen por código
        Exam exam = examRepository.findByCode(input.examCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe ningún examen con código " + input.examCode()));

        // 2. Verificar accesibilidad (estado + fecha on-the-fly)
        if (!exam.isAccessibleAt(clock)) {
            throw new BusinessRuleViolationException(
                    "Este examen no está disponible en este momento");
        }

        // 3. Prevenir duplicados: un estudiante (por nombre) no puede
        //    hacer el mismo examen dos veces.
        if (resultRepository.existsByExamIdAndStudentName(exam.getId(), input.studentName())) {
            throw new BusinessRuleViolationException(
                    "Este estudiante ya ha realizado este examen");
        }

        // 4. Validar que las respuestas cubren exactamente las preguntas del examen.
        validateAnswerSet(exam.getQuestionIds(), input.answers());

        // 5. Construir los Answers cargando la respuesta correcta de cada pregunta.
        List<Answer> answers = buildAnswers(input.answers());

        // 6. Crear y persistir el Result (la entidad calcula score y correctAnswers)
        Result result = Result.createNew(
                exam.getId(),
                input.studentName(),
                input.studentClub(),
                input.studentEmail(),
                answers,
                input.timeSpentSeconds(),
                clock.now()
        );

        Result persisted = resultRepository.save(result);
        return ResultView.from(persisted);
    }

    /**
     * Verifica que el conjunto de respuestas enviadas coincide EXACTAMENTE
     * con el conjunto de preguntas del examen: ni faltan, ni sobran, ni
     * hay duplicados.
     */
    private void validateAnswerSet(List<Long> expectedQuestionIds,
                                    List<AnswerSubmission> submittedAnswers) {
        if (submittedAnswers == null || submittedAnswers.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Debes responder al menos una pregunta");
        }

        Set<Long> expectedIds = new HashSet<>(expectedQuestionIds);
        Set<Long> submittedIds = new HashSet<>();
        for (AnswerSubmission s : submittedAnswers) {
            if (!submittedIds.add(s.questionId())) {
                throw new BusinessRuleViolationException(
                        "Hay respuestas duplicadas para la pregunta " + s.questionId());
            }
        }

        if (!submittedIds.equals(expectedIds)) {
            throw new BusinessRuleViolationException(
                    "Las respuestas no coinciden con las preguntas del examen "
                            + "(esperadas: " + expectedIds.size()
                            + ", recibidas: " + submittedIds.size() + ")");
        }
    }

    /**
     * Construye los objetos Answer cargando, para cada pregunta, su respuesta
     * correcta tal y como es EN ESTE MOMENTO. Eso queda guardado en el Result
     * como snapshot histórico.
     */
    private List<Answer> buildAnswers(List<AnswerSubmission> submittedAnswers) {
        List<Answer> answers = new ArrayList<>(submittedAnswers.size());
        for (AnswerSubmission s : submittedAnswers) {
            Question question = questionRepository.findById(s.questionId())
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "La pregunta con id " + s.questionId()
                                    + " ya no existe (¿se eliminó durante el examen?)"));

            answers.add(Answer.of(
                    s.questionId(),
                    s.chosenOption(),
                    question.getCorrectAnswer()
            ));
        }
        return answers;
    }
}
