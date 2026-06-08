import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  // Públicas
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'exam/:code',
    loadChildren: () =>
      import('./features/exam-take/exam-take.routes').then(m => m.EXAM_TAKE_ROUTES)
  },

  // Privadas con layout
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./shared/components/private-layout/private-layout.component')
        .then(m => m.PrivateLayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES)
      },
      {
        path: 'questions',
        canActivate: [adminGuard],
        loadChildren: () =>
          import('./features/questions/questions.routes').then(m => m.QUESTIONS_ROUTES)
      },
      {
        path: 'exams',
        canActivate: [adminGuard],
        loadChildren: () =>
          import('./features/exams/exams.routes').then(m => m.EXAMS_ROUTES)
      },
      {
        path: 'results',
        loadChildren: () =>
          import('./features/results/results.routes').then(m => m.RESULTS_ROUTES)
      },
      {
        path: 'users',
        canActivate: [adminGuard],
        loadChildren: () =>
          import('./features/users/users.routes').then(m => m.USERS_ROUTES)
      }
    ]
  },

  { path: '**', redirectTo: 'dashboard' }
];
