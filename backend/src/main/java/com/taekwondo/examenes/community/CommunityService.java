package com.taekwondo.examenes.community;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.exam.Exam;
import com.taekwondo.examenes.exam.ExamRepository;
import com.taekwondo.examenes.exam.ExamStatus;
import com.taekwondo.examenes.exam.PublishedQuestion;
import com.taekwondo.examenes.exam.Visibility;
import com.taekwondo.examenes.favorite.ExamFavorite;
import com.taekwondo.examenes.favorite.ExamFavoriteRepository;
import com.taekwondo.examenes.question.Question;
import com.taekwondo.examenes.question.QuestionRepository;
import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Exámenes de prueba (públicos) de otros profesores: verlos (RF-30), guardarlos en favoritos (RF-31) y copiar
 * sus preguntas a mi banco (DE-12). Se ve lo publicado, no el banco del autor, que puede haber cambiado.
 */
@Service
@Transactional
public class CommunityService {

    private static final List<ExamStatus> PUBLISHED_OR_CLOSED = List.of(ExamStatus.PUBLISHED, ExamStatus.EXPIRED);

    private final ExamRepository examRepository;
    private final ExamFavoriteRepository favoriteRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    public CommunityService(ExamRepository examRepository, ExamFavoriteRepository favoriteRepository,
                            QuestionRepository questionRepository, UserRepository userRepository) {
        this.examRepository = examRepository;
        this.favoriteRepository = favoriteRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    /** Los públicos de los demás, abiertos o cerrados, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<CommunityExamResponse> list(Long teacherId) {
        List<Exam> exams = examRepository.findAllByVisibilityAndStatusInAndOwnerIdNotOrderByCreatedAtDesc(
                Visibility.PUBLIC, PUBLISHED_OR_CLOSED, teacherId);
        Set<Long> favorites = favoriteRepository.findAllByProfessorIdOrderByCreatedAtDesc(teacherId).stream()
                .map(ExamFavorite::getExamId)
                .collect(Collectors.toSet());
        Map<Long, String> owners = ownerNames(exams);
        return exams.stream()
                .map(exam -> CommunityExamResponse.from(exam, owners.get(exam.getOwnerId()), favorites.contains(exam.getId())))
                .toList();
    }

    /** Mis favoritos que siguen siendo públicos, del último marcado al primero. */
    @Transactional(readOnly = true)
    public List<CommunityExamResponse> favorites(Long teacherId) {
        List<Long> ids = favoriteRepository.findAllByProfessorIdOrderByCreatedAtDesc(teacherId).stream()
                .map(ExamFavorite::getExamId)
                .toList();
        Map<Long, Exam> exams = examRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));
        List<Exam> stillPublic = ids.stream()
                .map(exams::get)
                .filter(exam -> exam != null && exam.getVisibility() == Visibility.PUBLIC)
                .toList();
        Map<Long, String> owners = ownerNames(stillPublic);
        return stillPublic.stream()
                .map(exam -> CommunityExamResponse.from(exam, owners.get(exam.getOwnerId()), true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommunityQuestionResponse> questions(Long examId, Long teacherId) {
        return getForeignPublic(examId, teacherId).getPublishedQuestions().stream()
                .map(CommunityQuestionResponse::from)
                .toList();
    }

    /**
     * Copia a mi banco las preguntas elegidas tal como se publicaron, con el grado y el tipo que tengan en el banco
     * del autor; los tags no, porque son suyos. Se salta las que ya tengo (mismo enunciado y opciones).
     */
    public CopyQuestionsResponse copyQuestions(Long examId, Collection<Long> questionIds, Long teacherId) {
        Map<Long, PublishedQuestion> published = getForeignPublic(examId, teacherId).getPublishedQuestions().stream()
                .collect(Collectors.toMap(PublishedQuestion::getQuestionId, Function.identity()));
        Set<Long> chosen = new LinkedHashSet<>(questionIds);
        if (!published.keySet().containsAll(chosen)) {
            throw new BusinessRuleException("Esa pregunta no está en este examen");
        }
        Map<Long, Question> sources = questionRepository.findAllById(chosen).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        Set<String> inMyBank = questionRepository.findAllByOwnerIdAndDeletedAtIsNull(teacherId).stream()
                .map(q -> fingerprint(q.getText(), q.getOptions()))
                .collect(Collectors.toCollection(HashSet::new));
        int copied = 0;
        for (Long id : chosen) {
            PublishedQuestion question = published.get(id);
            if (inMyBank.add(fingerprint(question.getText(), question.getOptions()))) {
                questionRepository.save(copyOf(question, sources.get(id), teacherId));
                copied++;
            }
        }
        return new CopyQuestionsResponse(copied, chosen.size() - copied);
    }

    private static Question copyOf(PublishedQuestion published, Question source, Long teacherId) {
        Question copy = new Question(teacherId);
        copy.update(published.getText(), published.getOptions(), published.getCorrectAnswers(), published.getExplanation(),
                source == null ? null : source.getGrade(), source == null ? null : source.getType(), Set.of());
        return copy;
    }

    /** Enunciado y opciones sin distinguir mayúsculas ni espacios de más: dos preguntas iguales a la vista. */
    private static String fingerprint(String text, List<String> options) {
        return Stream.concat(Stream.of(text), options.stream())
                .map(s -> s.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("\n"));
    }

    /** Público y publicado (abierto o cerrado) de otro profesor; de los míos, las preguntas ya están en mi banco. */
    private Exam getForeignPublic(Long examId, Long teacherId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un examen con id " + examId));
        if (exam.isOwnedBy(teacherId)) {
            throw new BusinessRuleException("Las preguntas de tus exámenes ya están en tu banco");
        }
        if (exam.getVisibility() != Visibility.PUBLIC || exam.isDraft()) {
            throw new ResourceNotFoundException("No existe un examen con id " + examId);
        }
        return exam;
    }

    private Map<Long, String> ownerNames(List<Exam> exams) {
        return userRepository.findAllById(exams.stream().map(Exam::getOwnerId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(User::getId, User::getDisplayName));
    }
}
