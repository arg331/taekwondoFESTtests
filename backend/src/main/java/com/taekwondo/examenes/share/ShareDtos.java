package com.taekwondo.examenes.share;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Peticiones y respuestas de los exámenes compartidos (RF-51). */
public final class ShareDtos {

    private ShareDtos() {
    }

    /** Invitar a un profesor a un examen propio. */
    public record InviteRequest(@NotNull Long teacherId) {}

    /** Con quién comparte el dueño su examen, y si ya ha aceptado. */
    public record ExamShareResponse(Long teacherId, String username, String displayName, ShareStatus status,
                                    LocalDateTime createdAt) {}

    /** Un profesor o administrador al que se puede invitar. */
    public record ShareCandidateResponse(Long id, String username, String displayName) {}

    /** Una invitación pendiente, para el panel del invitado: «Ana quiere compartir contigo el examen …». */
    public record InvitationResponse(Long id, Long examId, String examTitle, String invitedBy, LocalDateTime createdAt) {}
}
