import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Preguntas y exámenes: profesor o administrador (RF-04).
 * Como canMatch, un alumno ni siquiera descarga el código de esas pantallas.
 */
export const teacherGuard: CanMatchFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.canTeach() ? true : router.createUrlTree(['/dashboard']);
};

/** Gestión de usuarios y roles: solo el administrador (RF-36). */
export const adminGuard: CanMatchFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isAdmin() ? true : router.createUrlTree(['/dashboard']);
};
