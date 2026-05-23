import { Component, OnInit } from '@angular/core';
import { ManagerService, Manager } from '../services/manager.service';

@Component({
  selector: 'app-managers',
  templateUrl: './managers.component.html',
  styleUrls: ['./managers.component.scss']
})
export class ManagersComponent implements OnInit {

  managers: Manager[] = [];

  manager: Manager = this.resetForm();

  isEdit = false;
  loading = false;
  showModal = false;

  constructor(private service: ManagerService) {}

  openModal() {
    this.isEdit = false;
    this.manager = this.resetForm();
    this.showModal = true;
    document.body.classList.add('modal-open');
  }

  closeModal() {
    this.showModal = false;
    document.body.classList.remove('modal-open');
    this.afterSave();
  }

  ngOnInit(): void {
    this.loadManagers();
  }

  // =========================
  // 🔥 LOAD MANAGERS
  // =========================
  loadManagers() {
    this.loading = true;

    this.service.getManagers().subscribe({
      next: (res) => {
        this.managers = res;
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
  // 🔥 CREATE / UPDATE
  // =========================
  save() {

    if (!this.manager.firstName || !this.manager.email) {
      alert("Remplir les champs ⚠️");
      return;
    }

    // 🔥 UPDATE
    if (this.isEdit && this.manager.id) {

      this.service.update(this.manager.id, this.manager).subscribe({
        next: () => {
          alert("Modifié ✅");
          this.showModal = false;
          this.afterSave();
        },
        error: (err) => {
          console.error(err);
          alert("Erreur modification ❌");
        }
      });

    } else {

      // 🔥 CREATE
      this.service.create(this.manager).subscribe({
        next: () => {
          alert("Créé ✅");
          this.showModal = false;
          this.afterSave();
        },
        error: (err) => {
          console.error(err);
          const msg = err.error?.message || "Erreur création ❌";
          
          if (err.status === 403) {
            alert("ADMIN requis ❌");
          } else {
            alert(msg);
          }
        }
      });

    }
  }

  // =========================
  // 🔥 EDIT
  // =========================
  edit(m: Manager) {
    this.manager = { ...m };
    this.isEdit = true;
    this.showModal = true;
    document.body.classList.add('modal-open');
  }

  // =========================
  // 🔥 DELETE
  // =========================
  delete(id: number) {

    if (!confirm("Supprimer ce manager ?")) return;

    this.service.delete(id).subscribe({
      next: () => {
        alert("Supprimé ✅");
        this.loadManagers();
      },
      error: (err) => {
        console.error(err);
        alert("Erreur suppression ❌");
      }
    });
  }

  // =========================
  // 🔥 RESET FORM
  // =========================
  resetForm(): Manager {
    return {
      firstName: '',
      lastName: '',
      email: '',
      phone: ''
    };
  }

  // =========================
  // 🔥 AFTER SAVE
  // =========================
  afterSave() {
    this.manager = this.resetForm();
    this.isEdit = false;
    document.body.classList.remove('modal-open');
    this.loadManagers();
  }
}
