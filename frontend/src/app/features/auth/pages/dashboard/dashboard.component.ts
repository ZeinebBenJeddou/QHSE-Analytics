import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';import { MatCardModule } from '@angular/material/card';import { CommonModule } from '@angular/common';
import { TokenService } from '../../../../core/services/token.service';


@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatCardModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent {
  private readonly tokenService = inject(TokenService) as TokenService;
  private readonly router = inject(Router);

  logout(): void {
    this.tokenService.removeToken();
    this.router.navigate(['/auth/login']);
  }
}
