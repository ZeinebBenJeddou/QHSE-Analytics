import { Routes } from '@angular/router';
import { AuthGuard } from './shared/guards/auth.guard';
import { DashboardPage } from './pages/dashboard-page.component';
import { ForgotPasswordPage, ProfilPage, ResetPasswordPage, UsersPage, VerifyPage } from './pages/placeholder-pages.component';
import { RegisterPageComponent } from './pages/register-page.component';
import { VerifyOtpPageComponent } from './pages/verify-otp-page.component';
import { HomePage } from './pages/home-page.component';
import { ImportPage } from './features/import/import-page.component';
import { KpiPage } from './features/kpi/kpi-page.component';
import { LoginPage } from './pages/login-page.component';
import { ResultatsPage } from './features/resultats/resultats-page.component';
import { HistoriquePage } from './features/historique/historique-page.component';
import { MappingPage } from './features/mapping/mapping-page.component';
import { AnalyseIaPage } from './features/analyse-ia/analyse-ia-page.component';

export const routes: Routes = [
  { path: '', component: HomePage },
  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPageComponent },
  { path: 'verify', component: VerifyPage },
  { path: 'verify-otp', component: VerifyOtpPageComponent },
  { path: 'forgot-password', component: ForgotPasswordPage },
  { path: 'reset-password', component: ResetPasswordPage },
  {
    path: 'auth',
    children: [
      { path: 'verify', component: VerifyPage },
      { path: 'reset-password', component: ResetPasswordPage },
      { path: 'login', redirectTo: '/login', pathMatch: 'full' }
    ]
  },
  { path: 'dashboard', component: DashboardPage, canActivate: [AuthGuard] },
  { path: 'kpis', component: KpiPage, canActivate: [AuthGuard] },
  { path: 'import', component: ImportPage, canActivate: [AuthGuard], data: { roles: ['ANALYSTE'] } },
  { path: 'mapping', component: MappingPage, canActivate: [AuthGuard], data: { roles: ['ANALYSTE'] } },
  { path: 'historique', component: HistoriquePage, canActivate: [AuthGuard], data: { roles: ['ANALYSTE'] } },
  { path: 'resultats', component: ResultatsPage, canActivate: [AuthGuard], data: { roles: ['ANALYSTE'] } },
  { path: 'analyse-ia/:importId', component: AnalyseIaPage, canActivate: [AuthGuard], data: { roles: ['ANALYSTE', 'ADMIN'] } },
  { path: 'users', component: UsersPage, canActivate: [AuthGuard], data: { roles: ['ADMIN'] } },
  { path: 'profil', component: ProfilPage, canActivate: [AuthGuard] },
  { path: '**', redirectTo: '' }
];
