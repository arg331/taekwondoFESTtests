import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  // Redirige la raíz al dashboard
  {
    path: '',
    redirectTo: 'dashboard',
    pathMatch: 'full'
  },

  // Rutas públicas (auth)
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },

  // Ruta pública: estudiante accede al examen por código
  {
    path: 'exam/:code',
    loadChildren: () =>
      import('./features/exam-take/exam-take.routes').then(m => m.EXAM_TAKE_ROUTES)
  },

  // Rutas privadas (requieren login)
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
  },
  {
    path: 'questions',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/questions/questions.routes').then(m => m.QUESTIONS_ROUTES)
  },
  {
    path: 'exams',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/exams/exams.routes').then(m => m.EXAMS_ROUTES)
  },
  {
    path: 'results',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/results/results.routes').then(m => m.RESULTS_ROUTES)
  },

  // Ruta 404
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
