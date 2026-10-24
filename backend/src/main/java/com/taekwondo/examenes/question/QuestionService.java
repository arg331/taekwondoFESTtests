package com.taekwondo.examenes.question;

import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.exam.ExamRepository;
import com.taekwondo.examenes.tag.TagService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.taekwondo.examenes.question.QuestionSpecifications.hasAnyTag;
import static com.taekwondo.examenes.question.QuestionSpecifications.notDeleted;
import static com.taekwondo.examenes.question.QuestionSpecifications.ownedBy;
import static com.taekwondo.examenes.question.QuestionSpecifications.textContains;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final TagService tagService;
    private final Clock clock;

    public QuestionService(QuestionRepository questionRepository, ExamRepository examRepository,
                           TagService tagService, Clock clock) {
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.tagService = tagService;
        this.clock = clock;
    }

    public QuestionResponse create(QuestionRequest request, Long ownerId) {
        Question question = new Question(ownerId);
        apply(question, request, ownerId);
        return QuestionResponse.from(questionRepository.save(question), 0);
    }

    public QuestionResponse update(Long questionId, QuestionRequest request, Long ownerId) {
        Question question = getOwned(questionId, ownerId);
        apply(question, request, ownerId);
        return withUsage(List.of(questionRepository.saveAndFlush(question))).get(0);
    }

    /**
     * Saca la pregunta del banco y de los borradores que la usen, sin borrarla: los exámenes publicados
     * guardaron su copia al publicarse y siguen igual. Se puede recuperar.
     */
    public void delete(Long questionId, Long ownerId) {
        Question question = getOwned(questionId, ownerId);
        examRepository.findAllContainingQuestion(questionId).stream()
                .filter(exam -> exam.isDraft())
                .forEach(exam -> exam.removeQuestion(questionId));
        question.delete(LocalDateTime.now(clock));
    }

    /** Vuelve al banco una pregunta eliminada. */
    public QuestionResponse restore(Long questionId, Long ownerId) {
        Question question = questionRepository.findByIdAndOwnerId(questionId, ownerId)
                .orElseThrow(() -> notFound(questionId));
        question.restore();
        return withUsage(List.of(question)).get(0);
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(Long questionId, Long ownerId) {
        return withUsage(List.of(getOwned(questionId, ownerId))).get(0);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> list(Long ownerId) {
        return withUsage(questionRepository.findAllByOwnerIdAndDeletedAtIsNull(ownerId));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> listDeleted(Long ownerId) {
        return withUsage(questionRepository.findAllByOwnerIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(ownerId));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> search(Set<Long> tagIds, String text, Long ownerId) {
        return withUsage(questionRepository.findAll(
                ownedBy(ownerId).and(notDeleted()).and(hasAnyTag(tagIds)).and(textContains(text))));
    }

    /** Las preguntas con cuántos exámenes las usan, en una sola consulta. */
    private List<QuestionResponse> withUsage(List<Question> questions) {
        if (questions.isEmpty()) return List.of();
        Map<Long, Long> usage = examRepository.countExamsByQuestion(
                        questions.stream().map(Question::getId).toList()).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return questions.stream()
                .map(q -> QuestionResponse.from(q, usage.getOrDefault(q.getId(), 0L).intValue()))
                .toList();
    }

    private void apply(Question question, QuestionRequest request, Long ownerId) {
        question.update(
                request.text().trim(),
                request.options().stream().map(String::trim).toList(),
                request.correctAnswers(),
                request.explanation(),
                request.grade(),
                request.type(),
                tagService.findOwnedTags(request.tagIds(), ownerId));
    }

    /** Una pregunta del banco del profesor; las eliminadas no cuentan hasta recuperarlas. */
    private Question getOwned(Long questionId, Long ownerId) {
        return questionRepository.findByIdAndOwnerIdAndDeletedAtIsNull(questionId, ownerId)
                .orElseThrow(() -> notFound(questionId));
    }

    private static ResourceNotFoundException notFound(Long questionId) {
        return new ResourceNotFoundException("No existe una pregunta con id " + questionId);
    }
}
