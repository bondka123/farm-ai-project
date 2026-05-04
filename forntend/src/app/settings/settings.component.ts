import { Component, OnInit } from '@angular/core';
import { UserService, User } from '../services/user.service';
import { FaceService } from '../services/face.service';

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
  faceLoading = false;
  faceRegistered = false;
  message = '';
  isError = false;
  cameraStatus = '';

  constructor(
    private userService: UserService,
    private faceService: FaceService
  ) { }

  ngOnInit(): void {
    this.user.email = localStorage.getItem('email') || '';
    this.fetchProfile();
    this.checkFaceStatus();
  }

  fetchProfile() {
    this.userService.getProfile().subscribe({
      next: (data) => this.user = data,
      error: () => console.error("Erreur chargement profil")
    });
  }

  checkFaceStatus() {
    this.faceService.getStatus().subscribe({
      next: (res) => this.faceRegistered = res.faceRegistered,
      error: () => console.error("Erreur status visage")
    });
  }

  onRegisterFace() {
    this.startFaceProcess('register');
  }

  onUpdateFace() {
    this.startFaceProcess('update');
  }

  onDeleteFace() {
    if (!confirm('Supprimer votre visage enregistré ?')) return;
    this.loading = true;
    this.faceService.deleteFace().subscribe({
      next: () => {
        this.loading = false;
        this.faceRegistered = false;
        this.showMessage('Visage supprimé avec succès ✅', false);
      },
      error: (err) => {
        this.loading = false;
        this.showMessage('Erreur lors de la suppression ❌', true);
      }
    });
  }

  private startFaceProcess(type: 'register' | 'update') {
    this.faceLoading = true;
    this.cameraStatus = 'Initialisation de la caméra... Regardez l\'objectif 📷';
    
    const obs = type === 'register' ? this.faceService.registerFace() : this.faceService.updateFace();
    
    obs.subscribe({
      next: (res) => {
        this.faceLoading = false;
        this.faceRegistered = true;
        this.showMessage(type === 'register' ? 'Visage enregistré ! ✅' : 'Visage mis à jour ! ✅', false);
      },
      error: (err) => {
        this.faceLoading = false;
        const msg = err.error?.error || 'Erreur caméra ou détection ❌';
        this.showMessage(msg, true);
      }
    });
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

