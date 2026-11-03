import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { OVERLAY_DEFAULT_CONFIG } from '@angular/cdk/overlay';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/interceptors/jwt.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    // Con popover (lo que usa la CDK 21 por defecto), un desplegable dentro de un diálogo queda detrás de su
    // propio fondo transparente y no se puede elegir nada («Mi club», el grado en «Nueva pregunta»...)
    { provide: OVERLAY_DEFAULT_CONFIG, useValue: { usePopover: false } }
  ]
};
