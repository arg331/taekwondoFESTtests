package com.taekwondo.examenes.exam;

import com.taekwondo.examenes.attempt.ExamAttempt;
import com.taekwondo.examenes.attempt.ExamAttemptRepository;
import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.question.PublicQuestionResponse;
import com.taekwondo.examenes.question.Question;
import com.taekwondo.examenes.question.QuestionRepository;
import com.taekwondo.examenes.security.JwtService;
import com.taekwondo.examenes.tag.Tag;
import com.taekwondo.examenes.tag.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.taekwondo.examenes.question.QuestionSpecifications.hasAnyTag;
import static com.taekwondo.examenes.question.QuestionSpecifications.ownedBy;

@Service
@Transactional
public class ExamService {

    /** Margen sobre el tiempo límite para absorber la latencia del envío. */
    public static final Duration SUBMIT_GRACE = Duration.ofMinutes(2);
    private static final Duration UNLIMITED_ATTEMPT_VALIDITY = Duration.ofHours(12);
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository attemptRepository;
    private final TagService tagService;
    private final JwtService jwtService;
    private final Clock clock;

    public ExamService(ExamRepository examRepository, QuestionRepository questionRepository,
                       ExamAttemptRepository attemptRepository, TagService tagService,
                       JwtService jwtService, Clock clock) {
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.tagService = tagService;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    // ───── Creación ─────

    public ExamResponse createDraft(ExamDraftRequest request, Long ownerId) {
        Exam exam = new Exam(request.title().trim(), ownerId, toConfig(request), Set.of());
        return ExamResponse.from(examRepository.save(exam));
    }

    /** Crea un borrador con preguntas aleatorias del profesor, opcionalmente filtradas por tags. */
    public ExamResponse preGenerateDraft(ExamDraftRequest request, Long ownerId) {
        Set<Tag> tags = tagService.findOwnedTags(request.requiredAnyOfTagIds(), ownerId);
        Set<Long> tagIds = tags.stream().map(Tag::getId).collect(Collectors.toSet());

        List<Question> candidates = new ArrayList<>(
                questionRepository.findAll(ownedBy(ownerId).and(hasAnyTag(tagIds))));
        if (candidates.size() < request.numberOfQuestions()) {
            throw new BusinessRuleException("No hay suficientes preguntas con los tags solicitados: se necesitan "
                    + request.numberOfQuestions() + " y solo hay " + candidates.size());
        }
        Collections.shuffle(candidates);

        Exam exam = new Exam(request.title().trim(), ownerId, toConfig(request), tags);
        exam.replaceQuestions(candidates.stream()
                .limit(request.numberOfQuestions())
                .map(Question::getId)
                .toList());
        return ExamResponse.from(examRepository.save(exam));
    }

    /** Copia un examen propio, en cualquier estado, como borrador nuevo para ampliarlo. */
    public ExamResponse copy(Long examId, Long ownerId) {
        Exam original = getOwned(examId, ownerId);
        return ExamResponse.from(examRepository.save(original.copyAs(copyTitle(original.getTitle()))));
    }

    private static String copyTitle(String title) {
        String copyTitle = "Copia de " + title;
        return copyTitle.length() <= Exam.MAX_TITLE_LENGTH ? copyTitle : copyTitle.substring(0, Exam.MAX_TITLE_LENGTH);
    }

    // ───── Edición ─────

    public ExamResponse rename(Long examId, String newTitle, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        exam.rename(newTitle.trim());
        return ExamResponse.from(exam);
    }

    public ExamResponse changeConfig(Long examId, ExamConfigRequest request, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        exam.changeConfig(new ExamConfig(request.numberOfQuestions(), request.timeLimitMinutes(),
                request.showScore(), showAnswers(request.showScore(), request.showAnswers()),
                request.randomizeOptions(), request.randomizeQuestionOrder()));
        return ExamResponse.from(exam);
    }

    public ExamResponse updateQuestions(Long examId, List<Long> questionIds, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        Set<Long> uniqueIds = new HashSet<>(questionIds);
        if (uniqueIds.size() != questionIds.size()) {
            throw new BusinessRuleException("Hay preguntas duplicadas en la lista");
        }
        if (questionRepository.findAllByIdInAndOwnerId(uniqueIds, ownerId).size() != uniqueIds.size()) {
            throw new BusinessRuleException("Alguna de las preguntas indicadas no existe");
        }
        exam.replaceQuestions(questionIds);
        return ExamResponse.from(exam);
    }

    public ExamResponse changeVisibility(Long examId, Visibility visibility, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        exam.changeVisibility(visibility);
        return ExamResponse.from(exam);
    }

    // ───── Ciclo de vida ─────

    public ExamResponse publish(Long examId, PublishExamRequest request, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        // Sin fecha, el examen no caduca: queda abierto hasta que se cierre (RF-48)
        LocalDateTime expiresAt = request.expiresAt();
        if (expiresAt != null) requireFuture(expiresAt);
        ExamAccessMode accessMode = request.accessMode() != null ? request.accessMode() : ExamAccessMode.OPEN;
        // Sin indicar nada, 1 intento
        Integer maxAttempts = request.maxAttempts() == null && !Boolean.TRUE.equals(request.unlimitedAttempts())
                ? Integer.valueOf(Exam.MIN_ATTEMPTS)
                : attempts(request.maxAttempts(), request.unlimitedAttempts());

        exam.publish(generateUniqueCode(), request.visibility(), accessMode, expiresAt, maxAttempts,
                loadBankQuestionsInOrder(exam));
        return ExamResponse.from(exam);
    }

    /** Subir o bajar los intentos de un examen publicado (RF-52). */
    public ExamResponse changeAttempts(Long examId, ChangeAttemptsRequest request, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        exam.changeMaxAttempts(attempts(request.maxAttempts(), request.unlimitedAttempts()));
        return ExamResponse.from(exam);
    }

    /** Un número de 1 a 4 o «sin límite» (null), nunca las dos cosas ni ninguna. */
    private static Integer attempts(Integer maxAttempts, Boolean unlimitedAttempts) {
        boolean unlimited = Boolean.TRUE.equals(unlimitedAttempts);
        if (unlimited && maxAttempts != null) {
            throw new BusinessRuleException("Indica un número de intentos o «sin límite», no las dos cosas");
        }
        if (!unlimited && maxAttempts == null) {
            throw new BusinessRuleException("Indica un número de intentos o «sin límite»");
        }
        return unlimited ? null : maxAttempts;
    }

    public ExamResponse close(Long examId, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        exam.close();
        return ExamResponse.from(exam);
    }

    public ExamResponse reopen(Long examId, LocalDateTime newExpiresAt, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        if (newExpiresAt != null) requireFuture(newExpiresAt);
        exam.reopen(newExpiresAt);
        return ExamResponse.from(exam);
    }

    public ExamResponse changeExpiration(Long examId, LocalDateTime newExpiresAt, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        if (newExpiresAt != null) requireFuture(newExpiresAt);
        exam.changeExpiration(newExpiresAt);
        return ExamResponse.from(exam);
    }

    public void deleteDraft(Long examId, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        if (!exam.isDraft()) {
            throw new BusinessRuleException("Solo se pueden eliminar exámenes en borrador (estado actual: "
                    + exam.getStatus() + ")");
        }
        examRepository.delete(exam);
    }

    // ───── Consultas del profesor ─────

    @Transactional(readOnly = true)
    public ExamResponse get(Long examId, Long ownerId) {
        return ExamResponse.from(getOwned(examId, ownerId));
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> listMine(Long ownerId) {
        return examRepository.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(ExamResponse::from)
                .toList();
    }

    /** Exámenes públicos y abiertos ahora mismo, de otros profesores. */
    @Transactional(readOnly = true)
    public List<ExamResponse> listPublic(Long requesterId) {
        LocalDateTime now = now();
        return examRepository.findAllByVisibilityAndStatusAndOwnerIdNot(Visibility.PUBLIC, ExamStatus.PUBLISHED,
                        requesterId).stream()
                .filter(exam -> exam.isAccessibleAt(now))
                .map(ExamResponse::from)
                .toList();
    }

    // ───── Flujo del alumno (público) ─────

    @Transactional(readOnly = true)
    public ExamResponse getAccessibleByCode(String code) {
        return ExamResponse.from(getAccessible(code));
    }

    /**
     * Empieza un intento (DE-04): lo registra a nombre del alumno y devuelve las preguntas sin la
     * respuesta correcta, barajadas según la configuración, y el token del intento. Cada alumno
     * tiene un intento abierto como mucho y tantos en total como fije el profesor (RF-52):
     * los registrados se cuentan por su cuenta y los anónimos por su nombre.
     */
    public ExamAttemptResponse startAttempt(String code, StartAttemptRequest identity, Long studentUserId) {
        Exam exam = getAccessible(code);
        if (exam.requiresRegistration() && studentUserId == null) {
            throw new BusinessRuleException("Este examen requiere estar registrado e iniciar sesión");
        }
        String studentName = identity.studentName().trim();
        LocalDateTime now = now();
        Duration validity = attemptValidity(exam.getConfig());
        List<ExamAttempt> previous = studentUserId != null
                ? attemptRepository.findAllByExamIdAndStudentUserId(exam.getId(), studentUserId)
                : attemptRepository.findAllByExamIdAndStudentUserIdIsNullAndStudentNameIgnoreCase(exam.getId(), studentName);
        if (previous.stream().anyMatch(a -> a.isOpenAt(now, validity))) {
            throw new BusinessRuleException("Ya tienes un intento empezado en este examen");
        }
        // Cuentan los entregados y los que se quedaron sin entregar fuera de plazo
        if (exam.attemptsUsedUp(previous.size())) {
            throw new BusinessRuleException("Ya has hecho todos los intentos de este examen");
        }
        ExamAttempt attempt = attemptRepository.save(new ExamAttempt(exam.getId(), studentUserId, studentName,
                blankToNull(identity.studentClub()), blankToNull(identity.studentEmail()), now));

        ExamConfig config = exam.getConfig();
        List<PublicQuestionResponse> questions = new ArrayList<>(exam.getPublishedQuestions().stream()
                .map(q -> toPublicQuestion(q, config.isRandomizeOptions()))
                .toList());
        if (config.isRandomizeQuestionOrder()) {
            Collections.shuffle(questions);
        }

        String token = jwtService.generateAttemptToken(attempt.getId(), exam.getId(),
                now.atZone(clock.getZone()).toInstant(), validity);
        return new ExamAttemptResponse(token, questions, previous.size() + 1, exam.getMaxAttempts());
    }

    /** Cuánto dura un intento: el tiempo límite más el margen de entrega, o 12 horas sin límite (RN-07). */
    public static Duration attemptValidity(ExamConfig config) {
        return config.hasTimeLimit()
                ? Duration.ofMinutes(config.getTimeLimitMinutes()).plus(SUBMIT_GRACE)
                : UNLIMITED_ATTEMPT_VALIDITY;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Preguntas tal como se publicaron (DE-03), con solución y explicación, y si se han
     * editado en el banco después. Solo el profesor del examen.
     */
    @Transactional(readOnly = true)
    public List<PublishedQuestionResponse> publishedQuestions(Long examId, Long ownerId) {
        Exam exam = getOwned(examId, ownerId);
        if (exam.isDraft()) {
            throw new BusinessRuleException("El examen todavía no está publicado");
        }
        Map<Long, Question> bank = questionRepository.findAllById(exam.getQuestionIds()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        return exam.getPublishedQuestions().stream()
                .map(pq -> PublishedQuestionResponse.from(pq, bank.get(pq.getQuestionId())))
                .toList();
    }

    /** Preguntas del banco en el orden del examen, para copiarlas al publicar; falla si alguna ya no existe. */
    private List<Question> loadBankQuestionsInOrder(Exam exam) {
        Map<Long, Question> byId = questionRepository.findAllById(exam.getQuestionIds()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        if (byId.size() != exam.getQuestionIds().size()) {
            throw new BusinessRuleException("Alguna pregunta de este examen ya no existe");
        }
        return exam.getQuestionIds().stream().map(byId::get).toList();
    }

    Exam getAccessible(String code) {
        Exam exam = examRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No existe ningún examen con código " + code));
        if (!exam.isAccessibleAt(now())) {
            throw new BusinessRuleException("Este examen no está disponible en este momento");
        }
        return exam;
    }

    public Exam getOwned(Long examId, Long ownerId) {
        return examRepository.findByIdAndOwnerId(examId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un examen con id " + examId));
    }

    // ───── Auxiliares ─────

    private PublicQuestionResponse toPublicQuestion(PublishedQuestion question, boolean shuffleOptions) {
        List<PublicQuestionResponse.Option> options = new ArrayList<>(IntStream
                .range(0, question.getOptions().size())
                .mapToObj(i -> new PublicQuestionResponse.Option(i, question.getOptions().get(i)))
                .toList());
        if (shuffleOptions) {
            Collections.shuffle(options);
        }
        return new PublicQuestionResponse(question.getQuestionId(), question.getText(), options);
    }

    private ExamConfig toConfig(ExamDraftRequest r) {
        return new ExamConfig(r.numberOfQuestions(), r.timeLimitMinutes(), r.showScore(),
                showAnswers(r.showScore(), r.showAnswers()), r.randomizeOptions(), r.randomizeQuestionOrder());
    }

    /** Las respuestas solo se muestran con la nota; sin indicarlo, se muestran si se muestra la nota (RF-53). */
    private static boolean showAnswers(boolean showScore, Boolean requested) {
        return showScore && (requested == null || requested);
    }

    private void requireFuture(LocalDateTime date) {
        if (!date.isAfter(now())) {
            throw new BusinessRuleException("La fecha de expiración debe ser posterior al momento actual");
        }
    }

    /** Código corto para el QR, con reintentos por si colisiona. */
    private String generateUniqueCode() {
        for (int i = 0; i < MAX_CODE_ATTEMPTS; i++) {
            String code = "EXM-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase(Locale.ROOT);
            if (!examRepository.existsByCode(code)) return code;
        }
        throw new IllegalStateException("No se pudo generar un código único tras " + MAX_CODE_ATTEMPTS + " intentos");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
