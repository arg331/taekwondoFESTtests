import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, of, switchMap, timer } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClubService } from '../../../../core/services/club.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ConfirmService } from '../../../../core/services/confirm.service';
import { ClubCandidateResponse, ClubMemberResponse, ClubResponse } from '../../../../core/models/club.models';
import { ROLE_LABEL } from '../../../../core/models/auth.models';

/**
 * Un club: sus miembros, añadir y quitar, renombrarlo y, el administrador, borrarlo (RF-40, RF-42).
 * Los miembros solo los ve quien gestiona el club.
 */
/** Pausa tras la última tecla antes de pedir la lista, para no lanzar una petición por letra. */
const SEARCH_PAUSE_MS = 250;
/** Los que devuelve el servidor como mucho (ClubService.MAX_CANDIDATES). */
export const MAX_CANDIDATES = 20;

@Component({
  selector: 'app-club-detail',
  imports: [MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, RouterLink],
  templateUrl: './club-detail.component.html',
  styleUrl: './club-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ClubDetailComponent {
  private clubSvc = inject(ClubService);
  private auth = inject(AuthService);
  private confirmSvc = inject(ConfirmService);

  club = input.required<ClubResponse>();
  /** Administrador o profesor de este club. */
  canManage = input(false);
  isAdmin = input(false);
  renamed = output<ClubResponse>();
  deleted = output<number>();

  readonly roleLabel = ROLE_LABEL;
  readonly maxCandidates = MAX_CANDIDATES;
  private readonly clubId = computed(() => this.club().id);

  readonly members = rxResource({
    params: () => (this.canManage() ? this.clubId() : undefined),
    stream: ({ params: id }) => (id === undefined ? of([] as ClubMemberResponse[]) : this.clubSvc.members(id))
  });

  // Todo lo que se está escribiendo se reinicia al cambiar de club
  renaming = linkedSignal({ source: this.clubId, computation: () => false });
  nameDraft = linkedSignal({ source: this.clubId, computation: () => '' });
  query = linkedSignal({ source: this.clubId, computation: () => '' });

  /** A quién se puede añadir: sin escribir, todos; lo que se escribe acota la lista. */
  readonly candidates = rxResource({
    params: () => (this.canManage() ? { id: this.clubId(), q: this.query().trim() } : undefined),
    stream: ({ params }) => {
      if (params === undefined) return of([] as ClubCandidateResponse[]);
      const request = this.clubSvc.candidates(params.id, params.q);
      return params.q ? timer(SEARCH_PAUSE_MS).pipe(switchMap(() => request)) : request;
    }
  });
  error = linkedSignal<number, string | null>({ source: this.clubId, computation: () => null });
  busy = signal(false);

  startRename(): void {
    this.nameDraft.set(this.club().name);
    this.renaming.set(true);
  }

  saveName(): void {
    const name = this.nameDraft().trim();
    if (!name || this.busy()) return;
    this.run(this.clubSvc.rename(this.clubId(), name), club => {
      this.renaming.set(false);
      this.renamed.emit(club);
    });
  }

  add(candidate: ClubCandidateResponse): void {
    this.run(this.clubSvc.addMember(this.clubId(), candidate.id), member => {
      this.members.update(list => [...(list ?? []), member]);
      this.candidates.update(list => list?.filter(c => c.id !== candidate.id) ?? []);
    });
  }

  remove(member: ClubMemberResponse): void {
    const itsMe = member.id === this.auth.currentUser()?.id;
    const question = itsMe
      ? { title: 'Salir del club', message: `¿Salir de «${this.club().name}»? Dejarás de poder gestionarlo.`,
          confirmLabel: 'Salir' }
      : { title: 'Quitar miembro', message: `¿Quitar a ${member.displayName} de «${this.club().name}»?`,
          confirmLabel: 'Quitar' };
    this.confirmSvc.ask(question).subscribe(ok => {
      if (!ok) return;
      this.run(this.clubSvc.removeMember(this.clubId(), member.id), () => {
        this.members.update(list => list?.filter(m => m.id !== member.id) ?? []);
        const user = this.auth.currentUser();
        if (itsMe && user) this.auth.updateCurrentUser({ ...user, club: null });
      });
    });
  }

  deleteClub(): void {
    this.confirmSvc.ask({
      title: 'Borrar club', message: `¿Borrar «${this.club().name}»? Sus miembros se quedarán sin club.`,
      confirmLabel: 'Borrar'
    }).subscribe(ok => {
      if (!ok) return;
      const id = this.clubId();
      this.run(this.clubSvc.delete(id), () => this.deleted.emit(id));
    });
  }

  private run<T>(action$: Observable<T>, onSuccess: (value: T) => void): void {
    this.busy.set(true);
    this.error.set(null);
    action$.subscribe({
      next: value => {
        this.busy.set(false);
        onSuccess(value);
      },
      error: err => {
        this.busy.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo completar la acción');
      }
    });
  }
}
