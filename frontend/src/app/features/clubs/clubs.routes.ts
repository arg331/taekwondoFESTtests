import { Routes } from '@angular/router';

export const CLUBS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./clubs.component').then(m => m.ClubsComponent)
  }
];
