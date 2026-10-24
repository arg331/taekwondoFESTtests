import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { ShareService } from '../../../../core/services/share.service';
import { InvitationResponse } from '../../../../core/models/share.models';

/**
 * Invitaciones a exámenes compartidos, en el panel del profesor (RF-51): «Ana quiere compartir contigo
 * el examen: …», con «Aceptar» y «Rechazar». Al aceptar, el examen pasa a su lista de exámenes.
 */
@Component({
  selector: 'app-share-invitations',
  imports: [RouterLink, MatButtonModule, MatCardModule, MatIconModule],
  templateUrl: './share-invitations.component.html',
  styleUrl: './share-invitations.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShareInvitationsComponent {
  private shareSvc = inject(ShareService);

  readonly invitations = rxResource({ stream: () => this.shareSvc.invitations() });

  /** La última aceptada, para llevar a ella. */
  accepted = signal<InvitationResponse | null>(null);
  error = signal<string | null>(null);
  busy = signal(false);

  accept(invitation: InvitationResponse): void {
    this.run(this.shareSvc.accept(invitation.id), () => {
      this.remove(invitation);
      this.accepted.set(invitation);
    });
  }

  reject(invitation: InvitationResponse): void {
    this.run(this.shareSvc.reject(invitation.id), () => this.remove(invitation));
  }

  private remove(invitation: InvitationResponse): void {
    this.invitations.update(list => list?.filter(i => i.id !== invitation.id) ?? []);
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
