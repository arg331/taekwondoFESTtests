import { Routes } from '@angular/router';

export const EXAM_TAKE_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./exam-take.component').then(m => m.ExamTakeComponent)
  }
];
