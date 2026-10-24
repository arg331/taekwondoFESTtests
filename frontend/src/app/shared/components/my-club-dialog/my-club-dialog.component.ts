import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ClubService } from '../../../core/services/club.service';
import { AuthService } from '../../../core/services/auth.service';
import { ClubResponse } from '../../../core/models/club.models';

/** Valor de «Sin club» en el desplegable: con null, el desplegable se mostraría vacío. */
const NO_CLUB = 0;

/** El usuario elige su club, lo cambia o lo deja (RF-41). */
@Component({
  selector: 'app-my-club-dialog',
  imports: [MatDialogModule, MatButtonModule, MatFormFieldModule, MatSelectModule],
  templateUrl: './my-club-dialog.component.html',
  styleUrl: './my-club-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MyClubDialogComponent {
  private clubSvc = inject(ClubService);
  private auth = inject(AuthService);
  private dialogRef = inject(MatDialogRef<MyClubDialogComponent>);

  readonly noClub = NO_CLUB;
  readonly clubs = toSignal(this.clubSvc.list(), { initialValue: [] as ClubResponse[] });

  selected = signal<number>(this.auth.currentUser()?.club?.id ?? NO_CLUB);
  saving = signal(false);
  error = signal<string | null>(null);

  save(): void {
    if (this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    const clubId = this.selected() === NO_CLUB ? null : this.selected();
    this.clubSvc.chooseMine(clubId).subscribe({
      next: user => {
        this.auth.updateCurrentUser(user);
        this.dialogRef.close(user);
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo cambiar el club');
      }
    });
  }
}
