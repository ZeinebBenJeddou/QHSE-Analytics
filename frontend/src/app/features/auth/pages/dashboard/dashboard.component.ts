import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';import { MatCardModule } from '@angular/material/card';import { CommonModule } from '@angular/common';
import { TokenService } from '../../../../core/services/token.service';
import { AuthService } from '../../../../core/services/auth.service';


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatCardModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
// Pure redirect component: on init, sends admins to /admin/overview and analystes to /analyste/dashboard.
export class DashboardComponent {
  private readonly tokenService = inject(TokenService) as TokenService;
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  get isAdmin(): boolean {
    return this.tokenService.isAdmin();
  }

  get isAnalyste(): boolean {
    return this.tokenService.isAnalyste();
  }

  ngOnInit(): void {
    if (this.tokenService.isAdmin()) {
      this.router.navigate(['/admin/overview']);
    } else if (this.tokenService.isAnalyste()) {
      this.router.navigate(['/analyste/dashboard']);
    }
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

  goToAdminOverview(): void {
    this.router.navigate(['/admin/overview']);
  }

  goToAnalysteDashboard(): void {
    this.router.navigate(['/analyste/dashboard']);
  }
}
