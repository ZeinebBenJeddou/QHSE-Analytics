import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-verify-account',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './verify.component.html',
  styleUrls: ['./verify.component.css']
})
export class VerifyComponent {
  status: 'loading' | 'success' | 'error' = 'loading';
  message = 'Validation du compte en cours...';
  token = '';

  constructor(
    private auth: AuthService,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    this.verify();
  }

  verify(): void {
    if (!this.token) {
      this.status = 'error';
      this.message = 'Lien de vérification invalide ou token manquant.';
      return;
    }

    this.status = 'loading';
    this.message = 'Validation du compte en cours...';

    this.auth.verifyAccount(this.token).subscribe({
      next: (res: any) => {
        this.status = 'success';
        this.message = res?.message || 'Compte vérifié avec succès.';
        setTimeout(() => this.router.navigate(['/login']), 1500);
      },
      error: (err) => {
        this.status = 'error';
        this.message = err?.error?.error || err?.error?.message || 'Impossible de vérifier le compte.';
      }
    });
  }
}
