import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../../core/services/auth.service';
import { ClubService } from '../../../core/services/club.service';
import { RegisterRequest } from '../../../core/models/auth.models';
import { ClubResponse } from '../../../core/models/club.models';
import { APP_NAME } from '../../../core/app-brand';
import { passwordMatchValidator } from '../../../shared/utils/password-match';

/** Valor de «Sin club» en el desplegable: con null, el desplegable se mostraría vacío. */
const NO_CLUB = 0;

function registerError(status: number | undefined): string {
  if (status === 409) return 'El usuario o email ya existe';
  if (status === 404) return 'Ese club ya no existe: elige otro o déjalo en «Sin club»';
  return 'Error al registrarse. Inténtalo de nuevo.';
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RegisterComponent {
  readonly appName = APP_NAME;
  private fb     = inject(FormBuilder);
  private auth   = inject(AuthService);
  private router = inject(Router);

  loading    = signal(false);
  error      = signal<string | null>(null);
  showPass   = signal(false);
  registered = signal(false);

  readonly noClub = NO_CLUB;
  /** Clubes que ya existen, para el desplegable (RF-41). */
  readonly clubs = toSignal(inject(ClubService).list(), { initialValue: [] as ClubResponse[] });

  form = this.fb.nonNullable.group({
    displayName:     ['', [Validators.required, Validators.minLength(2)]],
    username:        ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
    /** Opcional (RF-01): sin email, la cuenta se recupera a través del administrador. */
    email:           ['', [Validators.email]],
    clubId:          [NO_CLUB],
    plainPassword:   ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordMatchValidator('plainPassword', 'confirmPassword') });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    const { displayName, username, email, clubId, plainPassword } = this.form.getRawValue();
    const request: RegisterRequest = {
      displayName,
      username,
      email: email.trim() || null,
      plainPassword,
      clubId: clubId === NO_CLUB ? null : clubId
    };
    this.auth.register(request).subscribe({
      next: () => {
        this.registered.set(true);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(registerError(err?.status));
        this.loading.set(false);
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login']);
  }
}
