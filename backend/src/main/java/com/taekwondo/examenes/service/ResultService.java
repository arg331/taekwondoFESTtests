package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.result.ExamStatisticsResponse;
import com.taekwondo.examenes.dto.result.ResultResponse;
import com.taekwondo.examenes.dto.result.SubmitExamRequest;
import com.taekwondo.examenes.entity.Answer;
import com.taekwondo.examenes.entity.Exam;
import com.taekwondo.examenes.entity.Question;
import com.taekwondo.examenes.entity.Result;
import com.taekwondo.examenes.exception.BusinessRuleException;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.repository.ExamRepository;
import com.taekwondo.examenes.repository.ResultRepository;
import com.taekwondo.examenes.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
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
    private final JwtService jwtService;
    private final Clock clock;

    public ResultService(ResultRepository resultRepository, ExamRepository examRepository,
                         ExamService examService, JwtService jwtService, Clock clock) {
        this.resultRepository = resultRepository;
        this.examRepository = examRepository;
        this.examService = examService;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /**
     * Corrige y guarda la entrega de un alumno. El tiempo empleado se calcula
     * en el servidor a partir del token del intento, no lo decide el cliente.
     */
    public ResultResponse submit(SubmitExamRequest request, Long studentUserId) {
        Exam exam = examRepository.findByCode(request.examCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe ningún examen con código " + request.examCode()));

        JwtService.Attempt attempt = jwtService.parseAttemptToken(request.attemptToken())
                .filter(a -> a.examId().equals(exam.getId()))
                .orElseThrow(() -> new BusinessRuleException(
                        "El intento no es válido o ha superado el tiempo límite"));
        // Basta con que el examen estuviera abierto al empezar: si caduca a mitad, la entrega vale.
        // Si el profesor lo cierra manualmente, ya no se aceptan entregas.
        if (!exam.isAccessibleAt(LocalDateTime.ofInstant(attempt.startedAt(), clock.getZone()))) {
            throw new BusinessRuleException("Este examen no está disponible en este momento");
        }
        Duration elapsed = Duration.between(attempt.startedAt(), clock.instant());
        if (exam.getConfig().hasTimeLimit() && elapsed.compareTo(
                Duration.ofMinutes(exam.getConfig().getTimeLimitMinutes()).plus(ExamService.SUBMIT_GRACE)) > 0) {
            throw new BusinessRuleException("Se ha superado el tiempo límite del examen");
        }

        if (exam.requiresRegistration() && studentUserId == null) {
            throw new BusinessRuleException("Este examen requiere estar registrado e iniciar sesión");
        }
        String studentName = request.studentName().trim();
        boolean alreadyDone = studentUserId != null
                ? resultRepository.existsByExamIdAndStudentUserId(exam.getId(), studentUserId)
                : resultRepository.existsByExamIdAndStudentNameIgnoreCase(exam.getId(), studentName);
        if (alreadyDone) {
            throw new BusinessRuleException("Este estudiante ya ha realizado este examen");
        }

        Map<Long, Integer> chosenByQuestion = new HashMap<>();
        for (SubmitExamRequest.Answer answer : request.answers()) {
            if (chosenByQuestion.containsKey(answer.questionId())) {
                throw new BusinessRuleException("Hay respuestas duplicadas para la pregunta " + answer.questionId());
            }
            chosenByQuestion.put(answer.questionId(), answer.chosenOption());
        }
        if (!chosenByQuestion.keySet().equals(Set.copyOf(exam.getQuestionIds()))) {
            throw new BusinessRuleException("Las respuestas no coinciden con las preguntas del examen");
        }

        List<Answer> answers = examService.loadQuestionsInOrder(exam).stream()
                .map(q -> new Answer(q.getId(), chosenByQuestion.get(q.getId()), q.getCorrectAnswer()))
                .toList();

        Result result = new Result(exam.getId(), studentUserId, studentName,
                blankToNull(request.studentClub()), blankToNull(request.studentEmail()),
                answers, (int) elapsed.toSeconds(), LocalDateTime.now(clock));
        return ResultResponse.forStudent(resultRepository.save(result), exam.getTitle(),
                exam.getConfig().isShowScore());
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
                            ? ResultResponse.forStudent(r, null, false)
                            : ResultResponse.forStudent(r, exam.getTitle(), exam.getConfig().isShowScore());
                })
                .toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
