import { inject } from '@angular/core';
import { CanMatchFn, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * Protege rutas privadas: sin sesión, redirige a login y vuelve después.
 * Como canMatch, ni siquiera se descarga el código de la ruta.
 */
export const authGuard: CanMatchFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }
  return redirectKeepingDestination(router, '/auth/login');
};

/**
 * Con una contraseña temporal (RF-33) solo se puede cambiarla: cualquier otra ruta lleva antes
 * a hacerlo y después vuelve. Sin sesión no hace nada; de eso se encarga authGuard.
 */
export const passwordChangedGuard: CanMatchFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.mustChangePassword() ? redirectKeepingDestination(router, '/auth/change-password') : true;
};

/** Redirige a `target` con la ruta pedida en returnUrl, para volver a ella después. */
function redirectKeepingDestination(router: Router, target: string): UrlTree {
  const requested = router.currentNavigation()?.extractedUrl;
  const queryParams = requested ? { returnUrl: router.serializeUrl(requested) } : {};
  return router.createUrlTree([target], { queryParams });
}
