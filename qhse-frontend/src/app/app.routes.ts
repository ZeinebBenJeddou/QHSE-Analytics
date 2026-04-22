import { Routes } from '@angular/router';
import { AuthGuard } from './shared/guards/auth.guard';
import { DashboardPage } from './pages/dashboard-page.component';
import { ForgotPasswordPage } from './pages/forgot-password-page.component';
import { HomePage } from './pages/home-page.component';
import { KpiPage } from './pages/kpi-page.component';
import { LoginPage } from './pages/login-page.component';
import { RegisterPage } from './pages/register-page.component';
import { ResetPasswordPage } from './pages/reset-password-page.component';
import { UsersPage } from './pages/users-page.component';
import { VerifyOtpPage } from './pages/verify-otp-page.component';
import { VerifyPage } from './pages/verify-page.component';

export const routes: Routes = [
  { path: '', component: HomePage },
  { path: 'login', component: LoginPage },
  { path: 'register', component: RegisterPage },
  { path: 'verify', component: VerifyPage },
  { path: 'verify-otp', component: VerifyOtpPage },
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
  { path: 'users', component: UsersPage, canActivate: [AuthGuard], data: { roles: ['ADMIN'] } },
  { path: '**', redirectTo: '' }
];
