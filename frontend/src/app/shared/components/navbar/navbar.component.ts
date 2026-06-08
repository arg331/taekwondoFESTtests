import { Component, inject, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDividerModule
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss'
})
export class NavbarComponent {
  auth   = inject(AuthService);
  router = inject(Router);

  adminLinks = [
    { label: 'Dashboard',  path: '/dashboard',  icon: 'dashboard' },
    { label: 'Preguntas',  path: '/questions',  icon: 'quiz' },
    { label: 'Exámenes',   path: '/exams',       icon: 'assignment' },
    { label: 'Resultados', path: '/results',     icon: 'bar_chart' },
    { label: 'Usuarios',   path: '/users',       icon: 'people' },
  ];

  studentLinks = [
    { label: 'Dashboard',      path: '/dashboard', icon: 'dashboard' },
    { label: 'Mis resultados', path: '/results',   icon: 'bar_chart' },
  ];

  navLinks = computed(() =>
    this.auth.isAdmin() ? this.adminLinks : this.studentLinks
  );

  logout(): void {
    this.auth.logout();
  }
}
