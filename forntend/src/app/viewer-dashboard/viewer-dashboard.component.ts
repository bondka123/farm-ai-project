import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { EmployeeService, Employee } from '../services/employee.service';
import { DepartmentService } from '../services/department.service';

@Component({
  selector: 'app-viewer-dashboard',
  templateUrl: './viewer-dashboard.component.html'
})
export class ViewerDashboardComponent implements OnInit {

  employees: Employee[] = [];
  departments: any[] = [];
  loading: boolean = false;
  searchQuery: string = '';
  currentView: 'dashboard' | 'settings' = 'dashboard';

  constructor(
    private http: HttpClient,
    private router: Router,
    private employeeService: EmployeeService,
    private departmentService: DepartmentService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData() {
    this.loading = true;
    
    // Load Employees
    this.employeeService.getAll().subscribe({
      next: (data) => {
        this.employees = data;
        this.loading = false;
      },
      error: (err) => {
        console.error("Error loading employees", err);
        this.loading = false;
      }
    });

    // Load Departments
    this.departmentService.getDepartments().subscribe({
      next: (data) => {
        this.departments = data;
      },
      error: (err) => console.error("Error loading departments", err)
    });
  }

  filteredEmployees() {
    if (!this.searchQuery) return this.employees;
    const query = this.searchQuery.toLowerCase();
    return this.employees.filter(e => 
      e.name.toLowerCase().includes(query) || 
      (e.email && e.email.toLowerCase().includes(query)) ||
      e.job.toLowerCase().includes(query)
    );
  }

  getRegisteredCount() {
    return this.employees.filter(e => e.faceRegistered).length;
  }

  getUnregisteredCount() {
    return this.employees.filter(e => !e.faceRegistered).length;
  }

  captureFace(employee: Employee) {
    if (!employee.id) return;
    
    // Launch registration via backend
    this.http.post(`http://localhost:8081/api/employees/register-face/${employee.id}`, {})
      .subscribe({
        next: (res: any) => {
          console.log("Registration started", res);
          alert(`Démarrage de la capture pour ${employee.name}. Veuillez regarder la fenêtre de la caméra sur le serveur.`);
        },
        error: (err) => {
          console.error("Error starting capture", err);
          alert("Erreur lors du démarrage de la capture. Vérifiez la connexion avec le script Python.");
        }
      });
  }

  logout() {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  switchView(view: 'dashboard' | 'settings') {
    this.currentView = view;
  }

  scrollTo(id: string) {
    this.currentView = 'dashboard';
    setTimeout(() => {
      const el = document.getElementById(id);
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    }, 100);
  }
}
