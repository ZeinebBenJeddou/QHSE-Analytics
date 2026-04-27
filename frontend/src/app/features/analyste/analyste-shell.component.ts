import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterModule, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { TokenService } from '../../core/services/token.service';

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

  navItems = [
    { label: 'Tableau de bord', icon: 'dashboard', route: '/analyste/dashboard' },
    { label: "Importer des données", icon: 'upload_file', route: '/analyste/import' },
    { label: 'Historique', icon: 'history', route: '/analyste/historique' },
    { label: 'Analyse IA', icon: 'psychology', route: '/analyste/ia' },
    { label: 'Mapping colonnes', icon: 'tune', route: '/analyste/mapping' },
  ];

  logout(): void {
    this.tokenService.removeToken();
    this.router.navigate(['/auth/login']);
  }

  toggleSidenav(): void {
    this.sidenavOpen = !this.sidenavOpen;
  }
}
