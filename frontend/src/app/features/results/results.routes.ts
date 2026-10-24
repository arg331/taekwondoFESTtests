import { Routes } from '@angular/router';
import { teacherGuard } from '../../core/guards/role.guard';

export const RESULTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./results.component').then(m => m.ResultsComponent)
  },
  {
    // Resultados de un usuario (RF-55): profesor o administrador
    path: 'user/:id',
    canMatch: [teacherGuard],
    loadComponent: () =>
      import('./components/user-results/user-results.component').then(m => m.UserResultsComponent)
  }
];
