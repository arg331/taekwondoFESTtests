import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from '../navbar/navbar.component';

@Component({
  selector: 'app-private-layout',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar />
    <main class="content">
      <router-outlet />
    </main>
  `,
  styles: [`
    .content {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }
  `]
})
export class PrivateLayoutComponent {}
