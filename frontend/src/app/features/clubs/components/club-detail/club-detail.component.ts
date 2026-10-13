import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, output, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable, of } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClubService } from '../../../../core/services/club.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ClubCandidateResponse, ClubMemberResponse, ClubResponse } from '../../../../core/models/club.models';
import { ROLE_LABEL } from '../../../../core/models/auth.models';

/**
 * Un club: sus miembros, añadir y quitar, renombrarlo y, el administrador, borrarlo (RF-40, RF-42).
 * Los miembros solo los ve quien gestiona el club.
 */
@Component({
  selector: 'app-club-detail',
  imports: [MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './club-detail.component.html',
  styleUrl: './club-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ClubDetailComponent {
  private clubSvc = inject(ClubService);
  private auth = inject(AuthService);

  club = input.required<ClubResponse>();
  /** Administrador o profesor de este club. */
  canManage = input(false);
  isAdmin = input(false);
  renamed = output<ClubResponse>();
  deleted = output<number>();

  readonly roleLabel = ROLE_LABEL;
  private readonly clubId = computed(() => this.club().id);

  readonly members = rxResource({
    params: () => (this.canManage() ? this.clubId() : undefined),
    stream: ({ params: id }) => (id === undefined ? of([] as ClubMemberResponse[]) : this.clubSvc.members(id))
  });

  // Todo lo que se está escribiendo se reinicia al cambiar de club
  renaming = linkedSignal({ source: this.clubId, computation: () => false });
  nameDraft = linkedSignal({ source: this.clubId, computation: () => '' });
  query = linkedSignal({ source: this.clubId, computation: () => '' });
  candidates = linkedSignal<number, ClubCandidateResponse[] | null>({ source: this.clubId, computation: () => null });
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

  search(): void {
    const q = this.query().trim();
    if (!q || this.busy()) return;
    this.run(this.clubSvc.candidates(this.clubId(), q), found => this.candidates.set(found));
  }

  add(candidate: ClubCandidateResponse): void {
    this.run(this.clubSvc.addMember(this.clubId(), candidate.id), member => {
      this.members.update(list => [...(list ?? []), member]);
      this.candidates.update(list => list?.filter(c => c.id !== candidate.id) ?? null);
    });
  }

  remove(member: ClubMemberResponse): void {
    const itsMe = member.id === this.auth.currentUser()?.id;
    const question = itsMe
      ? `¿Salir de «${this.club().name}»? Dejarás de poder gestionarlo.`
      : `¿Quitar a ${member.displayName} de «${this.club().name}»?`;
    if (!confirm(question)) return;
    this.run(this.clubSvc.removeMember(this.clubId(), member.id), () => {
      this.members.update(list => list?.filter(m => m.id !== member.id) ?? []);
      const user = this.auth.currentUser();
      if (itsMe && user) this.auth.updateCurrentUser({ ...user, club: null });
    });
  }

  deleteClub(): void {
    if (!confirm(`¿Borrar «${this.club().name}»? Sus miembros se quedarán sin club.`)) return;
    const id = this.clubId();
    this.run(this.clubSvc.delete(id), () => this.deleted.emit(id));
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
