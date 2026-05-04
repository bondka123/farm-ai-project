import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

// Layout
import { AdminLayoutComponent } from './layouts/admin-layout/admin-layout.component';

// Pages
import { LoginComponent } from './login/login.component';
import { DepartmentsComponent } from './departments/departments.component';
import { ManagersComponent } from './managers/managers.component';
import { ViewersComponent } from './viewers/viewers.component';
import { CamerasComponent } from './cameras/cameras.component';
import { AlertsComponent } from './alerts/alerts.component';
import { UserProfileComponent } from './user-profile/user-profile.component';
import { AuthGuard } from './guards/auth.guard';

// 🔥 AJOUT
import { EmployeesComponent } from './pages/employees/employees.component';
import { FaceRegistrationComponent } from './pages/face-registration/face-registration.component';
import { AnalyticsComponent } from './analytics/analytics.component';
import { ActivateComponent } from './pages/activate/activate.component';
import { FaceSetupComponent } from './pages/face-setup/face-setup.component';
import { ManagerDashboardComponent } from './manager-dashboard/manager-dashboard.component';
import { ViewerDashboardComponent } from './viewer-dashboard/viewer-dashboard.component';
import { SettingsComponent } from './settings/settings.component';

const routes: Routes = [

  // 🔐 LOGIN
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  
  // 🔗 ACTIVATION & ONBOARDING
  { path: 'activate', component: ActivateComponent },
  { path: 'face-setup', component: FaceSetupComponent },
  
  // 📊 MANAGER DASHBOARD (Isolated Layout)
  { 
    path: 'dashboard/manager', 
    component: ManagerDashboardComponent, 
    canActivate: [AuthGuard],
    data: { role: 'ROLE_MANAGER' }
  },
  { 
    path: 'dashboard/viewer', 
    component: ViewerDashboardComponent, 
    canActivate: [AuthGuard],
    data: { role: 'ROLE_VIEWER' }
  },

  // 📊 ADMIN DASHBOARD (Admin Layout)
  {
    path: 'dashboard',
    component: AdminLayoutComponent,
    canActivate: [AuthGuard],
    data: { role: 'ROLE_ADMIN' },
    children: [

      { path: 'departments', component: DepartmentsComponent },
      { path: 'managers', component: ManagersComponent },
      { path: 'viewers', component: ViewersComponent },
      { path: 'cameras', component: CamerasComponent },
      { path: 'alerts', component: AlertsComponent },

      // 🔥 EMPLOYEES
      { path: 'employees', component: EmployeesComponent },

      // 🔥 FACE REGISTRATION
      { path: 'face-registration', component: FaceRegistrationComponent },

      // 🔥 SETTINGS & PROFILE
      { path: 'settings', component: SettingsComponent },
      { path: 'user-profile', component: UserProfileComponent },
      { path: 'analytics', component: AnalyticsComponent },

      // 🔥 DEFAULT PAGE (Set to the new Master Dashboard)
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // ❌ NOT FOUND
  { path: '**', redirectTo: 'login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { useHash: true })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
