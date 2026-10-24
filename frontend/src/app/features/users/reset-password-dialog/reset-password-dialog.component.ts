import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { UserService } from '../../../core/services/user.service';
import { UserResponse } from '../../../core/models/auth.models';

/** Igual que en el servidor. */
const MIN_PASSWORD_LENGTH = 6;

/**
 * El administrador escribe una contraseña temporal para otro usuario (RF-35).
 * Al cerrarse devuelve el usuario actualizado; sin guardar, nada.
 */
@Component({
  selector: 'app-reset-password-dialog',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './reset-password-dialog.component.html',
  styleUrl: './reset-password-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResetPasswordDialogComponent {
  private userSvc = inject(UserService);
  private dialogRef = inject(MatDialogRef<ResetPasswordDialogComponent, UserResponse>);
  readonly user = inject<UserResponse>(MAT_DIALOG_DATA);

  readonly minLength = MIN_PASSWORD_LENGTH;
  readonly form = new FormGroup({
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(MIN_PASSWORD_LENGTH)]
    })
  });
  readonly password = this.form.controls.password;

  saving = signal(false);
  error = signal<string | null>(null);

  save(): void {
    if (this.password.invalid || this.saving()) {
      this.password.markAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    this.userSvc.resetPassword(this.user.id, this.password.value).subscribe({
      next: updated => this.dialogRef.close(updated),
      error: err => {
        this.saving.set(false);
        this.error.set(err?.error?.message ?? 'No se pudo restablecer la contraseña');
      }
    });
  }
}
