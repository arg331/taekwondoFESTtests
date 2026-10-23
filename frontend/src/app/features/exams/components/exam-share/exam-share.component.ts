import { ChangeDetectionStrategy, Component, computed, inject, input, linkedSignal, signal } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ShareService } from '../../../../core/services/share.service';
import { ConfirmService } from '../../../../core/services/confirm.service';
import { ExamShareResponse, ShareCandidateResponse, ShareStatus } from '../../../../core/models/share.models';

const STATUS_LABEL: Record<ShareStatus, string> = { ACCEPTED: 'Aceptada', PENDING: 'Pendiente' };

/**
 * El dueño comparte su examen con otros profesores (RF-51): ve con quién y si han aceptado, busca a
 * quién invitar y retira el acceso. Los invitados consultan el examen y sus resultados, sin cambiarlo.
 */
@Component({
  selector: 'app-exam-share',
  imports: [MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './exam-share.component.html',
  styleUrl: './exam-share.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamShareComponent {
  private shareSvc = inject(ShareService);
  private confirmSvc = inject(ConfirmService);

  examId = input.required<number>();

  readonly statusLabel = STATUS_LABEL;

  readonly shares = rxResource({
    params: () => this.examId(),
    stream: ({ params: examId }) => this.shareSvc.list(examId)
  });

  // Lo que se está buscando se reinicia al cambiar de examen
  query = linkedSignal({ source: this.examId, computation: () => '' });
  candidates = linkedSignal<number, ShareCandidateResponse[] | null>({ source: this.examId, computation: () => null });
  error = linkedSignal<number, string | null>({ source: this.examId, computation: () => null });
  busy = signal(false);

  noCandidates = computed(() => this.candidates()?.length === 0);

  search(): void {
    if (this.busy()) return;
    this.run(this.shareSvc.candidates(this.examId(), this.query().trim()), found => this.candidates.set(found));
  }

  invite(candidate: ShareCandidateResponse): void {
    this.run(this.shareSvc.invite(this.examId(), candidate.id), share => {
      this.shares.update(list => [...(list ?? []), share]);
      this.candidates.update(list => list?.filter(c => c.id !== candidate.id) ?? null);
    });
  }

  revoke(share: ExamShareResponse): void {
    const question = share.status === 'PENDING'
      ? { title: 'Cancelar invitación', message: `¿Cancelar la invitación a ${share.displayName}?`,
          confirmLabel: 'Cancelar invitación', cancelLabel: 'Volver' }
      : { title: 'Quitar acceso', message: `¿Quitar a ${share.displayName}? Dejará de ver este examen y sus resultados.`,
          confirmLabel: 'Quitar' };
    this.confirmSvc.ask(question).subscribe(ok => {
      if (!ok) return;
      this.run(this.shareSvc.revoke(this.examId(), share.teacherId),
        () => this.shares.update(list => list?.filter(s => s.teacherId !== share.teacherId) ?? []));
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
