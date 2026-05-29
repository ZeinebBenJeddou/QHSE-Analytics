import { Component, OnInit, OnDestroy, inject } from '@angular/core';
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
import { Subject } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';

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
export class AnalysteShellComponent implements OnInit, OnDestroy {
  private tokenService = inject(TokenService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private dashboardService = inject(DashboardService);
  private readonly destroy$ = new Subject<void>();

  sidenavOpen = true;
  activeLabel = 'Tableau de bord';
  notificationCount = 0;
  profileName = 'Analyste QHSE';
  profileRole = 'Analyste';

  dashItems = [
    { label: 'Tableau de bord', route: '/analyste/dashboard', badge: null as number | null },
  ];

  dataItems = [
    { label: 'Importer données',  route: '/analyste/import',     badge: null as number | null },
    { label: 'Historique imports',route: '/analyste/historique', badge: null as number | null },
  ];

  navItems = [
    { label: 'Analyse',  route: '/analyste/ia',     badge: null as number | null },
    { label: 'Export PDF',route: '/analyste/export', badge: null as number | null },
  ];

  profilItems = [
    { label: 'Profil',        route: '/analyste/profile',    badge: null as number | null },
    { label: 'Contexte QHSE', route: '/analyste/profil-qhse',badge: null as number | null },
  ];

  constructor() {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd), takeUntil(this.destroy$))
      .subscribe(() => {
        const allItems = [
          ...this.dashItems, ...this.dataItems, ...this.navItems, ...this.profilItems,
        ];
        const match = allItems.find(i =>
          this.router.isActive(i.route, { paths: 'subset', queryParams: 'ignored', fragment: 'ignored', matrixParams: 'ignored' })
        );
        if (match) this.activeLabel = match.label;
      });
  }

  ngOnInit(): void {
    this.dashboardService.getAlertes().pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => {
        this.notificationCount = res?.count ?? 0;
      },
      error: () => {},
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
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