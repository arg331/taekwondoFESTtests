import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClubService } from '../../core/services/club.service';
import { AuthService } from '../../core/services/auth.service';
import { ClubResponse } from '../../core/models/club.models';
import { ClubDetailComponent } from './components/club-detail/club-detail.component';

/**
 * Clubes para profesores y administrador (RF-38, RF-40, RF-42): la lista a la izquierda
 * y el club elegido a la derecha. El profesor abre el suyo al entrar.
 */
@Component({
  selector: 'app-clubs',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ClubDetailComponent
  ],
  templateUrl: './clubs.component.html',
  styleUrl: './clubs.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ClubsComponent implements OnInit {
  private clubSvc = inject(ClubService);
  private auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly isAdmin = this.auth.isAdmin;
  readonly myClubId = computed(() => this.auth.currentUser()?.club?.id ?? null);
  /** El administrador siempre puede crear; el profesor, solo si aún no tiene club (RF-38). */
  readonly canCreate = computed(() => this.isAdmin() || this.myClubId() === null);

  loading = signal(true);
  clubs = signal<ClubResponse[]>([]);
  selectedId = signal<number | null>(null);
  selected = computed(() => this.clubs().find(c => c.id === this.selectedId()) ?? null);
  canManageSelected = computed(() => this.isAdmin() || (this.selectedId() !== null && this.selectedId() === this.myClubId()));

  newName = signal('');
  creating = signal(false);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.clubSvc.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: clubs => {
        this.clubs.set(clubs);
        this.loading.set(false);
        if (!this.isAdmin()) this.selectedId.set(this.myClubId());
      },
      error: () => this.loading.set(false)
    });
  }

  create(): void {
    const name = this.newName().trim();
    if (!name || this.creating()) return;
    this.creating.set(true);
    this.error.set(null);
    this.clubSvc.create(name).subscribe({
      next: club => {
        this.creating.set(false);
        this.newName.set('');
        this.clubs.update(list => sortByName([...list, club]));
        this.selectedId.set(club.id);
        // El profesor que crea un club entra en él (RF-38)
        const user = this.auth.currentUser();
        if (user && !this.isAdmin()) this.auth.updateCurrentUser({ ...user, club });
      },
      error: err => {
        this.creating.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo crear el club');
      }
    });
  }

  onRenamed(club: ClubResponse): void {
    this.clubs.update(list => sortByName(list.map(c => (c.id === club.id ? club : c))));
    const user = this.auth.currentUser();
    if (user?.club?.id === club.id) this.auth.updateCurrentUser({ ...user, club });
  }

  onDeleted(id: number): void {
    this.clubs.update(list => list.filter(c => c.id !== id));
    this.selectedId.set(null);
  }
}

function sortByName(clubs: ClubResponse[]): ClubResponse[] {
  return [...clubs].sort((a, b) => a.name.localeCompare(b.name, 'es'));
}
