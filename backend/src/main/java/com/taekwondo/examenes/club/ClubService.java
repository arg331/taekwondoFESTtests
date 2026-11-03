package com.taekwondo.examenes.club;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.common.ForbiddenException;
import com.taekwondo.examenes.common.ResourceNotFoundException;
import com.taekwondo.examenes.result.ResultReportService;
import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRepository;
import com.taekwondo.examenes.user.UserResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Clubes (RF-38 a RF-42). Los crean el administrador y los profesores; los gestionan el
 * administrador y cualquier profesor del club; cada usuario pertenece como mucho a uno
 * y puede elegir el suyo.
 */
@Service
@Transactional
public class ClubService {

    /** Cuántos candidatos devuelve como mucho una búsqueda. */
    private static final int MAX_CANDIDATES = 20;

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ResultReportService resultReportService;

    public ClubService(ClubRepository clubRepository, UserRepository userRepository,
                       ResultReportService resultReportService) {
        this.clubRepository = clubRepository;
        this.userRepository = userRepository;
        this.resultReportService = resultReportService;
    }

    @Transactional(readOnly = true)
    public List<ClubResponse> list() {
        return clubRepository.findAllSortedByName().stream().map(ClubResponse::from).toList();
    }

    /** El profesor que lo crea entra en él; si ya está en otro, no puede crearlo. El administrador no entra. */
    public ClubResponse create(String name, Long requesterId) {
        User requester = getUser(requesterId);
        if (requester.isTeacher() && requester.getClub() != null) {
            throw new BusinessRuleException("Ya perteneces al club «" + requester.getClub().getName()
                    + "»: no puedes crear otro");
        }
        String trimmed = name.trim();
        if (clubRepository.existsByNameIgnoreCase(trimmed)) {
            throw duplicateName(trimmed);
        }
        Club club = clubRepository.save(new Club(trimmed));
        if (requester.isTeacher()) {
            requester.joinClub(club);
        }
        return ClubResponse.from(club);
    }

    public ClubResponse rename(Long clubId, String name, Long requesterId) {
        Club club = getClub(clubId);
        requireManager(club, getUser(requesterId));
        String trimmed = name.trim();
        if (clubRepository.existsByNameIgnoreCaseAndIdNot(trimmed, clubId)) {
            throw duplicateName(trimmed);
        }
        club.rename(trimmed);
        return ClubResponse.from(club);
    }

    /** Solo el administrador; los miembros se quedan sin club (RF-42). */
    public void delete(Long clubId, Long requesterId) {
        Club club = getClub(clubId);
        if (!getUser(requesterId).isAdmin()) {
            throw new ForbiddenException("Solo el administrador puede borrar clubes");
        }
        userRepository.findAllByClubId(clubId).forEach(User::leaveClub);
        clubRepository.delete(club);
    }

    @Transactional(readOnly = true)
    public List<ClubMemberResponse> members(Long clubId, Long requesterId) {
        Club club = getClub(clubId);
        requireManager(club, getUser(requesterId));
        List<User> members = userRepository.findAllByClubId(clubId);
        Set<Long> visible = resultReportService.withVisibleResults(members.stream().map(User::getId).toList(),
                requesterId);
        return members.stream().map(m -> ClubMemberResponse.from(m, visible.contains(m.getId()))).toList();
    }

    /**
     * A quién se puede añadir: el profesor busca entre quienes no tienen club (RF-40);
     * el administrador también entre los de otros clubes, para cambiarlos (RF-42).
     */
    @Transactional(readOnly = true)
    public List<ClubCandidateResponse> candidates(Long clubId, String text, Long requesterId) {
        Club club = getClub(clubId);
        User requester = getUser(requesterId);
        requireManager(club, requester);
        String query = text == null ? "" : text.trim();
        return userRepository.searchClubCandidates(query, clubId, !requester.isAdmin(), PageRequest.of(0, MAX_CANDIDATES))
                .stream().map(ClubCandidateResponse::from).toList();
    }

    /**
     * Un profesor del club solo añade usuarios sin club (RF-40); el administrador puede
     * cambiar de club a cualquiera (RF-42).
     */
    public ClubMemberResponse addMember(Long clubId, Long userId, Long requesterId) {
        Club club = getClub(clubId);
        User requester = getUser(requesterId);
        requireManager(club, requester);
        User member = getUser(userId);
        if (member.belongsTo(club)) {
            throw new BusinessRuleException("Ese usuario ya es miembro del club");
        }
        if (member.getClub() != null && !requester.isAdmin()) {
            throw new BusinessRuleException("Ese usuario ya pertenece al club «" + member.getClub().getName()
                    + "»: solo el administrador puede cambiarlo de club");
        }
        member.joinClub(club);
        return ClubMemberResponse.from(member,
                resultReportService.withVisibleResults(List.of(member.getId()), requesterId).contains(member.getId()));
    }

    public void removeMember(Long clubId, Long userId, Long requesterId) {
        Club club = getClub(clubId);
        requireManager(club, getUser(requesterId));
        User member = getUser(userId);
        if (!member.belongsTo(club)) {
            throw new BusinessRuleException("Ese usuario no es miembro del club");
        }
        member.leaveClub();
    }

    /** Cada usuario elige su club, lo cambia o lo deja (clubId null) (RF-41). */
    public UserResponse chooseMyClub(Long clubId, Long userId) {
        User user = getUser(userId);
        if (clubId == null) {
            user.leaveClub();
        } else {
            user.joinClub(getClub(clubId));
        }
        return UserResponse.from(user);
    }

    private static void requireManager(Club club, User requester) {
        if (!requester.isAdmin() && !(requester.isTeacher() && requester.belongsTo(club))) {
            throw new ForbiddenException("Solo el administrador y los profesores del club pueden gestionarlo");
        }
    }

    private static BusinessRuleException duplicateName(String name) {
        return new BusinessRuleException("Ya existe un club llamado «" + name + "»");
    }

    private Club getClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un club con id " + clubId));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe un usuario con id " + userId));
    }
}
