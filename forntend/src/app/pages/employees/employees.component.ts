import { Component, OnInit } from '@angular/core';
import { EmployeeService, Employee } from '../../services/employee.service';

@Component({
  selector: 'app-employees',
  templateUrl: './employees.component.html',
  styleUrls: ['./employees.component.scss']
})
export class EmployeesComponent implements OnInit {

  // ── Data ──────────────────────────────────────────────────────────────────
  employees: Employee[] = [];
  jobs = [
    { value: 'DOCTOR', label: 'Docteur' },
    { value: 'ELECTRICIAN', label: 'Electricien' },
    { value: 'WORKER', label: 'Ouvrier' }
  ];

  // ── Form ──────────────────────────────────────────────────────────────────
  form: any = { name: '', email: '', phone: '', job: '', department: null };
  submitted = false;
  showModal = false;
  isEditMode = false;
  editId: number | null = null;

  openModal() {
    this.form = { name: '', email: '', phone: '', job: '', department: null };
    this.submitted = false;
    this.isEditMode = false;
    this.editId = null;
    this.showModal = true;
    document.body.classList.add('modal-open');
  }

  closeModal() {
    this.showModal = false;
    document.body.classList.remove('modal-open');
  }

  edit(emp: Employee) {
    this.isEditMode = true;
    this.editId = emp.id || null;
    this.form = {
      name: emp.name,
      email: emp.email || '',
      phone: emp.phone || '',
      job: emp.job,
      department: null
    };
    this.submitted = false;
    this.showModal = true;
    document.body.classList.add('modal-open');
  }

  // ── Approval Modal ────────────────────────────────────────────────────────
  showConfirmModal = false;
  employeeToApprove: Employee | null = null;

  promptApprove(emp: Employee) {
    this.employeeToApprove = emp;
    this.showConfirmModal = true;
    document.body.classList.add('modal-open');
  }

  cancelApprove() {
    this.showConfirmModal = false;
    this.employeeToApprove = null;
    if (!this.showModal) {
      document.body.classList.remove('modal-open');
    }
  }

  approveEmployee() {
    if (!this.employeeToApprove || !this.employeeToApprove.id) return;

    this.service.approve(this.employeeToApprove.id).subscribe({
      next: () => {
        this.toast('Employé approuvé avec succès ✅', 'success');
        this.load();
        this.cancelApprove();
      },
      error: () => this.toast("Erreur lors de l'approbation.", 'error')
    });
  }

  // ── Toast ─────────────────────────────────────────────────────────────────
  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  // ── Dark mode ─────────────────────────────────────────────────────────────
  isDarkMode = true;

  // ── Email regex ───────────────────────────────────────────────────────────
  private readonly emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  constructor(private service: EmployeeService) {}

  ngOnInit(): void {
    this.applyTheme();
    this.load();
  }

  // ── Load employees ────────────────────────────────────────────────────────
  load() {
    this.service.getEmployees().subscribe({
      next: res => this.employees = res,
      error: () => this.toast('Erreur lors du chargement.', 'error')
    });
  }

  // ── Validation helpers ────────────────────────────────────────────────────
  get nameValid(): boolean {
    return this.form.name?.trim().length > 0;
  }

  get emailValid(): boolean {
    if (!this.form.email) return true; // optional
    return this.emailRegex.test(this.form.email);
  }

  get formValid(): boolean {
    return this.nameValid && this.emailValid && !!this.form.job;
  }

  // ── Create employee ───────────────────────────────────────────────────────
  save() {
    this.submitted = true;
    if (!this.formValid) return;

    // ✅ DO NOT send status — backend controls it (always PENDING on creation)
    const payload: Employee = {
      name: this.form.name.trim(),
      email: this.form.email || '',
      phone: this.form.phone || '',
      job: this.form.job,
      status: 'PENDING',
      faceRegistered: false
    };

    if (this.isEditMode && this.editId) {
      this.service.update(this.editId, payload).subscribe({
        next: () => {
          this.toast('Employé modifié avec succès ✅', 'success');
          this.load();
          this.closeModal();
        },
        error: () => this.toast("Erreur lors de la modification.", 'error')
      });
    } else {
      this.service.create(payload).subscribe({
        next: () => {
          this.toast('Employé ajouté avec succès ✅', 'success');
          this.load();
          this.closeModal();
        },
        error: () => this.toast("Erreur lors de l'ajout.", 'error')
      });
    }
  }

  // ── Delete employee ───────────────────────────────────────────────────────
  delete(id?: number) {
    if (!id) return;
    if (!confirm('Supprimer cet employé ?')) return;

    this.service.delete(id).subscribe({
      next: () => {
        this.toast('Employé supprimé.', 'success');
        this.load();
      },
      error: () => this.toast('Erreur lors de la suppression.', 'error')
    });
  }

  // ── Validate face (VIEWER action) ─────────────────────────────────────────
  validateFace(id?: number) {
    if (!id) return;

    this.service.validateFace(id).subscribe({
      next: () => {
        this.toast('Visage validé — statut APPROVED ✅', 'success');
        this.load();
      },
      error: () => this.toast('Erreur lors de la validation.', 'error')
    });
  }

  // ── Dark mode toggle ──────────────────────────────────────────────────────
  toggleDarkMode() {
    this.isDarkMode = !this.isDarkMode;
    this.applyTheme();
  }

  private applyTheme() {
    if (this.isDarkMode) {
      document.body.classList.remove('light-mode');
    } else {
      document.body.classList.add('light-mode');
    }
  }

  // ── Toast helper ──────────────────────────────────────────────────────────
  private toast(message: string, type: 'success' | 'error') {
    clearTimeout(this.toastTimer);
    this.toastMessage = message;
    this.toastType = type;
    this.showToast = true;
    this.toastTimer = setTimeout(() => this.showToast = false, 3000);
  }
}





