import { ChangeDetectionStrategy, Component, computed, inject, input, numberAttribute } from '@angular/core';
import { rxResource } from '@angular/core/rxjs-interop';
import { DatePipe, Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ResultService } from '../../../../core/services/result.service';
import { formatDuration } from '../../../../shared/utils/format';

/**
 * Resultados de un usuario (RF-55), desde los participantes de un examen, «Usuarios» o los miembros
 * de un club. El profesor ve sus entregas en sus propios exámenes; el administrador, todas.
 */
@Component({
  selector: 'app-user-results',
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './user-results.component.html',
  styleUrl: './user-results.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserResultsComponent {
  private resultSvc = inject(ResultService);
  private location = inject(Location);

  /** Parámetro de ruta :id. */
  id = input.required({ transform: numberAttribute });

  readonly formatDuration = formatDuration;

  readonly user = rxResource({
    params: () => this.id(),
    stream: ({ params: id }) => this.resultSvc.getUserResults(id)
  });

  notFound = computed(() => (this.user.error() as HttpErrorResponse | undefined)?.status === 404);

  back(): void {
    this.location.back();
  }
}
