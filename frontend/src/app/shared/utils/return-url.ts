import { ActivatedRoute } from '@angular/router';

const DEFAULT_URL = '/dashboard';

/** La ruta a la que volver tras el login o un cambio de contraseña; solo rutas internas. */
export function safeReturnUrl(route: ActivatedRoute): string {
  const url = route.snapshot.queryParamMap.get('returnUrl');
  return url && url.startsWith('/') && !url.startsWith('//') ? url : DEFAULT_URL;
}
