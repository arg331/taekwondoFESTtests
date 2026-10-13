package com.taekwondo.examenes.club;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Clubes. El listado es público (para el desplegable del registro); crear y gestionar,
 * profesor o administrador (ver SecurityConfig). Quién puede gestionar cada club lo decide ClubService.
 */
@RestController
@RequestMapping("/api/clubs")
public class ClubController {

    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @GetMapping
    public List<ClubResponse> list() {
        return clubService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClubResponse create(@Valid @RequestBody ClubRequest request, @AuthenticationPrincipal Long userId) {
        return clubService.create(request.name(), userId);
    }

    @PatchMapping("/{id}")
    public ClubResponse rename(@PathVariable Long id, @Valid @RequestBody ClubRequest request,
                               @AuthenticationPrincipal Long userId) {
        return clubService.rename(id, request.name(), userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        clubService.delete(id, userId);
    }

    @GetMapping("/{id}/members")
    public List<ClubMemberResponse> members(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return clubService.members(id, userId);
    }

    @PutMapping("/{id}/members/{memberId}")
    public ClubMemberResponse addMember(@PathVariable Long id, @PathVariable Long memberId,
                                        @AuthenticationPrincipal Long userId) {
        return clubService.addMember(id, memberId, userId);
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long memberId,
                             @AuthenticationPrincipal Long userId) {
        clubService.removeMember(id, memberId, userId);
    }
}
