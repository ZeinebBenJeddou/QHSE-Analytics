import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RouterOutlet, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { AuthStorageService } from './shared/services/auth-storage.service';
import { AuthService } from './shared/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterModule, MatToolbarModule, MatButtonModule, MatIconModule],
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class App implements OnInit {
  constructor(
    private router: Router,
    public authStorage: AuthStorageService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (!this.authStorage.authState() && this.authStorage.refreshToken) {
      this.authService.refreshToken({ refreshToken: this.authStorage.refreshToken }).subscribe({
        next: () => {
          // Successful refresh will update authState through AuthStorageService.
        },
        error: () => {
          this.authStorage.clear();
        }
      });
    }
  }

  protected logout() {
    this.authStorage.clear();
    this.router.navigate(['/login']);
  }
}
