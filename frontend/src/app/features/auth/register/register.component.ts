import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

function passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
  const password = control.get('plainPassword');
  const confirm  = control.get('confirmPassword');
  if (!password || !confirm) return null;

  if (password.value !== confirm.value) {
    confirm.setErrors({ passwordMismatch: true });
    return { passwordMismatch: true };
  } else {
    const errors = { ...confirm.errors };
    delete errors['passwordMismatch'];
    confirm.setErrors(Object.keys(errors).length ? errors : null);
    return null;
  }
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
    MatProgressSpinnerModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private fb     = inject(FormBuilder);
  private auth   = inject(AuthService);
  private router = inject(Router);

  loading    = signal(false);
  error      = signal<string | null>(null);
  showPass   = signal(false);
  registered = signal(false);

  form = this.fb.group({
    displayName:     ['', [Validators.required, Validators.minLength(2)]],
    username:        ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
    email:           ['', [Validators.required, Validators.email]],
    plainPassword:   ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordMatchValidator });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    this.loading.set(true);
    this.error.set(null);
    const { confirmPassword, ...request } = this.form.value as any;
    this.auth.register(request).subscribe({
      next: () => {
        this.registered.set(true);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(
          err.status === 409
            ? 'El usuario o email ya existe'
            : 'Error al registrarse. Inténtalo de nuevo.'
        );
        this.loading.set(false);
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/auth/login']);
  }
}
