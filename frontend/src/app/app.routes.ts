import { Routes } from '@angular/router';
import { HomeComponent } from './features/auth/pages/home/home.component';
import { LoginPage } from './features/auth/pages/login/login.component';
import { OtpComponent } from './features/auth/pages/otp/otp.component';
import { RegisterComponent } from './features/auth/pages/register/register.component';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/pages/reset-password/reset-password.component';
import { VerifyAccountComponent } from './features/auth/pages/verify-account/verify-account.component';
import { DashboardComponent } from './features/auth/pages/dashboard/dashboard.component';
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
import { AdminAuditComponent } from './features/admin/pages/audit/audit.component';
import { adminAuditResolver } from './features/admin/pages/audit/audit.resolver';
import { RagAdminComponent } from './features/admin/pages/rag/rag-admin.component';
import { ragAdminResolver } from './features/admin/pages/rag/rag-admin.resolver';
import { IaHealthComponent } from './features/admin/pages/ia-health/ia-health.component';
import { iaHealthResolver } from './features/admin/pages/ia-health/ia-health.resolver';
import { IaConfigComponent } from './features/admin/pages/ia-config/ia-config.component';
import { iaConfigResolver } from './features/admin/pages/ia-config/ia-config.resolver';
import { DataRetentionComponent } from './features/admin/pages/data-retention/data-retention.component';
import { dataRetentionResolver } from './features/admin/pages/data-retention/data-retention.resolver';
import { AnalysteShellComponent } from './features/analyste/analyste-shell.component';

import { ImportComponent } from './features/analyste/pages/import/import.component';
import { ImportMappingComponent } from './features/analyste/pages/import-mapping/import-mapping.component';
import { HistoriqueComponent } from './features/analyste/pages/historique/historique.component';
import { DashboardAnalysteComponent } from './features/analyste/pages/dashboard/dashboard-analyste.component';
import { AnalyseIAComponent } from './features/analyste/pages/analyse-ia/analyse-ia.component';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { analysteGuard } from './core/guards/analyste.guard';
import { ComparatifComponent } from './features/analyste/pages/comparatif/comparatif.component';
import { ExportPdfComponent } from './features/analyste/pages/export-pdf/export-pdf.component';

export const routes: Routes = [
  { path: '', redirectTo: 'auth', pathMatch: 'full' },
  { path: 'auth', component: HomeComponent },
  { path: 'auth/login', component: LoginPage },
  { path: 'auth/otp', component: OtpComponent },
  { path: 'auth/register', component: RegisterComponent },
  { path: 'auth/forgot-password', component: ForgotPasswordComponent },
  { path: 'auth/reset-password', component: ResetPasswordComponent },
  { path: 'auth/verify', component: VerifyAccountComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },

 
  {
    path: 'admin',
    component: AdminShellComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview', component: AdminOverviewComponent, resolve: { data: adminOverviewResolver } },
      { path: 'historique', component: AdminHistoriqueComponent },
      { path: 'audit', component: AdminAuditComponent, resolve: { audit: adminAuditResolver } },
      { path: 'rag', component: RagAdminComponent, resolve: { entries: ragAdminResolver } },
      { path: 'ia-health', component: IaHealthComponent, resolve: { health: iaHealthResolver } },
      { path: 'ia-config', component: IaConfigComponent, resolve: { configs: iaConfigResolver } },
      { path: 'data-retention', component: DataRetentionComponent, resolve: { policy: dataRetentionResolver } },
      { path: 'users', component: AdminUsersComponent, resolve: { users: adminUsersResolver } },
      { path: 'kpis', component: AdminKpisComponent, resolve: { kpis: adminKpisResolver } },
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
      { path: 'comparatif', component: ComparatifComponent },
      
      { path: 'tendances', component: DashboardAnalysteComponent },
      { path: 'categories', component: DashboardAnalysteComponent },
      { path: 'export', component: ExportPdfComponent },
    ],
  },

  { path: '**', redirectTo: 'auth' },
];