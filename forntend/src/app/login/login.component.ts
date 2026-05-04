import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html'
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

        console.log("LOGIN RESPONSE:", res);

        // ✅ STOCKAGE
        localStorage.setItem('token', res.token);
        localStorage.setItem('role', res.role);
        localStorage.setItem('email', res.email);
        localStorage.setItem('faceRegistered', res.faceRegistered);
        localStorage.setItem('userId', res.userId);

        // =========================
        // 🔥 REDIRECTION PAR ROLE
        // =========================

        if (res.role === 'ROLE_ADMIN') {

          this.router.navigate(['/dashboard']);

        } else if (res.role === 'ROLE_MANAGER' || res.role === 'ROLE_VIEWER') {

          if (res.faceRegistered === false || res.faceRegistered === 'false') {
            this.router.navigate(['/face-setup']);
          } else {
            const path = res.role === 'ROLE_MANAGER' ? '/dashboard/manager' : '/dashboard/viewer';
            this.router.navigate([path]);
          }

        } else {
          this.router.navigate(['/login']);
        }
      },

      error: (err) => {

        this.loading = false;
        this.error = err.error?.message || 'Login failed ❌';

      }
    });
  }

  // =========================
  // 🤖 LOGIN PAR VISAGE
  // =========================
  faceLogin() {

    this.faceLoading = true;
    this.error = '';

    this.http.post<any>('http://localhost:8081/api/auth/face-login', {})
      .subscribe({

        next: (res) => {

          this.faceLoading = false;

          console.log("FACE LOGIN:", res);

          if (res.status === 'success') {

            const role = res.role || '';

            // ✅ STOCKAGE
            localStorage.setItem('token', res.token);
            localStorage.setItem('role', role);
            localStorage.setItem('email', res.email);
            localStorage.setItem('faceRegistered', 'true');

            // 🔥 REDIRECTION
            if (role === 'ROLE_ADMIN' || role === 'ADMIN') {
              this.router.navigate(['/dashboard']);
            } else if (role === 'ROLE_MANAGER' || role === 'MANAGER') {
              this.router.navigate(['/dashboard/manager']);
            } else if (role === 'ROLE_VIEWER' || role === 'VIEWER') {
              this.router.navigate(['/dashboard/viewer']);
            } else {
              this.router.navigate(['/login']);
            }

          } else {
            this.error = res.message || "Face non reconnue ❌";
          }
        },

        error: () => {
          this.faceLoading = false;
          this.error = "Erreur reconnaissance ❌";
        }
      });
  }
}