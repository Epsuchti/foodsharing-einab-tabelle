import { Routes } from '@angular/router';

import { UserPermission } from './api';
import { authGuard } from './core/auth.guard';
import { bezirkGuard } from './core/bezirk.guard';
import { AdminDashboardPageComponent } from './pages/admin-dashboard-page/admin-dashboard-page.component';
import { AdminFoodsharingAutomationPageComponent } from './pages/admin-foodsharing-automation-page/admin-foodsharing-automation-page.component';
import { BezirkLandingPageComponent } from './pages/bezirk-landing-page/bezirk-landing-page.component';
import { ConfirmBookingPageComponent } from './pages/confirm-booking-page/confirm-booking-page.component';
import { FoodsharingIdHelpPageComponent } from './pages/foodsharing-id-help-page/foodsharing-id-help-page.component';
import { LoginPageComponent } from './pages/login-page/login-page.component';
import { MyBookingsPageComponent } from './pages/my-bookings-page/my-bookings-page.component';
import { PublicSlotsPageComponent } from './pages/public-slots-page/public-slots-page.component';
import { UnsubscribePageComponent } from './pages/unsubscribe-page/unsubscribe-page.component';
import { TeacherBookingsPageComponent } from './pages/teacher-bookings-page/teacher-bookings-page.component';
import { TeacherDashboardPageComponent } from './pages/teacher-dashboard-page/teacher-dashboard-page.component';
import { TeacherSignupPageComponent } from './pages/teacher-signup-page/teacher-signup-page.component';
import { VerifyLoginPageComponent } from './pages/verify-login-page/verify-login-page.component';

export const routes: Routes = [
  { path: '', component: BezirkLandingPageComponent },
  {
    path: 'bezirke/:bezirkSlug',
    canActivate: [bezirkGuard],
    children: [
      { path: '', component: PublicSlotsPageComponent },
      { path: 'foodsharing-id', component: FoodsharingIdHelpPageComponent },
      { path: 'teacher-signup', component: TeacherSignupPageComponent },
      { path: 'login', component: LoginPageComponent },
      { path: 'verify-login', component: VerifyLoginPageComponent },
      { path: 'confirm-booking', component: ConfirmBookingPageComponent },
      { path: 'unsubscribe', component: UnsubscribePageComponent },
      { path: 'my-bookings', component: MyBookingsPageComponent, canActivate: [authGuard] },
      { path: 'teacher', component: TeacherDashboardPageComponent, canActivate: [authGuard], data: { permissions: [UserPermission.CanGiveEinAbs] } },
      { path: 'teacher/bookings', component: TeacherBookingsPageComponent, canActivate: [authGuard], data: { permissions: [UserPermission.CanGiveEinAbs] } },
      { path: 'admin', component: AdminDashboardPageComponent, canActivate: [authGuard], data: { permissions: [UserPermission.CanManageUsers] } },
      { path: 'admin/foodsharing-automation', component: AdminFoodsharingAutomationPageComponent, canActivate: [authGuard], data: { permissions: [UserPermission.CanUseAutomationSlotApproval, UserPermission.CanUseAutomationRequestApproval, UserPermission.CanUseAutomationOpenSlotAdvertising, UserPermission.CanSeeUserPickupCountGrouping, UserPermission.CanSeeAllAutomationDecisions] } }
    ]
  },
  { path: '**', redirectTo: '' }
];
