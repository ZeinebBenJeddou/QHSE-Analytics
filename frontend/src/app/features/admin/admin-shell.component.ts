import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';
import { TokenService } from '../../core/services/token.service';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [RouterModule, MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule],
  template: `
    <div class="admin-shell">
      <mat-toolbar color="primary" class="admin-toolbar">
        <span class="brand">Admin Console</span>
        <span class="spacer"></span>
        <button mat-button routerLink="overview">Overview</button>
        <button mat-button routerLink="users">Users</button>
        <button mat-button routerLink="kpis">KPIs</button>

        <button mat-icon-button [matMenuTriggerFor]="profileMenu" aria-label="Admin profile menu">
          <mat-icon>account_circle</mat-icon>
        </button>
        <mat-menu #profileMenu="matMenu">
          <button mat-menu-item routerLink="profile">
            <mat-icon>person</mat-icon>
            <span>Profile</span>
          </button>
          <button mat-menu-item (click)="logout()">
            <mat-icon>logout</mat-icon>
            <span>Logout</span>
          </button>
        </mat-menu>
      </mat-toolbar>

      <main class="admin-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [
    '.admin-shell { display: grid; min-height: 100vh; }',
    '.admin-toolbar { position: sticky; top: 0; z-index: 2; }',
    '.brand { font-weight: 600; }',
    '.spacer { flex: 1 1 auto; }',
    '.admin-content { padding: 24px; }'
  ]
})
export class AdminShellComponent {
  private readonly router = inject(Router);
  private readonly tokenService = inject(TokenService);

  logout(): void {
    this.tokenService.removeToken();
    this.router.navigate(['/auth/login']);
  }
}
