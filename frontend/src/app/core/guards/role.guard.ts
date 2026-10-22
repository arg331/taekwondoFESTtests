import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Solo ADMIN (profesor) puede acceder a preguntas, exámenes y gestión de usuarios.
 * Como canMatch, un alumno ni siquiera descarga el código de esas pantallas.
 */
export const adminGuard: CanMatchFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAdmin() ? true : router.createUrlTree(['/dashboard']);
};
