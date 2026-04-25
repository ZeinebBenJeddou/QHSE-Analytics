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
import { AdminUsersComponent } from './features/admin/pages/users/users.component';
import { AdminKpisComponent } from './features/admin/pages/kpis/kpis.component';
import { AdminProfileComponent } from './features/admin/pages/profile/profile.component';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

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
      { path: 'overview', component: AdminOverviewComponent },
      { path: 'users', component: AdminUsersComponent },
      { path: 'kpis', component: AdminKpisComponent },
      { path: 'profile', component: AdminProfileComponent }
    ]
  },
  { path: '**', redirectTo: 'auth' }
];
