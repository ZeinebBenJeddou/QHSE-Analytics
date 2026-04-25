import { Routes } from '@angular/router';
import { HomeComponent } from './features/auth/pages/home/home.component';
import { LoginPage } from './features/auth/pages/login/login.component';
import { OtpComponent } from './features/auth/pages/otp/otp.component';
import { RegisterComponent } from './features/auth/pages/register/register.component';
import { ForgotPasswordComponent } from './features/auth/pages/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/pages/reset-password/reset-password.component';

import { VerifyAccountComponent } from './features/auth/pages/verify-account/verify-account.component';
import { DashboardComponent } from './features/auth/pages/dashboard/dashboard.component';
import { authGuard } from './core/guards/auth.guard';

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
  { path: '**', redirectTo: 'auth' }
];
