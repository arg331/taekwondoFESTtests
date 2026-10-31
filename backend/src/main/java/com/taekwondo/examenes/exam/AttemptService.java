package com.taekwondo.examenes.exam;

import com.taekwondo.examenes.attempt.AttemptQuestion;
import com.taekwondo.examenes.attempt.ExamAttempt;
import com.taekwondo.examenes.attempt.ExamAttemptRepository;
import com.taekwondo.examenes.attempt.StudentIdentity;
import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.question.PublicQuestionResponse;
import com.taekwondo.examenes.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * El intento del alumno (DE-04, RF-24): empezarlo, recuperarlo si recarga la página o pierde la
 * conexión, y guardar cada respuesta al contestarla. El intento recuerda en qué orden vio las
 * preguntas y las opciones, así que al recuperarlo todo sale igual y el reloj sigue donde iba.
 */
@Service
@Transactional
public class AttemptService {

    /** Margen sobre el tiempo límite para absorber la latencia del envío. */
    public static final Duration SUBMIT_GRACE = Duration.ofMinutes(2);
    private static final Duration UNLIMITED_ATTEMPT_VALIDITY = Duration.ofHours(12);
    private static final String OPEN_ATTEMPT_WITH_NAME = "Ya hay un intento en curso con ese nombre. Si es tuyo, "
            + "sigue en el navegador donde lo empezaste; si no, escribe tu nombre completo.";

    private final ExamService examService;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository attemptRepository;
    private final JwtService jwtService;
    private final Clock clock;

