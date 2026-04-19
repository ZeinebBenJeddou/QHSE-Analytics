import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent {
  data = { email: '', password: '' };
  hidePassword = true;

  constructor(
    private auth: AuthService,
    private router: Router
  ) {}

  login(): void {
    this.auth.setPendingEmail(this.data.email);

    this.auth.login(this.data).subscribe({
      next: () => {
        this.router.navigate(['/otp']);
      },
      error: (err) => {
        this.auth.clearPendingEmail();
        alert(err.error?.message || 'Email ou mot de passe incorrect');
      }
    });
  }
}