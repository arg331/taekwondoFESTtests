package com.taekwondo.examenes.result;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.exam.Exam;
import com.taekwondo.examenes.exam.ExamRepository;
import com.taekwondo.examenes.exam.ExamService;
import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Informes del profesor sobre las entregas: el acierto de cada pregunta de un examen (RF-28) y los
 * resultados de un usuario (RF-55). Antes de contar, corrige los intentos caducados (RF-24).
 */
@Service
@Transactional
public class ResultReportService {

    private final ResultRepository resultRepository;
    private final ResultService resultService;
    private final ExamService examService;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;

    public ResultReportService(ResultRepository resultRepository, ResultService resultService,
                               ExamService examService, ExamRepository examRepository, UserRepository userRepository) {
        this.resultRepository = resultRepository;
        this.resultService = resultService;
        this.examService = examService;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
    }

    /** Cada pregunta del examen, en su orden, con cómo se ha respondido en todas las entregas. */
    public List<QuestionStatsResponse> questionStats(Long examId, Long ownerId) {
        Exam exam = examService.getOwned(examId, ownerId);
        if (exam.isDraft()) {
            throw new BusinessRuleException("El examen todavía no está publicado");
        }
        resultService.autoSubmitExpired(examId);
        Map<Long, List<Answer>> answersByQuestion = resultRepository.findAllByExamIdOrderByCompletedAtDesc(examId)
                .stream()
                .flatMap(r -> r.getAnswers().stream())
                .collect(Collectors.groupingBy(Answer::getQuestionId));
        return exam.getPublishedQuestions().stream()
                .map(q -> QuestionStatsResponse.of(q, answersByQuestion.getOrDefault(q.getQuestionId(), List.of())))
                .toList();
    }

    /**
     * Las entregas de un usuario con la vista completa del profesor. El profesor ve las de sus propios
     * exámenes (RN-14); el administrador, todas.
     */
    public UserResultsResponse userResults(Long userId, Long requesterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con id " + userId));
        boolean admin = userRepository.findById(requesterId).map(User::isAdmin).orElse(false);
        resultService.autoSubmitExpiredOf(userId);
        List<Result> results = resultRepository.findAllByStudentUserIdOrderByCompletedAtDesc(userId);
        Map<Long, Exam> exams = examRepository.findAllById(results.stream().map(Result::getExamId)
                        .collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));
        List<ResultResponse> visible = results.stream()
                .filter(r -> exams.containsKey(r.getExamId()))
                .filter(r -> admin || exams.get(r.getExamId()).getOwnerId().equals(requesterId))
                .map(r -> ResultResponse.from(r, exams.get(r.getExamId()).getTitle()))
                .toList();
        return new UserResultsResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getClub() == null ? null : user.getClub().getName(), visible);
    }
}
