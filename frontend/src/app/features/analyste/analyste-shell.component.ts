import { Component, OnInit, inject } from '@angular/core';
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
import { AuthService } from '../../core/services/auth.service';
import { DashboardService } from '../../core/services/dashboard.service';
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
export class AnalysteShellComponent implements OnInit {
  private tokenService = inject(TokenService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private dashboardService = inject(DashboardService);

  sidenavOpen = true;
  activeLabel = 'Tableau de bord';
  notificationCount = 0;
  profileName = 'Analyste QHSE';
  profileRole = 'Analyste';

  dashItems = [
    { label: 'Tableau de bord', icon: 'dashboard', route: '/analyste/dashboard', badge: null as number | null },
    { label: 'Profil', icon: 'person', route: '/analyste/profile', badge: null as number | null },
  ];

  dataItems = [
    { label: 'Importer données', icon: 'upload_file', route: '/analyste/import', badge: null as number | null },
    { label: 'Historique imports', icon: 'history', route: '/analyste/historique', badge: null as number | null },
  ];

  navItems = [
    { label: 'Comparatif N vs N-1', icon: 'compare_arrows', route: '/analyste/comparatif', badge: null as number | null },
    { label: 'Analyse IA',          icon: 'analytics',       route: '/analyste/ia',          badge: null as number | null },
    { label: 'Alertes',             icon: 'notifications_active', route: '/analyste/alertes', badge: null as number | null },
  ];

  rapportItems = [
    { label: 'Export PDF', icon: 'picture_as_pdf', route: '/analyste/export', badge: null as number | null },
  ];

  constructor() {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe(() => {
        const allItems = [
          ...this.dashItems, ...this.dataItems,
          ...this.navItems, ...this.rapportItems,
        ];
        const match = allItems.find(i =>
          this.router.isActive(i.route, { paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' })
        );
        if (match) this.activeLabel = match.label;
      });
  }

  ngOnInit(): void {
    this.dashboardService.getAlertes().subscribe({
      next: (res) => {
        const count = res?.count ?? 0;
        this.notificationCount = count;
        const alerteItem = this.navItems.find(i => i.route === '/analyste/alertes');
        if (alerteItem) alerteItem.badge = count > 0 ? count : null;
      },
      error: () => {},
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
}