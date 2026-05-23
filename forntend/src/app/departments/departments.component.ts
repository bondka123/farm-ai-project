import { Component, OnInit } from '@angular/core';
import { DepartmentService } from '../services/department.service';
import { ManagerService } from '../services/manager.service';

@Component({
  selector: 'app-departments',
  templateUrl: './departments.component.html'
})
export class DepartmentsComponent implements OnInit {

  departments: any[] = [];
  managers: any[] = [];

  department: any = this.resetForm();

  editMode = false;
  editId: number | null = null;
  showModal = false;

  openModal() {
    this.editMode = false;
    this.department = this.resetForm();
    this.showModal = true;
    document.body.classList.add('modal-open');
  }

  closeModal() {
    this.showModal = false;
    document.body.classList.remove('modal-open');
    this.reset();
  }

  constructor(
    private service: DepartmentService,
    private managerService: ManagerService
  ) { }

  ngOnInit(): void {
    this.loadDepartments();
    this.loadManagers();
  }

  loadDepartments() {
    this.service.getDepartments().subscribe(res => {
      this.departments = res;
    });
  }

  loadManagers() {
    this.managerService.getManagers().subscribe(res => {
      this.managers = res;
    });
  }

  save() {

    if (!this.department.manager?.id) {
      alert('Select manager');
      return;
    }

    this.department.doctors = Number(this.department.doctors || 0);
    this.department.electricians = Number(this.department.electricians || 0);
    this.department.workers = Number(this.department.workers || 0);

    if (this.department.doctors < 0 || this.department.electricians < 0 || this.department.workers < 0) {
      alert("Les nombres d'employes doivent etre positifs");
      return;
    }

    if (this.department.startTime === '' || this.department.endTime === '') {
      alert("Veuillez saisir les horaires");
      return;
    }

    const payload = {
      ...this.department,
      startTime: this.department.startTime + ':00',
      endTime: this.department.endTime + ':00'
    };

    if (this.editMode) {
      this.service.update(this.editId!, payload).subscribe(() => {
        alert('Updated');
        this.showModal = false;
        document.body.classList.remove('modal-open');
        this.loadDepartments();
        this.reset();
      });
    } else {
      this.service.create(payload).subscribe(() => {
        alert('Added');
        this.showModal = false;
        document.body.classList.remove('modal-open');
        this.loadDepartments();
        this.reset();
      });
    }
  }

  edit(dep: any) {
    this.department = {
      ...dep,
      manager: { id: dep.manager?.id },
      startTime: dep.startTime?.substring(0, 5),
      endTime: dep.endTime?.substring(0, 5),
      doctors: Number(dep.doctors || 0),
      electricians: Number(dep.electricians || 0),
      workers: Number(dep.workers || 0)
    };

    this.editMode = true;
    this.editId = dep.id;
    this.showModal = true;
    document.body.classList.add('modal-open');
  }

  delete(id: number) {
    if (confirm('Delete department ?')) {
      this.service.delete(id).subscribe(() => {
        alert('Deleted');
        this.loadDepartments();
      });
    }
  }

  reset() {
    this.department = this.resetForm();
    this.editMode = false;
    this.editId = null;
    document.body.classList.remove('modal-open');
  }

  resetForm() {
    return {
      name: '',
      startTime: '',
      endTime: '',
      doctors: 0,
      electricians: 0,
      workers: 0,
      manager: { id: null }
    };
  }

  getRequiredTotal(dep: any = this.department): number {
    return Number(dep.doctors || 0) + Number(dep.electricians || 0) + Number(dep.workers || 0);
  }
}

