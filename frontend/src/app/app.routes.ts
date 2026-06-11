import { Routes } from '@angular/router';
import { HomeComponent } from './features/auth/pages/home/home.component';
import { LoginPage } from './features/auth/pages/login/login.component';
import { OtpComponent } from './features/auth/pages/otp/otp.component';
import { RegisterComponent } from './features/auth/pages/register/register.component';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/pages/reset-password/reset-password.component';
import { VerifyAccountComponent } from './features/auth/pages/verify-account/verify-account.component';

import { AdminShellComponent } from './features/admin/admin-shell.component';
import { AdminOverviewComponent } from './features/admin/pages/overview/overview.component';
import { adminOverviewResolver } from './features/admin/pages/overview/admin-overview.resolver';
import { AdminUsersComponent } from './features/admin/pages/users/users.component';
import { adminUsersResolver } from './features/admin/pages/users/users.resolver';
import { AdminKpisComponent } from './features/admin/pages/kpis/kpis.component';
import { adminKpisResolver } from './features/admin/pages/kpis/kpis.resolver';
import { AdminProfileComponent } from './features/admin/pages/profile/profile.component';
import { adminProfileResolver } from './features/admin/pages/profile/profile.resolver';
import { AdminHistoriqueComponent } from './features/admin/pages/historique/historique-admin.component';
import { adminHistoriqueResolver } from './features/admin/pages/historique/historique-admin.resolver';
import { AdminAuditComponent } from './features/admin/pages/audit/audit.component';
import { adminAuditResolver } from './features/admin/pages/audit/audit.resolver';

import { AnalysteShellComponent } from './features/analyste/analyste-shell.component';

import { ImportComponent } from './features/analyste/pages/import/import.component';
import { ImportMappingComponent } from './features/analyste/pages/import-mapping/import-mapping.component';
import { HistoriqueComponent } from './features/analyste/pages/historique/historique.component';
import { DashboardAnalysteComponent } from './features/analyste/pages/dashboard/dashboard-analyste.component';
import { AnalyseIAComponent } from './features/analyste/pages/analyse-ia/analyse-ia.component';
import { adminGuard } from './core/guards/admin.guard';
import { analysteGuard } from './core/guards/analyste.guard';
import { ExportPdfComponent } from './features/analyste/pages/export-pdf/export-pdf.component';
import { AnalysteProfilComponent } from './features/profil/analyste-profil.component';

export const routes: Routes = [
  { path: '', redirectTo: 'auth', pathMatch: 'full' },
  { path: 'auth', component: HomeComponent },
  { path: 'auth/login', component: LoginPage },
  { path: 'auth/otp', component: OtpComponent },
  { path: 'auth/register', component: RegisterComponent },
  { path: 'auth/forgot-password', component: ForgotPasswordComponent },
  { path: 'auth/reset-password', component: ResetPasswordComponent },
  { path: 'auth/verify', component: VerifyAccountComponent },

 
  {
    path: 'admin',
    component: AdminShellComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview', component: AdminOverviewComponent, resolve: { data: adminOverviewResolver } },
      { path: 'historique', component: AdminHistoriqueComponent, resolve: { historique: adminHistoriqueResolver } },
      { path: 'audit', component: AdminAuditComponent, resolve: { audit: adminAuditResolver } },
      { path: 'users', component: AdminUsersComponent, resolve: { users: adminUsersResolver } },
      { path: 'kpis', component: AdminKpisComponent, resolve: { kpis: adminKpisResolver } },
      { path: 'analyses/:userId/:id', component: AnalyseIAComponent },
      { path: 'profile', component: AdminProfileComponent, resolve: { profile: adminProfileResolver } },
    ],
  },


  {
    path: 'analyste',
    component: AnalysteShellComponent,
    canActivate: [analysteGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardAnalysteComponent },
      { path: 'dashboard/:id', component: DashboardAnalysteComponent },
      { path: 'import', component: ImportComponent },
      { path: 'import/mapping', component: ImportMappingComponent },
      { path: 'historique', component: HistoriqueComponent },
      { path: 'profile', component: AdminProfileComponent, resolve: { profile: adminProfileResolver } },
      { path: 'ia', component: AnalyseIAComponent },
      { path: 'ia/:id', component: AnalyseIAComponent },
      { path: 'export', component: ExportPdfComponent },
      { path: 'profil-qhse', component: AnalysteProfilComponent },
    ],
  },

  { path: '**', redirectTo: 'auth' },
];