    public AttemptService(ExamService examService, ExamRepository examRepository,
                          ExamAttemptRepository attemptRepository, JwtService jwtService, Clock clock) {
        this.examService = examService;
        this.examRepository = examRepository;
        this.attemptRepository = attemptRepository;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    /**
     * Empieza un intento: lo registra a nombre del alumno y devuelve las preguntas sin la respuesta
     * correcta, barajadas según la configuración, y el token del intento. Cada alumno tiene tantos
     * intentos como fije el profesor (RF-52): los registrados se cuentan por su cuenta y los anónimos
     * por su nombre. El registrado que vuelve a empezar con un intento abierto lo recupera (RF-24); sin sesión,
     * el nombre no demuestra quién eres, así que ese intento solo se recupera con su token (DE-21).
     */
    public ExamAttemptResponse start(String code, StartAttemptRequest identity, Long studentUserId) {
        Exam exam = examService.getAccessible(code);
        if (exam.requiresRegistration() && studentUserId == null) {
            throw new BusinessRuleException("Este examen requiere estar registrado e iniciar sesión");
        }
        String studentName = identity.studentName().trim();
        LocalDateTime now = now();
        List<ExamAttempt> previous = attemptsOf(exam.getId(), studentUserId, StudentIdentity.nameKey(studentName));
        Optional<ExamAttempt> open = previous.stream()
                .filter(a -> a.isOpenAt(now, attemptValidity(exam.getConfig())))
                .findFirst();
        if (open.isPresent()) {
            if (studentUserId == null) {
                throw new BusinessRuleException(OPEN_ATTEMPT_WITH_NAME);
            }
            return response(exam, open.get(), numberOf(open.get(), previous), now);
        }
        // Cuentan los entregados y los que se quedaron sin entregar fuera de plazo
        if (exam.attemptsUsedUp(previous.size())) {
            throw new BusinessRuleException("Ya has hecho todos los intentos de este examen");
        }
        ExamAttempt attempt = attemptRepository.save(new ExamAttempt(exam.getId(), studentUserId, studentName,
                blankToNull(identity.studentClub()), blankToNull(identity.studentEmail()), now));
        attempt.assignQuestions(layout(exam));
        return response(exam, attempt, previous.size() + 1, now);
    }

    /** El intento tal como lo dejó el alumno: mismas preguntas, sus respuestas y el tiempo que le queda. */
    public ExamAttemptResponse resume(String code, ResumeAttemptRequest request) {
        Exam exam = findByCode(code);
        ExamAttempt attempt = openAttempt(exam, request.attemptToken());
        List<ExamAttempt> attempts = attemptsOf(exam.getId(), attempt.getStudentUserId(), attempt.getStudentKey());
        return response(exam, attempt, numberOf(attempt, attempts), now());
    }

    /** Guarda lo marcado en una pregunta en cuanto el alumno contesta (DE-05). */
    public void saveAnswer(String code, SaveAnswerRequest request) {
        Exam exam = findByCode(code);
        ExamAttempt attempt = openAttempt(exam, request.attemptToken());
        attempt.saveAnswer(request.questionId(), request.chosenOptions() == null ? List.of() : request.chosenOptions());
    }

    /**
     * El intento del token, si todavía se puede contestar: es de este examen, no se ha entregado,
     * el examen estaba abierto al empezarlo y no se le ha acabado el plazo.
     */
    public ExamAttempt openAttempt(Exam exam, String attemptToken) {
        BusinessRuleException invalid = new BusinessRuleException(
                "El intento no es válido o ha superado el tiempo límite");
        ExamAttempt attempt = jwtService.parseAttemptToken(attemptToken)
                .filter(token -> token.examId().equals(exam.getId()))
                .flatMap(token -> attemptRepository.findById(token.attemptId()))
                .filter(a -> a.getExamId().equals(exam.getId()))
                .orElseThrow(() -> invalid);
        if (attempt.isSubmitted()) {
            throw new BusinessRuleException("Este intento ya se ha entregado");
        }
        // Basta con que el examen estuviera abierto al empezar: si caduca a mitad o el profesor lo cierra
        // después, el intento sigue hasta que se le acaba el tiempo (DE-07)
        if (!exam.acceptsAttemptStartedAt(attempt.getStartedAt())) {
            throw new BusinessRuleException("Este examen no está disponible en este momento");
        }
        if (!attempt.isOpenAt(now(), attemptValidity(exam.getConfig()))) {
            throw invalid;
        }
        return attempt;
    }

    /** Cuánto dura un intento: el tiempo límite más el margen de entrega, o 12 horas sin límite (RN-07). */
    public static Duration attemptValidity(ExamConfig config) {
        return config.hasTimeLimit()
                ? Duration.ofMinutes(config.getTimeLimitMinutes()).plus(SUBMIT_GRACE)
                : UNLIMITED_ATTEMPT_VALIDITY;
    }

    /** Cuándo se acaba el tiempo del alumno (sin el margen de entrega): ahí se corrige si no lo entrega. */
    public static LocalDateTime deadline(ExamConfig config, LocalDateTime startedAt) {
        return config.hasTimeLimit()
                ? startedAt.plusMinutes(config.getTimeLimitMinutes())
                : startedAt.plus(UNLIMITED_ATTEMPT_VALIDITY);
    }

    // ───── Auxiliares ─────

    private ExamAttemptResponse response(Exam exam, ExamAttempt attempt, int number, LocalDateTime now) {
        // Los intentos empezados antes de guardar las preguntas (V14) las reciben al recuperarse
        if (attempt.getQuestions().isEmpty()) {
            attempt.assignQuestions(layout(exam));
        }
        Map<Long, PublishedQuestion> published = exam.getPublishedQuestions().stream()
                .collect(Collectors.toMap(PublishedQuestion::getQuestionId, Function.identity()));
        List<PublicQuestionResponse> questions = attempt.getQuestions().stream()
                .map(q -> toPublicQuestion(published.get(q.getQuestionId()), q.getOptionOrder()))
                .toList();
        String token = jwtService.generateAttemptToken(attempt.getId(), exam.getId(),
                attempt.getStartedAt().atZone(clock.getZone()).toInstant(), attemptValidity(exam.getConfig()));
        return new ExamAttemptResponse(token, questions, number, exam.getMaxAttempts(), attempt.getChosenOptions(),
                secondsLeft(exam.getConfig(), attempt, now));
    }

    /** Orden de preguntas y opciones de un intento nuevo, barajado según la configuración. */
    private static List<AttemptQuestion> layout(Exam exam) {
        ExamConfig config = exam.getConfig();
        List<AttemptQuestion> layout = new ArrayList<>(exam.getPublishedQuestions().stream()
                .map(q -> new AttemptQuestion(q.getQuestionId(),
                        optionOrder(q.getOptions().size(), config.isRandomizeOptions())))
                .toList());
        if (config.isRandomizeQuestionOrder()) {
            Collections.shuffle(layout);
        }
        return layout;
    }

    private static List<Integer> optionOrder(int size, boolean shuffle) {
        List<Integer> order = new ArrayList<>(IntStream.range(0, size).boxed().toList());
        if (shuffle) {
            Collections.shuffle(order);
        }
        return order;
    }

    private static PublicQuestionResponse toPublicQuestion(PublishedQuestion question, List<Integer> optionOrder) {
        List<PublicQuestionResponse.Option> options = optionOrder.stream()
                .map(i -> new PublicQuestionResponse.Option(i, question.getOptions().get(i)))
                .toList();
        return new PublicQuestionResponse(question.getQuestionId(), question.getText(), options,
                question.getCorrectAnswers().size() > 1);
    }

    /** Segundos que le quedan al alumno; null sin tiempo límite. */
    private static Long secondsLeft(ExamConfig config, ExamAttempt attempt, LocalDateTime now) {
        if (!config.hasTimeLimit()) return null;
        return Math.max(0, Duration.between(now, deadline(config, attempt.getStartedAt())).toSeconds());
    }

    /** Qué número de intento es entre los del alumno (RF-52). */
    private static int numberOf(ExamAttempt attempt, List<ExamAttempt> attempts) {
        return (int) attempts.stream().filter(a -> a.getId() <= attempt.getId()).count();
    }

    private List<ExamAttempt> attemptsOf(Long examId, Long studentUserId, String studentKey) {
        return studentUserId != null
                ? attemptRepository.findAllByExamIdAndStudentUserId(examId, studentUserId)
                : attemptRepository.findAllByExamIdAndStudentUserIdIsNullAndStudentKey(examId, studentKey);
    }

    private Exam findByCode(String code) {
        return examRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ningún examen con código " + code));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
