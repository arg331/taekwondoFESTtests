import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';

export const AUTH_ROUTES: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./register/register.component').then(m => m.RegisterComponent)
  },
  {
    // Recuperar el usuario o la contraseña sin sesión (RF-34)
    path: 'recover',
    loadComponent: () =>
      import('./recover-account/recover-account.component').then(m => m.RecoverAccountComponent)
  },
  {
    // El enlace del correo: /auth/reset-password?token=...
    path: 'reset-password',
    loadComponent: () =>
      import('./reset-password/reset-password.component').then(m => m.ResetPasswordComponent)
  },
  {
    // Cambio voluntario (RF-06) y de la contraseña temporal (RF-33): pide sesión
    path: 'change-password',
    canMatch: [authGuard],
    loadComponent: () =>
      import('./change-password/change-password.component').then(m => m.ChangePasswordComponent)
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  }
];
