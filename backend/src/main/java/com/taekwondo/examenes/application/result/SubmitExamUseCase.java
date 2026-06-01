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
 * Orquesta tres agregados:
 *  - Exam (verificar que existe y es accesible)
 *  - Question (verificar respuestas correctas en este momento)
 *  - Result (crear el snapshot histórico)
 *
 * Sobre la autenticación:
 *  - Si el examen es REGISTERED_ONLY, se exige studentUserId no nulo.
 *  - Si el examen es OPEN, studentUserId puede ser null (anónimo) o
 *    no nulo (registrado que decide hacerlo con su cuenta).
 *
 * El studentUserId lo decide el controller a partir del JWT (si lo hay).
 * Este caso de uso solo recibe el dato resuelto, no toca tokens.
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

    /**
     * @param input          datos del envío (código, respuestas, datos del estudiante)
     * @param studentUserId  id del usuario logueado, o null si es anónimo
     */
    public ResultView execute(SubmitExamInput input, Long studentUserId) {
        // 1. Resolver examen por código
        Exam exam = examRepository.findByCode(input.examCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe ningún examen con código " + input.examCode()));

        // 2. Verificar accesibilidad (estado + fecha on-the-fly)
        if (!exam.isAccessibleAt(clock)) {
            throw new BusinessRuleViolationException(
                    "Este examen no está disponible en este momento");
        }

        // 3. Si el examen requiere registro, exigir que haya studentUserId
        if (exam.requiresRegistration() && studentUserId == null) {
            throw new BusinessRuleViolationException(
                    "Este examen requiere estar registrado e iniciar sesión");
        }

        // 4. Prevenir duplicados: un estudiante (por nombre) no puede
        //    hacer el mismo examen dos veces.
        if (resultRepository.existsByExamIdAndStudentName(exam.getId(), input.studentName())) {
            throw new BusinessRuleViolationException(
                    "Este estudiante ya ha realizado este examen");
        }

        // 5. Validar que las respuestas cubren exactamente las preguntas del examen.
        validateAnswerSet(exam.getQuestionIds(), input.answers());

        // 6. Construir los Answers cargando la respuesta correcta de cada pregunta.
        List<Answer> answers = buildAnswers(input.answers());

        // 7. Crear y persistir el Result
        Result result = Result.createNew(
                exam.getId(),
                studentUserId,
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
