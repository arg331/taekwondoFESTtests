import { ChangeDetectionStrategy, Component, inject, linkedSignal, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { rxResource, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, map } from 'rxjs';
import { MatPaginatorIntl, MatPaginatorModule } from '@angular/material/paginator';
import { PaginatorIntlEs } from '../../shared/paginator-intl-es';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelect, MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { ROLE_LABEL, USER_ROLES, UserResponse, UserRole } from '../../core/models/auth.models';
import { ResetPasswordDialogComponent } from './reset-password-dialog/reset-password-dialog.component';

/** Cuánto se ven los avisos: un cambio de rol rechazado o una contraseña restablecida. */
const MESSAGE_DURATION_MS = 5000;
const PAGE_SIZE = 20;
/** Se busca cuando se deja de escribir, no con cada tecla. */
const SEARCH_DEBOUNCE_MS = 300;

/** Gestión de usuarios del administrador: lista, rol (RF-36), contraseña temporal (RF-35) y desactivar (RF-05). */
@Component({
  selector: 'app-users',
  imports: [
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    RouterLink
  ],
  providers: [{ provide: MatPaginatorIntl, useClass: PaginatorIntlEs }],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComponent {
  private userSvc = inject(UserService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  auth            = inject(AuthService);

  readonly roles = USER_ROLES;
  readonly roleLabel = ROLE_LABEL;

  readonly pageSize = PAGE_SIZE;

  /** Por nombre, usuario o email (RF-37: quien olvida su usuario se busca por su email). */
  query   = signal('');
  /** Lo escrito, cuando se deja de escribir: busca el servidor. */
  searchTerm = toSignal(toObservable(this.query).pipe(
    debounceTime(SEARCH_DEBOUNCE_MS), map(q => q.trim()), distinctUntilChanged()), { initialValue: '' });
  /** Página que se ve, desde 0; buscar otra cosa vuelve a la primera. */
  page = linkedSignal({ source: this.searchTerm, computation: () => 0 });
  /** Por páginas (RNF-09): con cientos de usuarios, traerlos todos hacía la pantalla muy pesada. */
  users = rxResource({
    params: () => ({ q: this.searchTerm(), page: this.page() }),
    stream: ({ params }) => this.userSvc.search(params.q, params.page, PAGE_SIZE)
  });

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
    this.users.value.update(page => page && { ...page, items: page.items.map(u => u.id === updated.id ? updated : u) });
  }
}
