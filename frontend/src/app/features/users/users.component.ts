import { ChangeDetectionStrategy, Component, inject, signal, OnInit, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelect, MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { ROLE_LABEL, USER_ROLES, UserResponse, UserRole } from '../../core/models/auth.models';

/** Cuánto se ve el aviso de un cambio de rol rechazado. */
const ERROR_DURATION_MS = 5000;

/** Gestión de usuarios del administrador: lista y cambio de rol (RF-36). */
@Component({
  selector: 'app-users',
  imports: [
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComponent implements OnInit {
  private userSvc = inject(UserService);
  private snackBar = inject(MatSnackBar);
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
      next: updated => this.users.update(list => list.map(u => u.id === updated.id ? updated : u)),
      error: err => {
        select.value = user.role;
        this.snackBar.open(err?.error?.message ?? 'No se pudo cambiar el rol', undefined, { duration: ERROR_DURATION_MS });
      }
    });
  }

  isCurrentUser(user: UserResponse): boolean {
    return user.id === this.auth.currentUser()?.id;
  }
}
