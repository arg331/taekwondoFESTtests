import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Recuperar el usuario o la contraseña sin sesión (RF-34): quien tiene email recibe su usuario y un
 * enlace para poner una contraseña nueva. La respuesta es la misma exista o no la cuenta.
 */
@Component({
  selector: 'app-recover-account',
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
  templateUrl: './recover-account.component.html',
  styleUrl: '../reset-password/auth-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecoverAccountComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);

  sending = signal(false);
  /** Lo que responde el servidor al enviar: el formulario deja de verse. */
  sent = signal<string | null>(null);
  error = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]]
  });

  submit(): void {
    if (this.form.invalid || this.sending()) return;
    this.sending.set(true);
    this.error.set(null);
    this.auth.requestRecovery(this.form.getRawValue().email.trim()).subscribe({
      next: response => {
        this.sending.set(false);
        this.sent.set(response.message);
      },
      error: err => {
        this.sending.set(false);
        this.error.set(err?.error?.message ?? 'No se ha podido enviar. Inténtalo de nuevo.');
      }
    });
  }
}
