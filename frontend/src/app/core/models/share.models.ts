/** Exámenes compartidos entre profesores (RF-51). */

export type ShareStatus = 'PENDING' | 'ACCEPTED';

/** Con quién comparte el dueño su examen. */
export interface ExamShareResponse {
  teacherId: number;
  username: string;
  displayName: string;
  status: ShareStatus;
  createdAt: string;
}

/** Profesor o administrador al que se puede invitar. */
export interface ShareCandidateResponse {
  id: number;
  username: string;
  displayName: string;
}

/** Invitación pendiente, para el panel del invitado. */
export interface InvitationResponse {
  id: number;
  examId: number;
  examTitle: string;
  invitedBy: string;
  createdAt: string;
}
