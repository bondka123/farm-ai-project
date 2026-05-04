import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-activate',
  templateUrl: './activate.component.html',
  styleUrls: ['./activate.component.scss']
})
export class ActivateComponent implements OnInit {

  loading = true;
  error = '';
  success = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService
  ) { }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.error = 'Invalid or missing activation token.';
      this.loading = false;
      return;
    }

    this.userService.activate(token).subscribe({
      next: (res: any) => {
        this.success = 'Activation successful!';
        this.loading = false;
        
        // Save userId for face registration
        if (res.userId) {
          localStorage.setItem('userId', String(res.userId));
        }
        if (res.role) {
          localStorage.setItem('role', res.role);
        }
        if (res.token) {
          localStorage.setItem('token', res.token);
        }
        if (res.faceRegistered !== undefined) {
          localStorage.setItem('faceRegistered', String(res.faceRegistered));
        }

        setTimeout(() => {
          this.router.navigate(['/face-setup']);
        }, 1500);
      },
      error: (err) => {
        this.error = err.error?.error || 'Failed to activate account.';
        this.loading = false;
      }
    });
  }

}
