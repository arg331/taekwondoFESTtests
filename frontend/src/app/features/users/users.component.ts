import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { UserResponse } from '../../core/models/auth.models';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatDividerModule
  ],
  templateUrl: './users.component.html',
  styleUrl: './users.component.scss'
})
export class UsersComponent implements OnInit {
  private userSvc = inject(UserService);
  auth            = inject(AuthService);

  loading = signal(true);
  users   = signal<UserResponse[]>([]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.userSvc.getAll().subscribe({
      next: u => { this.users.set(u); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  promote(user: UserResponse): void {
    this.userSvc.promote(user.id).subscribe({
      next: updated => this.users.update(
        list => list.map(u => u.id === updated.id ? updated : u)
      )
    });
  }

  demote(user: UserResponse): void {
    if (!confirm(`¿Degradar a ${user.displayName} a estudiante?`)) return;
    this.userSvc.demote(user.id).subscribe({
      next: updated => this.users.update(
        list => list.map(u => u.id === updated.id ? updated : u)
      )
    });
  }

  isCurrentUser(user: UserResponse): boolean {
    return user.id === this.auth.currentUser()?.id;
  }
}
