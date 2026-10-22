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
