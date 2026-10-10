import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { TeacherResultsComponent } from './components/teacher-results/teacher-results.component';
import { MyResultsComponent } from './components/my-results/my-results.component';

@Component({
  selector: 'app-results',
  imports: [TeacherResultsComponent, MyResultsComponent],
  template: `
    <div class="results-page">
      <h1>{{ auth.isAdmin() ? 'Resultados' : 'Mis resultados' }}</h1>
      @if (auth.isAdmin()) {
        <app-teacher-results />
      } @else {
        <app-my-results />
      }
    </div>
  `,
  styles: `
    .results-page { display: flex; flex-direction: column; gap: 20px; }
    h1 { margin: 0; font-size: 1.6rem; font-weight: 600; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResultsComponent {
  auth = inject(AuthService);
}
