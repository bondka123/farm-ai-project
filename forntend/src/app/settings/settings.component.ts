import { Component, OnInit } from '@angular/core';
import { UserService, User } from '../services/user.service';

@Component({
  selector: 'app-settings',
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {

  user: User = {
    firstName: '',
    lastName: '',
    email: '',
    phone: ''
  };

  passwords = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  };

  loading = false;
  message = '';
  isError = false;

  constructor(private userService: UserService) { }

  ngOnInit(): void {
    // We could fetch the current user profile here
    // For now, we assume we can get basic info from localStorage or a new /me endpoint
    // Let's use the local storage if available or a mock for now
    this.user.email = localStorage.getItem('email') || '';
  }

  updateProfile() {
    this.loading = true;
    this.userService.updateProfile(this.user).subscribe({
      next: () => {
        this.loading = false;
        this.showMessage('Profil mis à jour avec succès ✅', false);
      },
      error: () => {
        this.loading = false;
        this.showMessage('Erreur lors de la mise à jour ❌', true);
      }
    });
  }

  changePassword() {
    if (this.passwords.newPassword !== this.passwords.confirmPassword) {
      this.showMessage('Les mots de passe ne correspondent pas ❌', true);
      return;
    }

    this.loading = true;
    this.userService.changePassword(this.passwords).subscribe({
      next: () => {
        this.loading = false;
        this.showMessage('Mot de passe mis à jour ✅', false);
        this.passwords = { oldPassword: '', newPassword: '', confirmPassword: '' };
      },
      error: () => {
        this.loading = false;
        this.showMessage('Erreur lors de la mise à jour du mot de passe ❌', true);
      }
    });
  }

  private showMessage(msg: string, isError: boolean) {
    this.message = msg;
    this.isError = isError;
    setTimeout(() => this.message = '', 3000);
  }
}
