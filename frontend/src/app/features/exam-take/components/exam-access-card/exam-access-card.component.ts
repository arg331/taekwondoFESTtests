import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { toSignal } from '@angular/core/rxjs-interop';
import { ExamResponse, StartAttemptRequest, VISIBILITY_ICON } from '../../../../core/models/exam.models';
import { ClubService } from '../../../../core/services/club.service';
import { ClubResponse } from '../../../../core/models/club.models';
import { UserResponse } from '../../../../core/models/auth.models';
import { APP_NAME } from '../../../../core/app-brand';

/** Con lo que se identifica el alumno al empezar (DE-04). */
export type StudentInfo = StartAttemptRequest;

/**
 * Portada del examen: datos del alumno antes de empezar. Si el examen exige
 * cuenta y no hay sesión, ofrece iniciar sesión y volver aquí.
 */
@Component({
  selector: 'app-exam-access-card',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './exam-access-card.component.html',
  styleUrl: './exam-access-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamAccessCardComponent {
  readonly appName = APP_NAME;
  private fb = inject(FormBuilder);
  private clubSvc = inject(ClubService);

  /** Clubes que existen, para elegir el propio (vale también sin cuenta). */
  readonly clubs = toSignal(this.clubSvc.list(), { initialValue: [] as ClubResponse[] });

  exam = input.required<ExamResponse>();
  user = input<UserResponse | null>(null);
  starting = input(false);
  start = output<StudentInfo>();

  /** Público = de prueba; privado = oficial (RF-49). */
  isPractice = computed(() => this.exam().visibility === 'PUBLIC');
  kindIcon = computed(() => VISIBILITY_ICON[this.exam().visibility]);
  needsLogin = computed(() => this.exam().accessMode === 'REGISTERED_ONLY' && !this.user());
  loginQueryParams = computed(() => ({ returnUrl: `/exam/${this.exam().code}` }));

  form = this.fb.nonNullable.group({
    studentName:  ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    /** Nombre del club elegido; '' = sin club. */
    studentClub:  [''],
    studentEmail: ['', Validators.email]
  });

  constructor() {
    // Con sesión iniciada, rellena nombre, club y email con los de la cuenta (club y email pueden faltar)
    effect(() => {
      const user = this.user();
      if (user && !this.form.dirty) {
        this.form.patchValue({
          studentName: user.displayName,
          studentClub: user.club?.name ?? '',
          studentEmail: user.email ?? ''
        });
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.starting()) return;
    const v = this.form.getRawValue();
    this.start.emit({
      studentName: v.studentName.trim(),
      studentClub: v.studentClub.trim() || null,
      studentEmail: v.studentEmail.trim() || null
    });
  }
}
