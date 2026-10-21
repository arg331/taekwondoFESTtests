import { ChangeDetectionStrategy, Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelect, MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { ROLE_LABEL, USER_ROLES, UserResponse, UserRole } from '../../core/models/auth.models';
import { ResetPasswordDialogComponent } from './reset-password-dialog/reset-password-dialog.component';

/** Cuánto se ven los avisos: un cambio de rol rechazado o una contraseña restablecida. */
const MESSAGE_DURATION_MS = 5000;

/** Gestión de usuarios del administrador: lista, rol (RF-36), contraseña temporal (RF-35) y desactivar (RF-05). */
@Component({
  selector: 'app-users',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    RouterLink
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComponent implements OnInit {
  private userSvc = inject(UserService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  auth            = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  readonly roles = USER_ROLES;
  readonly roleLabel = ROLE_LABEL;

  loading = signal(true);
  users   = signal<UserResponse[]>([]);

  ngOnInit(): void {
    this.userSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: u => { this.users.set(u); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  /** Si el servidor lo rechaza, el desplegable vuelve al rol que tenía. */
  changeRole(user: UserResponse, role: UserRole, select: MatSelect): void {
    this.userSvc.changeRole(user.id, role).subscribe({
      next: updated => this.replace(updated),
      error: err => {
        select.value = user.role;
        this.snackBar.open(err?.error?.message ?? 'No se pudo cambiar el rol', undefined, { duration: MESSAGE_DURATION_MS });
      }
    });
  }

  /** La temporal la escribe el administrador; la fila queda marcada hasta que el usuario la cambie. */
  resetPassword(user: UserResponse): void {
    this.dialog.open<ResetPasswordDialogComponent, UserResponse, UserResponse>(ResetPasswordDialogComponent, {
      data: user, width: '440px', maxWidth: '95vw'
    }).afterClosed().subscribe(updated => {
      if (!updated) return;
      this.replace(updated);
      this.snackBar.open(`Contraseña temporal guardada: ${updated.displayName} tendrá que cambiarla al entrar`,
        undefined, { duration: MESSAGE_DURATION_MS });
    });
  }

  /** Desactivar cierra sus sesiones y le impide entrar; se puede deshacer reactivándola. */
  toggleActive(user: UserResponse): void {
    this.userSvc.setActive(user.id, !user.active).subscribe({
      next: updated => this.replace(updated),
      error: err => this.snackBar.open(err?.error?.message ?? 'No se pudo cambiar la cuenta', undefined,
        { duration: MESSAGE_DURATION_MS })
    });
  }

  isCurrentUser(user: UserResponse): boolean {
    return user.id === this.auth.currentUser()?.id;
  }

  private replace(updated: UserResponse): void {
    this.users.update(list => list.map(u => u.id === updated.id ? updated : u));
  }
}
