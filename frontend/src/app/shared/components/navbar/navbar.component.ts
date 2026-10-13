import { ChangeDetectionStrategy, Component, inject, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { ROLE_LABEL } from '../../../core/models/auth.models';
import { APP_NAME } from '../../../core/app-brand';

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
  styleUrl: './navbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent {
  readonly appName = APP_NAME;
  auth   = inject(AuthService);
  router = inject(Router);

  teacherLinks = [
    { label: 'Dashboard',  path: '/dashboard',  icon: 'dashboard' },
    { label: 'Preguntas',  path: '/questions',  icon: 'quiz' },
    { label: 'Exámenes',   path: '/exams',       icon: 'assignment' },
    { label: 'Resultados', path: '/results',     icon: 'bar_chart' },
  ];

  adminLinks = [
    ...this.teacherLinks,
    { label: 'Usuarios',   path: '/users',       icon: 'people' },
  ];

  studentLinks = [
    { label: 'Dashboard',      path: '/dashboard', icon: 'dashboard' },
    { label: 'Mis resultados', path: '/results',   icon: 'bar_chart' },
  ];

  navLinks = computed(() =>
    this.auth.isAdmin() ? this.adminLinks : this.auth.canTeach() ? this.teacherLinks : this.studentLinks
  );
  roleLabel = computed(() => {
    const role = this.auth.currentUser()?.role;
    return role ? ROLE_LABEL[role] : '';
  });

  logout(): void {
    this.auth.logout();
  }
}
