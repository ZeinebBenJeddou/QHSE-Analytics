import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { AdminService } from '../../core/services/admin.service';
import { KpiService } from '../../core/services/kpi.service';
import { NotificationService } from '../../core/services/notification.service';
import {
  AdminKpiCritiqueResponse,
  AdminStatsResponse,
  CategorieKpiResponse,
  CreateAnalysteRequest,
  KpiResponse,
  UserListResponse,
  UserResponse,
  UserRole,
} from '../../core/models/api.models';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent implements OnInit {
  userCount = 0;
  stats: AdminStatsResponse | null = null;
  users: UserResponse[] = [];
  kpis: KpiResponse[] = [];
  categories: CategorieKpiResponse[] = [];
  criticalKpis: AdminKpiCritiqueResponse[] = [];
  selectedCategory = '';
  showInactive = false;
  creatingUser = false;
  createForm: CreateAnalysteRequest = { nom: '', prenom: '', email: '', password: '' };

  constructor(
    private readonly adminService: AdminService,
    private readonly kpiService: KpiService,
    private readonly notificationService: NotificationService,
  ) {}

  ngOnInit(): void {
    this.reloadAll();
  }

  reloadAll(): void {
    this.loadStats();
    this.loadUsers();
    this.loadCategories();
    this.loadKpis();
    this.loadCriticalKpis();
  }

  loadStats(): void {
    this.adminService.getStats().subscribe({ next: (stats) => this.stats = stats });
  }

  loadUsers(): void {
    this.adminService.getUsers().subscribe({
      next: (response: UserListResponse) => {
        this.users = response.users;
        this.userCount = response.users.length;
      }
    });
  }

  loadCategories(): void {
    this.kpiService.getCategories().subscribe({ next: (categories) => this.categories = categories });
  }

  loadKpis(): void {
    if (this.showInactive) {
      this.kpiService.getInactiveKpis().subscribe({ next: (kpis) => this.kpis = kpis });
      return;
    }

    this.kpiService.getKpis(this.selectedCategory || undefined).subscribe({ next: (kpis) => this.kpis = kpis });
  }

  loadCriticalKpis(): void {
    this.adminService.getKpisCritiques().subscribe({ next: (items) => this.criticalKpis = items });
  }

  onCategoryChange(): void {
    this.loadKpis();
  }

  toggleInactive(): void {
    this.showInactive = !this.showInactive;
    this.loadKpis();
  }

  createAnalyste(): void {
    if (!this.createForm.nom || !this.createForm.prenom || !this.createForm.email || !this.createForm.password) {
      this.notificationService.error('Complétez tous les champs avant la création.');
      return;
    }

    this.creatingUser = true;
    this.adminService.createAnalyste(this.createForm).subscribe({
      next: () => {
        this.creatingUser = false;
        this.notificationService.success('Analyste créé avec succès.');
        this.createForm = { nom: '', prenom: '', email: '', password: '' };
        this.loadUsers();
      },
      error: () => {
        this.creatingUser = false;
      }
    });
  }

  refreshUser(userId: number): void {
    this.adminService.getUserById(userId).subscribe({
      next: () => this.loadUsers()
    });
  }

  verifyUser(userId: number): void {
    this.adminService.verifyUser(userId).subscribe({ next: () => { this.notificationService.success('Compte vérifié.'); this.loadUsers(); } });
  }

  toggleUserActive(user: UserResponse): void {
    const request = user.active ? this.adminService.deactivateUser(user.id) : this.adminService.activateUser(user.id);
    request.subscribe({ next: () => this.loadUsers() });
  }

  promote(user: UserResponse): void {
    this.adminService.promoteToAdmin(user.id).subscribe({ next: () => this.loadUsers() });
  }

  demote(user: UserResponse): void {
    this.adminService.demoteToAnalyste(user.id).subscribe({ next: () => this.loadUsers() });
  }

  resetPassword(user: UserResponse): void {
    this.adminService.resetPassword(user.id).subscribe({
      next: () => this.notificationService.success(`Mot de passe réinitialisé pour ${user.email}.`)
    });
  }

  deleteUser(user: UserResponse): void {
    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        this.notificationService.success('Utilisateur supprimé.');
        this.loadUsers();
      }
    });
  }

  userRoleLabel(role: UserRole): string {
    return role === 'ADMIN' ? 'Administrateur' : 'Analyste';
  }
}
