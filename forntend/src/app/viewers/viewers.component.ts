import { Component, OnInit } from '@angular/core';
import { UserService, User } from '../services/user.service';

@Component({
  selector: 'app-viewers',
  templateUrl: './viewers.component.html'
})
export class ViewersComponent implements OnInit {

  viewers: User[] = [];
  viewer: User = this.resetForm();

  editMode = false;
  editId: number | null = null;
  loading = false;
  showModal = false;

  openModal() {
    this.editMode = false;
    this.viewer = this.resetForm();
    this.showModal = true;
    document.body.classList.add('modal-open');
  }

  closeModal() {
    this.showModal = false;
    document.body.classList.remove('modal-open');
    this.reset();
  }

  constructor(private service: UserService) {}

  ngOnInit(): void {
    this.load();
  }

  // =========================
  // LOAD
  // =========================
  load() {
    this.loading = true;

    this.service.getViewers().subscribe({
      next: (res) => {
        this.viewers = res;
        this.loading = false;
      },
      error: (err) => {
        console.error(err);
        alert("Erreur chargement ❌");
        this.loading = false;
      }
    });
  }

  // =========================
  // SAVE (CREATE + UPDATE)
  // =========================
  save() {

    if (!this.viewer.email) {
      alert("Email obligatoire ❌");
      return;
    }

    // =====================
    // UPDATE
    // =====================
    if (this.editMode && this.editId !== null) {

      this.service.update(this.editId, this.viewer).subscribe({
        next: () => {

          const index = this.viewers.findIndex(v => v.id === this.editId);

          if (index !== -1) {
            this.viewers[index] = {
              ...this.viewer,
              id: this.editId
            };
          }

          alert("Viewer mis à jour ✅");
          this.showModal = false;
          document.body.classList.remove('modal-open');
          this.reset();
        },
        error: (err) => {
          console.error(err);
          alert("Erreur update ❌");
        }
      });

    } else {

      // =====================
      // CREATE
      // =====================
      this.service.createViewer(this.viewer).subscribe({
        next: (newViewer: User) => {

          // 🔥 AJOUT DIRECT UI (avec ID backend)
          this.viewers.unshift(newViewer);

          alert("Viewer créé + email envoyé ✅");
          this.showModal = false;
          document.body.classList.remove('modal-open');
          this.reset();
        },
        error: (err) => {

          console.error(err);

          if (err.status === 400 || err.status === 500) {
            alert("Email déjà utilisé ❌");
          } else if (err.status === 403) {
            alert("Accès refusé (token ou rôle) ❌");
          } else {
            alert("Erreur création ❌");
          }
        }
      });

    }
  }

  // =========================
  // EDIT
  // =========================
  edit(v: User) {
    this.viewer = { ...v };
    this.editMode = true;
    this.editId = v.id!;
    this.showModal = true;
    document.body.classList.add('modal-open');
  }

  // =========================
  // DELETE
  // =========================
  delete(id: number) {

    if (!id) return;

    if (confirm("Supprimer ce viewer ?")) {

      this.service.delete(id).subscribe({
        next: () => {

          // 🔥 suppression immédiate UI
          this.viewers = this.viewers.filter(v => v.id !== id);

          alert("Viewer supprimé ✅");
        },
        error: (err) => {
          console.error(err);

          if (err.status === 403) {
            alert("Accès refusé ❌");
          } else {
            alert("Erreur suppression ❌");
          }
        }
      });

    }
  }

  // =========================
  // RESET
  // =========================
  reset() {
    this.viewer = this.resetForm();
    this.editMode = false;
    this.editId = null;
    document.body.classList.remove('modal-open');
  }

  // =========================
  // FORM INIT
  // =========================
  resetForm(): User {
    return {
      firstName: '',
      lastName: '',
      email: '',
      phone: ''
    };
  }
}