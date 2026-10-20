package com.taekwondo.examenes.result;

import com.taekwondo.examenes.attempt.ExamAttempt;
import com.taekwondo.examenes.attempt.ExamAttemptRepository;
import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.exam.Exam;
import com.taekwondo.examenes.exam.ExamRepository;
import com.taekwondo.examenes.exam.ExamService;
import com.taekwondo.examenes.exam.PublishedQuestion;
import com.taekwondo.examenes.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResultService {

    private final ResultRepository resultRepository;
    private final ExamRepository examRepository;
    private final ExamService examService;
    private final ExamAttemptRepository attemptRepository;
    private final JwtService jwtService;
    private final Clock clock;

    public ResultService(ResultRepository resultRepository, ExamRepository examRepository,
                         ExamService examService, ExamAttemptRepository attemptRepository,
                         JwtService jwtService, Clock clock) {
        this.resultRepository = resultRepository;
        this.examRepository = examRepository;
        this.examService = examService;
        this.attemptRepository = attemptRepository;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /**
     * Corrige y guarda la entrega de un intento. Quién la hace lo dice el intento (DE-04), y el
     * tiempo empleado lo calcula el servidor desde que empezó, no lo decide el cliente.
     */
    public ResultResponse submit(SubmitExamRequest request) {
        Exam exam = examRepository.findByCode(request.examCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe ningún examen con código " + request.examCode()));
        BusinessRuleException invalid = new BusinessRuleException(
                "El intento no es válido o ha superado el tiempo límite");
        ExamAttempt attempt = jwtService.parseAttemptToken(request.attemptToken())
                .filter(token -> token.examId().equals(exam.getId()))
                .flatMap(token -> attemptRepository.findById(token.attemptId()))
                .filter(a -> a.getExamId().equals(exam.getId()))
                .orElseThrow(() -> invalid);
        if (attempt.isSubmitted()) {
            throw new BusinessRuleException("Este intento ya se ha entregado");
        }
        // Basta con que el examen estuviera abierto al empezar: si caduca a mitad, la entrega vale.
        // Si el profesor lo cierra manualmente, ya no se aceptan entregas.
        if (!exam.isAccessibleAt(attempt.getStartedAt())) {
            throw new BusinessRuleException("Este examen no está disponible en este momento");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!attempt.isOpenAt(now, ExamService.attemptValidity(exam.getConfig()))) {
            throw invalid;
        }

        Map<Long, Integer> chosenByQuestion = chosenByQuestion(request);
        if (!chosenByQuestion.keySet().equals(Set.copyOf(exam.getQuestionIds()))) {
            throw new BusinessRuleException("Las respuestas no coinciden con las preguntas del examen");
        }
        // Se corrige contra la copia publicada (DE-03), no contra el banco
        List<Answer> answers = exam.getPublishedQuestions().stream()
                .map(q -> new Answer(q.getQuestionId(), chosenOption(q, chosenByQuestion), q.getCorrectAnswer()))
                .toList();

        attempt.markSubmitted(now);
        Result result = new Result(exam.getId(), attempt.getStudentUserId(), attempt.getStudentName(),
                attempt.getStudentClub(), attempt.getStudentEmail(), answers,
                (int) Duration.between(attempt.getStartedAt(), now).toSeconds(), now);
        return ResultResponse.forStudent(resultRepository.save(result), exam.getTitle(), exam.getConfig());
    }

    private static Map<Long, Integer> chosenByQuestion(SubmitExamRequest request) {
        Map<Long, Integer> chosen = new HashMap<>();
        for (SubmitExamRequest.Answer answer : request.answers()) {
            if (chosen.containsKey(answer.questionId())) {
                throw new BusinessRuleException("Hay respuestas duplicadas para la pregunta " + answer.questionId());
            }
            chosen.put(answer.questionId(), answer.chosenOption());
        }
        return chosen;
    }

    /** La opción elegida en esa pregunta (null = en blanco); tiene que existir en la pregunta publicada. */
    private static Integer chosenOption(PublishedQuestion question, Map<Long, Integer> chosenByQuestion) {
        Integer chosen = chosenByQuestion.get(question.getQuestionId());
        if (chosen != null && chosen >= question.getOptions().size()) {
            throw new BusinessRuleException("La opción " + chosen + " no existe en la pregunta " + question.getQuestionId());
        }
        return chosen;
    }

    // ───── Consultas del profesor ─────

    @Transactional(readOnly = true)
    public ResultResponse get(Long resultId, Long ownerId) {
        ResourceNotFoundException notFound = new ResourceNotFoundException("No existe un resultado con id " + resultId);
        Result result = resultRepository.findById(resultId).orElseThrow(() -> notFound);
        Exam exam = examRepository.findByIdAndOwnerId(result.getExamId(), ownerId).orElseThrow(() -> notFound);
        return ResultResponse.from(result, exam.getTitle());
    }

    @Transactional(readOnly = true)
    public List<ResultResponse> listByExam(Long examId, Long ownerId) {
        Exam exam = examService.getOwned(examId, ownerId);
        return resultRepository.findAllByExamIdOrderByCompletedAtDesc(examId).stream()
                .map(r -> ResultResponse.from(r, exam.getTitle()))
                .toList();
    }

    /** Excel con las entregas de un examen propio, ordenadas por club (RF-27). */
    @Transactional(readOnly = true)
    public ExportedFile exportByExam(Long examId, Long ownerId) {
        Exam exam = examService.getOwned(examId, ownerId);
        List<Result> results = resultRepository.findAllByExamIdOrderByCompletedAtDesc(examId);
        return new ExportedFile("resultados-" + asciiSlug(exam.getTitle()) + ".xlsx",
                ResultSpreadsheet.CONTENT_TYPE, ResultSpreadsheet.build(results));
    }

    /** «Examen de Árbitros 2026» → «examen-de-arbitros-2026»: seguro como nombre de fichero. */
    private static String asciiSlug(String text) {
        String slug = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "examen" : slug;
    }

    @Transactional(readOnly = true)
    public ExamStatisticsResponse statistics(Long examId, Long ownerId) {
        examService.getOwned(examId, ownerId);
        List<Result> results = resultRepository.findAllByExamIdOrderByCompletedAtDesc(examId);
        if (results.isEmpty()) {
            return new ExamStatisticsResponse(examId, 0, 0.0, 0, 0, 0, 0);
        }
        IntSummaryStatistics scores = results.stream().mapToInt(Result::getScore).summaryStatistics();
        int passed = (int) results.stream().filter(Result::isPassed).count();
        return new ExamStatisticsResponse(examId, results.size(), scores.getAverage(), passed,
                results.size() - passed, scores.getMax(), scores.getMin());
    }

    // ───── Consultas del alumno ─────

    /** Intentos del usuario. La nota solo se muestra si el examen lo permite. */
    @Transactional(readOnly = true)
    public List<ResultResponse> listMine(Long studentUserId) {
        List<Result> results = resultRepository.findAllByStudentUserIdOrderByCompletedAtDesc(studentUserId);
        Set<Long> examIds = results.stream().map(Result::getExamId).collect(Collectors.toSet());
        Map<Long, Exam> exams = examRepository.findAllById(examIds).stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));
        return results.stream()
                .map(r -> {
                    Exam exam = exams.get(r.getExamId());
                    return exam == null
                            ? ResultResponse.hidden(r, null)
                            : ResultResponse.forStudent(r, exam.getTitle(), exam.getConfig());
                })
                .toList();
    }

}
