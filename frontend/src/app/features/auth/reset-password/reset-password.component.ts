import { ChangeDetectionStrategy, Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { passwordMatchValidator } from '../../../shared/utils/password-match';

/** Igual que en el registro y en el servidor. */
const MIN_PASSWORD_LENGTH = 6;

/**
 * La contraseña nueva desde el enlace del correo (RF-34). El código del enlace caduca a los 30
 * minutos y es de un solo uso; al cambiarla se cierran las sesiones abiertas.
 */
@Component({
  selector: 'app-reset-password',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './reset-password.component.html',
  styleUrl: './auth-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResetPasswordComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  /** Parámetro ?token= del enlace del correo. */
  token = input<string | undefined>();

  readonly minLength = MIN_PASSWORD_LENGTH;
  saving = signal(false);
  done = signal(false);
  error = signal<string | null>(null);
  showPass = signal(false);

  form = this.fb.nonNullable.group({
    newPassword: ['', [Validators.required, Validators.minLength(MIN_PASSWORD_LENGTH)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordMatchValidator('newPassword', 'confirmPassword') });

  submit(): void {
    const token = this.token();
    if (!token || this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.error.set(null);
    this.auth.resetPassword(token, this.form.getRawValue().newPassword).subscribe({
      next: () => {
        this.saving.set(false);
        this.done.set(true);
      },
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'No se ha podido cambiar la contraseña. Inténtalo de nuevo.');
      }
    });
  }
}
