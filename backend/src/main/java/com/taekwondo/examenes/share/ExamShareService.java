package com.taekwondo.examenes.share;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.exam.Exam;
import com.taekwondo.examenes.exam.ExamRepository;
import com.taekwondo.examenes.exam.ExamResponse;
import com.taekwondo.examenes.exam.ExamService;
import com.taekwondo.examenes.question.Question;
import com.taekwondo.examenes.question.QuestionRepository;
import com.taekwondo.examenes.question.QuestionResponse;
import com.taekwondo.examenes.share.ShareDtos.ExamShareResponse;
import com.taekwondo.examenes.share.ShareDtos.InvitationResponse;
import com.taekwondo.examenes.share.ShareDtos.ShareCandidateResponse;
import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRepository;
import com.taekwondo.examenes.user.UserRole;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compartir un examen con otros profesores (RF-51). El dueño invita; el invitado acepta o rechaza. Al
 * aceptar consulta el mismo examen y todos sus resultados (ExamService.getViewable), pero cambiarlo
 * sigue siendo solo del dueño.
 */
@Service
@Transactional
public class ExamShareService {

    private static final Set<UserRole> TEACHER_ROLES = Set.of(UserRole.TEACHER, UserRole.ADMIN);
    private static final int MAX_CANDIDATES = 20;

    private final ExamShareRepository shareRepository;
    private final ExamService examService;
    private final ExamRepository examRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final Clock clock;

    public ExamShareService(ExamShareRepository shareRepository, ExamService examService,
                            ExamRepository examRepository, UserRepository userRepository,
                            QuestionRepository questionRepository, Clock clock) {
        this.shareRepository = shareRepository;
        this.examService = examService;
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.clock = clock;
    }

    // ───── El dueño ─────

    public ExamShareResponse invite(Long examId, Long ownerId, Long teacherId) {
        examService.getOwned(examId, ownerId);
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con id " + teacherId));
        if (teacherId.equals(ownerId)) {
            throw new BusinessRuleException("No puedes compartir un examen contigo mismo");
        }
        if (!TEACHER_ROLES.contains(teacher.getRole())) {
            throw new BusinessRuleException("Solo se puede compartir con profesores");
        }
        if (shareRepository.findByExamIdAndTeacherId(examId, teacherId).isPresent()) {
            throw new BusinessRuleException("Ya has compartido este examen con ese profesor");
        }
        ExamShare share = shareRepository.save(new ExamShare(examId, teacherId, ownerId, LocalDateTime.now(clock)));
        return toResponse(share, teacher);
    }

    @Transactional(readOnly = true)
    public List<ExamShareResponse> list(Long examId, Long ownerId) {
        examService.getOwned(examId, ownerId);
        List<ExamShare> shares = shareRepository.findAllByExamIdOrderByCreatedAt(examId);
        Map<Long, User> teachers = usersById(shares.stream().map(ExamShare::getTeacherId).collect(Collectors.toSet()));
        return shares.stream().map(s -> toResponse(s, teachers.get(s.getTeacherId()))).toList();
    }

    /** Profesores y administradores activos que coinciden, salvo el dueño y los ya invitados. */
    @Transactional(readOnly = true)
    public List<ShareCandidateResponse> candidates(Long examId, Long ownerId, String query) {
        examService.getOwned(examId, ownerId);
        Set<Long> excluded = shareRepository.findAllByExamIdOrderByCreatedAt(examId).stream()
                .map(ExamShare::getTeacherId).collect(Collectors.toCollection(HashSet::new));
        excluded.add(ownerId);
        String text = query == null ? "" : query.trim();
        return userRepository.searchActiveByRoles(text, TEACHER_ROLES, PageRequest.of(0, MAX_CANDIDATES + excluded.size()))
                .stream()
                .filter(u -> !excluded.contains(u.getId()))
                .limit(MAX_CANDIDATES)
                .map(u -> new ShareCandidateResponse(u.getId(), u.getUsername(), u.getDisplayName()))
                .toList();
    }

    /** Cancela la invitación o retira el acceso. */
    public void revoke(Long examId, Long ownerId, Long teacherId) {
        examService.getOwned(examId, ownerId);
        shareRepository.delete(shareRepository.findByExamIdAndTeacherId(examId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Este examen no está compartido con ese profesor")));
    }

    // ───── El invitado ─────

    @Transactional(readOnly = true)
    public List<InvitationResponse> invitations(Long teacherId) {
        List<ExamShare> pending = shareRepository.findAllByTeacherIdAndStatusOrderByCreatedAtDesc(teacherId,
                ShareStatus.PENDING);
        Map<Long, Exam> exams = examRepository.findAllById(pending.stream().map(ExamShare::getExamId).toList()).stream()
                .collect(Collectors.toMap(Exam::getId, Function.identity()));
        Map<Long, User> owners = usersById(pending.stream().map(ExamShare::getInvitedBy).collect(Collectors.toSet()));
        return pending.stream()
                .map(s -> new InvitationResponse(s.getId(), s.getExamId(), exams.get(s.getExamId()).getTitle(),
                        owners.get(s.getInvitedBy()).getDisplayName(), s.getCreatedAt()))
                .toList();
    }

    /** Acepta la invitación: desde ahora el examen sale en su lista. */
    public ExamResponse accept(Long invitationId, Long teacherId) {
        ExamShare share = pendingInvitation(invitationId, teacherId);
        share.accept(LocalDateTime.now(clock));
        return examService.get(share.getExamId(), teacherId);
    }

    public void reject(Long invitationId, Long teacherId) {
        shareRepository.delete(pendingInvitation(invitationId, teacherId));
    }

    /** «Dejar de ver»: el invitado renuncia a un examen que aceptó. */
    public void leave(Long examId, Long teacherId) {
        ExamShare share = shareRepository.findByExamIdAndTeacherId(examId, teacherId)
                .filter(s -> !s.isPending())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un examen con id " + examId));
        shareRepository.delete(share);
    }

    /**
     * Preguntas de los exámenes que me han compartido y he aceptado (RF-57): las del banco del dueño, al
     * día y una vez cada una, de la más reciente a la más antigua. Solo se consultan. Las que el dueño
     * eliminó de su banco no salen, aunque sigan en su examen publicado.
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> sharedQuestions(Long teacherId) {
        List<Exam> exams = examRepository.findAllById(examService.sharedExamIds(teacherId));
        Set<Long> questionIds = exams.stream().flatMap(e -> e.getQuestionIds().stream()).collect(Collectors.toSet());
        Map<Long, User> owners = usersById(exams.stream().map(Exam::getOwnerId).collect(Collectors.toSet()));
        return questionRepository.findAllById(questionIds).stream()
                .filter(q -> !q.isDeleted())
                .sorted(Comparator.comparing(Question::getCreatedAt).reversed())
                .map(q -> QuestionResponse.shared(q, owners.get(q.getOwnerId()).getDisplayName()))
                .toList();
    }

    // ───── Auxiliares ─────

    private ExamShare pendingInvitation(Long invitationId, Long teacherId) {
        return shareRepository.findById(invitationId)
                .filter(s -> s.getTeacherId().equals(teacherId) && s.isPending())
                .orElseThrow(() -> new ResourceNotFoundException("No existe esa invitación"));
    }

    private Map<Long, User> usersById(Set<Long> ids) {
        return userRepository.findAllById(ids).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private static ExamShareResponse toResponse(ExamShare share, User teacher) {
        return new ExamShareResponse(teacher.getId(), teacher.getUsername(), teacher.getDisplayName(),
                share.getStatus(), share.getCreatedAt());
    }
}
