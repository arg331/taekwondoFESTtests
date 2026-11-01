package com.taekwondo.examenes.result;

import com.taekwondo.examenes.attempt.ExamAttempt;
import com.taekwondo.examenes.attempt.ExamAttemptRepository;
import com.taekwondo.examenes.attempt.StudentIdentity;
import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.exam.AttemptService;
import com.taekwondo.examenes.exam.Exam;
import com.taekwondo.examenes.exam.ExamConfig;
import com.taekwondo.examenes.exam.ExamRepository;
import com.taekwondo.examenes.exam.ExamService;
import com.taekwondo.examenes.exam.PublishedQuestion;
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
    private final AttemptService attemptService;
    private final Clock clock;

    public ResultService(ResultRepository resultRepository, ExamRepository examRepository,
                         ExamService examService, ExamAttemptRepository attemptRepository,
                         AttemptService attemptService, Clock clock) {
        this.resultRepository = resultRepository;
        this.examRepository = examRepository;
        this.examService = examService;
        this.attemptRepository = attemptRepository;
        this.attemptService = attemptService;
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
        // Si caduca a mitad o el profesor lo cierra después de empezar, la entrega vale en su tiempo (DE-07)
        ExamAttempt attempt = attemptService.openAttempt(exam, request.attemptToken());

        Map<Long, List<Integer>> chosenByQuestion = chosenByQuestion(request);
        if (!chosenByQuestion.keySet().equals(Set.copyOf(exam.getQuestionIds()))) {
            throw new BusinessRuleException("Las respuestas no coinciden con las preguntas del examen");
        }
        List<Answer> answers = grade(exam, q -> chosenOptions(q, chosenByQuestion));

        LocalDateTime now = LocalDateTime.now(clock);
        if (attemptRepository.markSubmitted(attempt.getId(), now) == 0) {
            throw new BusinessRuleException("Este intento ya se ha entregado");
        }
        Result result = resultRepository.save(toResult(exam, attempt, answers, now, false));
        return ResultResponse.forStudent(result, exam.getTitle(), exam.getConfig());
    }

    // ───── Entrega automática (RF-24) ─────

    /** Corrige los intentos sin entregar a los que se les ha acabado el plazo. La llama una tarea periódica. */
    public void autoSubmitExpired() {
        autoSubmit(attemptRepository.findAllBySubmittedAtIsNull());
    }

    /** Lo mismo, solo en un examen: antes de enseñar sus resultados al profesor. */
    void autoSubmitExpired(Long examId) {
        autoSubmit(attemptRepository.findAllByExamIdAndSubmittedAtIsNull(examId));
    }

    /** Lo mismo, solo los de un usuario: antes de enseñar sus resultados (RF-55). */
    void autoSubmitExpiredOf(Long studentUserId) {
        autoSubmit(attemptRepository.findAllByStudentUserIdAndSubmittedAtIsNull(studentUserId));
    }

    private void autoSubmit(List<ExamAttempt> open) {
        if (open.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now(clock);
        Map<Long, Exam> exams = examRepository.findAllById(open.stream().map(ExamAttempt::getExamId)
                        .collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));
        open.stream()
                .filter(a -> a.isExpiredAt(now, AttemptService.attemptValidity(exams.get(a.getExamId()).getConfig())))
                .forEach(a -> autoSubmit(exams.get(a.getExamId()), a));
    }

    /** Se entrega cuando se le acabó el tiempo, con lo que dejó marcado; lo que no contestó cuenta como fallo. */
    private void autoSubmit(Exam exam, ExamAttempt attempt) {
        Map<Long, List<Integer>> saved = attempt.getChosenOptions();
        List<Answer> answers = grade(exam, q -> saved.getOrDefault(q.getQuestionId(), List.of()));
        LocalDateTime deadline = AttemptService.deadline(exam.getConfig(), attempt.getStartedAt());
        // Otra petición puede haberlo corregido a la vez: entonces no se repite
        if (attemptRepository.markSubmitted(attempt.getId(), deadline) == 0) return;
        resultRepository.save(toResult(exam, attempt, answers, deadline, true));
    }

    /** Se corrige contra la copia publicada (DE-03), no contra el banco. */
    private static List<Answer> grade(Exam exam, Function<PublishedQuestion, List<Integer>> chosen) {
        return exam.getPublishedQuestions().stream()
                .map(q -> new Answer(q.getQuestionId(), chosen.apply(q), q.getCorrectAnswers()))
                .toList();
    }

    private static Result toResult(Exam exam, ExamAttempt attempt, List<Answer> answers, LocalDateTime completedAt,
                                   boolean autoSubmitted) {
        return new Result(exam.getId(), attempt.getStudentUserId(), attempt.getStudentName(),
                attempt.getStudentClub(), attempt.getStudentEmail(), answers,
                (int) Duration.between(attempt.getStartedAt(), completedAt).toSeconds(), completedAt, autoSubmitted);
    }

    private static Map<Long, List<Integer>> chosenByQuestion(SubmitExamRequest request) {
        Map<Long, List<Integer>> chosen = new HashMap<>();
        for (SubmitExamRequest.Answer answer : request.answers()) {
            if (chosen.containsKey(answer.questionId())) {
                throw new BusinessRuleException("Hay respuestas duplicadas para la pregunta " + answer.questionId());
            }
            chosen.put(answer.questionId(), answer.chosenOptions() == null ? List.of() : answer.chosenOptions());
        }
        return chosen;
    }

    /** Las opciones marcadas en esa pregunta (vacío = en blanco); tienen que existir en la pregunta publicada. */
    private static List<Integer> chosenOptions(PublishedQuestion question, Map<Long, List<Integer>> chosenByQuestion) {
        List<Integer> chosen = chosenByQuestion.get(question.getQuestionId());
        chosen.stream().filter(option -> option >= question.getOptions().size()).findFirst().ifPresent(option -> {
            throw new BusinessRuleException("La opción " + option + " no existe en la pregunta " + question.getQuestionId());
        });
        return chosen;
    }

    // ───── Consultas del profesor ─────

    public List<ResultResponse> listByExam(Long examId, Long teacherId) {
        Exam exam = examService.getViewable(examId, teacherId);
        autoSubmitExpired(examId);
        return resultRepository.findAllByExamIdOrderByCompletedAtDesc(examId).stream()
                .map(r -> ResultResponse.from(r, exam))
                .toList();
    }

    /** Excel con las entregas de un examen propio, ordenadas por club (RF-27). */
    public ExportedFile exportByExam(Long examId, Long teacherId) {
        Exam exam = examService.getViewable(examId, teacherId);
        autoSubmitExpired(examId);
        List<Result> results = resultRepository.findAllByExamIdOrderByCompletedAtDesc(examId);
        return new ExportedFile("resultados-" + asciiSlug(exam.getTitle()) + ".xlsx",
                ResultSpreadsheet.CONTENT_TYPE, ResultSpreadsheet.build(results, exam.getConfig()));
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

    public ExamStatisticsResponse statistics(Long examId, Long teacherId) {
        ExamConfig config = examService.getViewable(examId, teacherId).getConfig();
        autoSubmitExpired(examId);
        List<Result> results = resultRepository.findAllByExamIdOrderByCompletedAtDesc(examId);
        if (results.isEmpty()) {
            return new ExamStatisticsResponse(examId, 0, 0, 0.0, 0, 0, 0, 0, config.getPassingScore());
        }
        // Cada alumno cuenta una vez, con su mejor nota (RF-54)
        Map<String, Integer> bestByStudent = results.stream().collect(Collectors.toMap(
                r -> StudentIdentity.key(r.getStudentUserId(), r.getStudentName()), Result::getScore, Math::max));
        IntSummaryStatistics scores = bestByStudent.values().stream().mapToInt(Integer::intValue).summaryStatistics();
        int passed = (int) bestByStudent.values().stream().filter(config::passes).count();
        return new ExamStatisticsResponse(examId, results.size(), bestByStudent.size(), scores.getAverage(), passed,
                bestByStudent.size() - passed, scores.getMax(), scores.getMin(), config.getPassingScore());
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
