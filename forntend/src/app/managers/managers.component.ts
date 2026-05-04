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

  constructor(private service: ManagerService) {}

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
          this.afterSave();
        },
        error: (err) => {
          console.error(err);

          if (err.status === 403) {
            alert("ADMIN requis ❌");
          } else {
            alert("Erreur création ❌");
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
    this.loadManagers();
  }
}