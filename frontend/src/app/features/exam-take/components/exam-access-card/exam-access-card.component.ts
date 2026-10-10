import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExamResponse } from '../../../../core/models/exam.models';
import { UserResponse } from '../../../../core/models/auth.models';

export interface StudentInfo {
  studentName: string;
  studentClub: string | null;
  studentEmail: string | null;
}

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
    MatProgressSpinnerModule
  ],
  templateUrl: './exam-access-card.component.html',
  styleUrl: './exam-access-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExamAccessCardComponent {
  private fb = inject(FormBuilder);

  exam = input.required<ExamResponse>();
  user = input<UserResponse | null>(null);
  starting = input(false);
  start = output<StudentInfo>();

  needsLogin = computed(() => this.exam().accessMode === 'REGISTERED_ONLY' && !this.user());
  loginQueryParams = computed(() => ({ returnUrl: `/exam/${this.exam().code}` }));

  form = this.fb.nonNullable.group({
    studentName:  ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    studentClub:  ['', Validators.maxLength(100)],
    studentEmail: ['', Validators.email]
  });

  constructor() {
    // Con sesión iniciada, rellena nombre y email con los de la cuenta
    effect(() => {
      const user = this.user();
      if (user && !this.form.dirty) {
        this.form.patchValue({ studentName: user.displayName, studentEmail: user.email });
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
