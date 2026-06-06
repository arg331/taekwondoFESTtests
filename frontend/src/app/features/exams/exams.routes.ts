import { Routes } from '@angular/router';

export const EXAMS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./exams.component').then(m => m.ExamsComponent)
  }
];
