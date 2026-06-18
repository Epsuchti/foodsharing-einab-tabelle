import { Routes } from '@angular/router';

import { UserRole } from './api';
import { authGuard } from './core/auth.guard';
import { AdminDashboardPageComponent } from './pages/admin-dashboard-page/admin-dashboard-page.component';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { MyBookingsPageComponent } from './pages/my-bookings-page/my-bookings-page.component';
import { PublicSlotsPageComponent } from './pages/public-slots-page/public-slots-page.component';
import { TeacherBookingsPageComponent } from './pages/teacher-bookings-page/teacher-bookings-page.component';
import { TeacherDashboardPageComponent } from './pages/teacher-dashboard-page/teacher-dashboard-page.component';
import { TeacherSignupPageComponent } from './pages/teacher-signup-page/teacher-signup-page.component';
import { UnsubscribePageComponent } from './pages/unsubscribe-page/unsubscribe-page.component';
import { VerifyLoginPageComponent } from './pages/verify-login-page/verify-login-page.component';

export const routes: Routes = [
  { path: '', component: PublicSlotsPageComponent },
  { path: 'teacher-signup', component: TeacherSignupPageComponent },
  { path: 'login', component: LoginPageComponent },
  { path: 'verify-login', component: VerifyLoginPageComponent },
  { path: 'unsubscribe', component: UnsubscribePageComponent },
  { path: 'my-bookings', component: MyBookingsPageComponent, canActivate: [authGuard], data: { roles: [UserRole.User, UserRole.Teacher, UserRole.Admin] } },
  { path: 'teacher', component: TeacherDashboardPageComponent, canActivate: [authGuard], data: { roles: [UserRole.Teacher, UserRole.Admin] } },
  { path: 'teacher/bookings', component: TeacherBookingsPageComponent, canActivate: [authGuard], data: { roles: [UserRole.Teacher, UserRole.Admin] } },
  { path: 'admin', component: AdminDashboardPageComponent, canActivate: [authGuard], data: { roles: [UserRole.Admin] } },
  { path: '**', redirectTo: '' }
];
