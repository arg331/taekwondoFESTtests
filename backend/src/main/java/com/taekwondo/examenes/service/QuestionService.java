package com.taekwondo.examenes.service;

import com.taekwondo.examenes.dto.question.QuestionRequest;
import com.taekwondo.examenes.dto.question.QuestionResponse;
import com.taekwondo.examenes.entity.Exam;
import com.taekwondo.examenes.entity.Question;
import com.taekwondo.examenes.exception.BusinessRuleException;
import com.taekwondo.examenes.exception.ResourceNotFoundException;
import com.taekwondo.examenes.repository.ExamRepository;
import com.taekwondo.examenes.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static com.taekwondo.examenes.repository.QuestionSpecifications.hasAnyTag;
import static com.taekwondo.examenes.repository.QuestionSpecifications.ownedBy;
import static com.taekwondo.examenes.repository.QuestionSpecifications.textContains;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final ExamRepository examRepository;
    private final TagService tagService;

    public QuestionService(QuestionRepository questionRepository, ExamRepository examRepository,
                           TagService tagService) {
        this.questionRepository = questionRepository;
        this.examRepository = examRepository;
        this.tagService = tagService;
    }

    public QuestionResponse create(QuestionRequest request, Long ownerId) {
        Question question = new Question(ownerId);
        apply(question, request, ownerId);
        return QuestionResponse.from(questionRepository.save(question));
    }

    public QuestionResponse update(Long questionId, QuestionRequest request, Long ownerId) {
        Question question = getOwned(questionId, ownerId);
        apply(question, request, ownerId);
        return QuestionResponse.from(questionRepository.saveAndFlush(question));
    }

    /**
     * Borra la pregunta y la quita de los borradores que la usen. Si ya está en
     * un examen publicado o cerrado no se puede borrar: ese examen dejaría de
     * poder entregarse y sus resultados perderían la referencia.
     */
    public void delete(Long questionId, Long ownerId) {
        Question question = getOwned(questionId, ownerId);
        List<Exam> exams = examRepository.findAllContainingQuestion(questionId);
        List<String> nonDraftTitles = exams.stream()
                .filter(exam -> !exam.isDraft())
                .map(Exam::getTitle)
                .toList();
        if (!nonDraftTitles.isEmpty()) {
            throw new BusinessRuleException("La pregunta se usa en exámenes ya publicados: "
                    + String.join(", ", nonDraftTitles));
        }
        exams.forEach(exam -> exam.removeQuestion(questionId));
        questionRepository.delete(question);
    }

    @Transactional(readOnly = true)
    public QuestionResponse get(Long questionId, Long ownerId) {
        return QuestionResponse.from(getOwned(questionId, ownerId));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> list(Long ownerId) {
        return questionRepository.findAllByOwnerId(ownerId).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> search(Set<Long> tagIds, String text, Long ownerId) {
        return questionRepository.findAll(ownedBy(ownerId).and(hasAnyTag(tagIds)).and(textContains(text)))
                .stream()
                .map(QuestionResponse::from)
                .toList();
    }

    private void apply(Question question, QuestionRequest request, Long ownerId) {
        question.update(
                request.text().trim(),
                request.options().stream().map(String::trim).toList(),
                request.correctAnswer(),
                request.explanation(),
                request.difficulty(),
                tagService.findOwnedTags(request.tagIds(), ownerId));
    }

    private Question getOwned(Long questionId, Long ownerId) {
        return questionRepository.findByIdAndOwnerId(questionId, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe una pregunta con id " + questionId));
    }
}
