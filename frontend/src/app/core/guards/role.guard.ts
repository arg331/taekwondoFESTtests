import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Guard que bloquea rutas a usuarios con rol STUDENT.
 * Solo ADMIN puede acceder a preguntas, exámenes y gestión de usuarios.
 */
export const adminGuard: CanActivateFn = () => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  if (auth.isAdmin()) return true;

  router.navigate(['/dashboard']);
  return false;
};
