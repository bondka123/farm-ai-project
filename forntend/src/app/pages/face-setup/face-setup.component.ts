import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-face-setup',
  templateUrl: './face-setup.component.html',
  styleUrls: ['./face-setup.component.scss']
})
export class FaceSetupComponent implements OnInit {

  loading = false;
  error = '';
  success = '';

  constructor(
    private router: Router,
    private userService: UserService
  ) { }

  ngOnInit(): void {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      this.error = 'No user ID found. Please login or activate from your email again.';
    }
  }

  scanFace(): void {
    this.loading = true;
    this.error = '';
    this.success = '';

    const userIdStr = localStorage.getItem('userId');
    if (!userIdStr) {
      this.error = 'No user ID found.';
      this.loading = false;
      return;
    }

    const userId = Number(userIdStr);

    this.userService.registerFace(userId).subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.error || res.status === 'error') {
          this.error = res.error || res.message || 'Erreur lors de l\'enregistrement du visage.';
          return;
        }

        this.success = 'Visage enregistre avec succes';
        localStorage.setItem('faceRegistered', 'true');
        if (res.role) {
          localStorage.setItem('role', res.role);
        }

        setTimeout(() => {
          const role = localStorage.getItem('role');
          if (role === 'ROLE_MANAGER') {
            this.router.navigate(['/dashboard/manager']);
          } else if (role === 'ROLE_ADMIN') {
            this.router.navigate(['/dashboard']);
          } else {
            this.router.navigate(['/login']);
          }
        }, 1500);
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.error || 'Erreur lors de l\'enregistrement du visage.';
      }
    });
  }

}
