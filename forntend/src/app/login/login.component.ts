import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {

  email: string = '';
  password: string = '';

  loading = false;
  faceLoading = false;

  error = '';
  success = '';

  constructor(private http: HttpClient, private router: Router) {}

  // =========================
  // 🔐 LOGIN NORMAL
  // =========================
  login() {
    this.loading = true;
    this.error = '';

    this.http.post<any>('http://localhost:8081/api/auth/login', {
      email: this.email,
      password: this.password
    }).subscribe({
      next: (res) => {
        this.loading = false;
        console.log("--- Standard Login Success ---");
        console.log("User:", res.email);
        
        this.saveSession(res);
        this.handleRedirection(res.role, res.faceRegistered);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Login failed ❌';
        console.error("Login Error:", this.error);
      }
    });
  }

  // =========================
  // 🤖 LOGIN PAR VISAGE
  // =========================
  faceLogin() {
    this.faceLoading = true;
    this.error = '';

    console.log("--- Starting Biometric Authentication ---");

    this.http.post<any>('http://localhost:8081/api/auth/face-login', {})
      .subscribe({
        next: (res) => {
          this.faceLoading = false;

          if (res.status === 'success' && res.token && res.role) {
            console.log("--- Face Recognition Success ---");
            console.log("Matched User:", res.email);
            console.log("Confidence Score:", res.confidence);
            
            this.saveSession(res);
            this.handleRedirection(res.role, res.faceRegistered);
          } else {
            console.warn("Face Recognition Denied:", res.message);
            this.error = res.message || "Biometric authentication failed ❌";
          }
        },
        error: (err) => {
          this.faceLoading = false;
          this.error = "Biometric service unreachable ❌";
          console.error("Face Login Error:", err);
        }
      });
  }

  // =========================
  // 🛡️ SECURITY UTILS
  // =========================
  private saveSession(res: any) {
    localStorage.setItem('token', res.token);
    localStorage.setItem('role', res.role);
    localStorage.setItem('email', res.email);
    localStorage.setItem('faceRegistered', String(res.faceRegistered));
    localStorage.setItem('userId', res.userId);
  }

  private normalizeRole(role: string): string {
    if (!role) return '';
    const r = role.toUpperCase();
    if (r.includes('ADMIN')) return 'ROLE_ADMIN';
    if (r.includes('MANAGER')) return 'ROLE_MANAGER';
    if (r.includes('VIEWER') || r.includes('OBSERVER')) return 'ROLE_VIEWER';
    return r;
  }

  private handleRedirection(role: string, faceRegistered: any) {
    const normalizedRole = this.normalizeRole(role);
    console.log("Detected Role:", role);
    console.log("Normalized Role:", normalizedRole);

    if (normalizedRole === 'ROLE_ADMIN') {
      console.log("Redirecting to: /dashboard (Admin)");
      this.router.navigate(['/dashboard']);
    } else if (normalizedRole === 'ROLE_MANAGER' || normalizedRole === 'ROLE_VIEWER') {
      
      // Check if face is registered for staff
      const isFaceOk = faceRegistered === true || faceRegistered === 'true';
      
      if (!isFaceOk) {
        console.warn("Face registration missing for staff. Redirecting to setup.");
        this.router.navigate(['/face-setup']);
      } else {
        const path = normalizedRole === 'ROLE_MANAGER' ? '/dashboard/manager' : '/dashboard/viewer';
        console.log(`Redirecting to: ${path}`);
        this.router.navigate([path]);
      }
    } else {
      console.error("Critical Security Alert: Unauthorized role detected:", role);
      this.error = "Unauthorized access level ❌";
      localStorage.clear();
      this.router.navigate(['/login']);
    }
  }
}