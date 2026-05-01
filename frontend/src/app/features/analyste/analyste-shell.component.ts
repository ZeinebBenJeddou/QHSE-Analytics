import { Component, inject } from '@angular/core';
import { Router, RouterModule, RouterOutlet, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { TokenService } from '../../core/services/token.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-analyste-shell',
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
    MatBadgeModule,
  ],
  templateUrl: './analyste-shell.component.html',
  styleUrls: ['./analyste-shell.component.css'],
})
export class AnalysteShellComponent {
  private tokenService = inject(TokenService);
  private router = inject(Router);

  sidenavOpen = true;
  activeLabel = 'Tableau de bord';

  navItems = [
    { label: 'Profil',            icon: 'person',       route: '/analyste/profile',    badge: null },
    { label: 'Tableau de bord',   icon: 'dashboard',    route: '/analyste/dashboard',  badge: null },
    { label: 'Importer des données', icon: 'upload_file', route: '/analyste/import',   badge: null },
    { label: 'Historique',        icon: 'history',      route: '/analyste/historique', badge: null },
    { label: 'Analyse IA',        icon: 'psychology',   route: '/analyste/ia',         badge: 'IA' },
  ];

  constructor() {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        const match = this.navItems.find(i =>
          this.router.isActive(i.route, { paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' })
        );
        if (match) this.activeLabel = match.label;
      });
  }

  logout(): void {
    this.tokenService.removeToken();
    this.router.navigate(['/auth/login']);
  }

  toggleSidenav(): void {
    this.sidenavOpen = !this.sidenavOpen;
  }
}