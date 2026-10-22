import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
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
  const requested = router.currentNavigation()?.extractedUrl;
  const queryParams = requested ? { returnUrl: router.serializeUrl(requested) } : {};
  return router.createUrlTree(['/auth/login'], { queryParams });
};
