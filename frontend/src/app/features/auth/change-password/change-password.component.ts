import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { passwordMatchValidator } from '../../../shared/utils/password-match';
import { safeReturnUrl } from '../../../shared/utils/return-url';

/** Igual que en el registro y en el servidor. */
const MIN_PASSWORD_LENGTH = 6;

/**
 * Cambiar la propia contraseña (RF-06). Con una contraseña temporal (RF-33) es obligatorio:
 * no se puede cancelar, solo cambiarla o cerrar sesión.
 */
@Component({
  selector: 'app-change-password',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './change-password.component.html',
  styleUrl: './change-password.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ChangePasswordComponent {
  private fb = inject(FormBuilder);
  protected readonly auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly minLength = MIN_PASSWORD_LENGTH;
  /** Se fija al entrar: al guardar, la sesión nueva ya no la tiene y el texto no debe cambiar. */
  readonly temporary = this.auth.mustChangePassword();

  saving = signal(false);
  error = signal<string | null>(null);
  showPass = signal(false);

  form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(MIN_PASSWORD_LENGTH)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordMatchValidator('newPassword', 'confirmPassword') });

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    const { currentPassword, newPassword } = this.form.getRawValue();

    this.auth.changePassword({ currentPassword, newPassword }).subscribe({
      next: () => this.router.navigateByUrl(safeReturnUrl(this.route)),
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo cambiar la contraseña');
      }
    });
  }

  cancel(): void {
    this.router.navigateByUrl(safeReturnUrl(this.route));
  }

  logout(): void {
    this.auth.logout();
  }
}
