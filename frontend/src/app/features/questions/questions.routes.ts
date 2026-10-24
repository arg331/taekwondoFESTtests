import { Routes } from '@angular/router';

export const QUESTIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./questions.component').then(m => m.QuestionsComponent)
  }
];
