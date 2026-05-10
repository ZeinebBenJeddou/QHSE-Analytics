import { Component, inject } from '@angular/core';
import { Router, RouterModule, RouterOutlet, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TokenService } from '../../core/services/token.service';
import { AuthService } from '../../core/services/auth.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-admin-shell',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterModule,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './admin-shell.component.html',
  styleUrls: ['./admin-shell.component.css'],
})
export class AdminShellComponent {
  private readonly tokenService = inject(TokenService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  sidenavOpen = true;
  activeLabel = 'Overview';

  navItems = [
    { label: 'Profil',        icon: 'person',           route: '/admin/profile',  badge: null },
    { label: 'Overview',      icon: 'bar_chart',        route: '/admin/overview', badge: null },
    { label: 'Historique',    icon: 'history',          route: '/admin/historique', badge: null },
    { label: 'Utilisateurs',  icon: 'group',            route: '/admin/users',    badge: null },
    { label: 'KPIs',          icon: 'speed',            route: '/admin/kpis',     badge: null },
    { label: 'Audit',         icon: 'policy',           route: '/admin/audit',     badge: null },
    { label: 'Base RAG',      icon: 'hub',              route: '/admin/rag',       badge: null },
    { label: 'Santé IA',      icon: 'monitor_heart',    route: '/admin/ia-health', badge: null },
    { label: 'Config IA',     icon: 'tune',             route: '/admin/ia-config',       badge: null },
    { label: 'Rétention',    icon: 'delete_sweep',     route: '/admin/data-retention',  badge: null },
  ];

  constructor() {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        const match = this.navItems.find(i =>
          this.router.isActive(i.route, {
            paths: 'subset', queryParams: 'ignored',
            fragment: 'ignored', matrixParams: 'ignored',
          })
        );
        if (match) this.activeLabel = match.label;
      });
  }

  logout(): void {
    this.authService.logout().subscribe({
      complete: () => {
        this.tokenService.removeToken();
        this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.tokenService.removeToken();
        this.router.navigate(['/auth/login']);
      }
    });
  }

  toggleSidenav(): void {
    this.sidenavOpen = !this.sidenavOpen;
  }

  trackByRoute(index: number, item: { route: string }): string {
    return item.route;
  }
}