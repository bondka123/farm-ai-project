import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Alert } from '../services/alert.service';
import { Camera } from '../services/camera.service';
import { Employee } from '../services/employee.service';
import {
  ManagerDashboardService,
  ManagerDepartment,
  TeamSuggestion
} from '../services/manager-dashboard.service';
import { TeamService } from '../services/team.service';

@Component({
  selector: 'app-manager-dashboard',
  templateUrl: './manager-dashboard.component.html',
  styleUrls: ['./manager-dashboard.component.scss']
})
export class ManagerDashboardComponent implements OnInit {

  private readonly defaultJobs = ['DOCTOR', 'ELECTRICIAN', 'WORKER'];

  department?: ManagerDepartment;
  employees: Employee[] = [];
  cameras: Camera[] = [];
  alerts: Alert[] = [];

  teamSuggestion: TeamSuggestion = {};
  teamCandidates: TeamSuggestion = {};
  availableEmployees: TeamSuggestion = {};
  pendingCount = 0;
  selectedTeam: TeamSuggestion = {};

  modifyMode = false;

  loading = true;
  teamLoading = false;
  isValidating = false;
  error = '';
  teamMessage = '';
  userRole = '';
  currentView: 'dashboard' | 'settings' = 'dashboard';

  private pollingSub?: any;

  constructor(
    private managerDashboardService: ManagerDashboardService,
    private teamService: TeamService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.userRole = localStorage.getItem('role') || '';
    this.loadManagerData();
    
    // 🔁 Poll for real-time status/alerts
    this.pollingSub = setInterval(() => {
      this.loadDepartmentData();
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.pollingSub) clearInterval(this.pollingSub);
  }

  switchView(view: 'dashboard' | 'settings'): void {
    this.currentView = view;
  }

  scrollTo(sectionId: string): void {
    this.currentView = 'dashboard';
    setTimeout(() => {
      const element = document.getElementById(sectionId);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
      }
    }, 100);
  }

  get suggestionJobs(): string[] {
    const jobs = new Set<string>(this.defaultJobs);
    Object.keys(this.selectedTeam).forEach(job => jobs.add(job));
    Object.keys(this.teamCandidates).forEach(job => jobs.add(job));
    return Array.from(jobs);
  }

  get hasSelectedEmployees(): boolean {
    return this.flattenSelectedEmployeeIds().length > 0;
  }

  loadManagerData(): void {
    this.loading = true;
    this.error = '';
    this.teamMessage = '';

    this.managerDashboardService.getDepartment().subscribe({
      next: (department) => {
        this.department = department;
        this.loadDepartmentData();
        
        // Only load suggestion if NO employees are assigned AND we aren't currently editing/selecting
        if (department.assignedEmployees === 0 
            && !this.modifyMode 
            && Object.keys(this.selectedTeam).length === 0) {
          this.loadTeamSuggestion(department.id);
        }
      },
      error: (err) => {
        this.loading = false;
        this.department = undefined;
        this.error = this.resolveError(err, "Impossible de charger le département.");
      }
    });
  }

  loadDepartmentData(): void {
    this.managerDashboardService.getEmployees().subscribe({
      next: (employees) => {
        this.employees = employees;
        this.loading = false;
      },
      error: (err) => {
        this.error = this.resolveError(err, 'Erreur employés');
        this.loading = false;
      }
    });

    this.managerDashboardService.getCameras().subscribe({
      next: (cameras) => this.cameras = cameras,
      error: (err) => this.error = this.resolveError(err, 'Erreur caméras')
    });

    this.managerDashboardService.getAlerts().subscribe({
      next: (alerts) => this.alerts = alerts.filter(a => !a.resolved),
      error: (err) => this.error = this.resolveError(err, 'Erreur alertes')
    });

    this.managerDashboardService.getTeamCandidates().subscribe({
      next: (c) => this.teamCandidates = c,
      error: () => this.teamCandidates = {}
    });

    this.managerDashboardService.getAvailableEmployees().subscribe({
      next: (res: any) => {
        this.availableEmployees = res.candidates || {};
        this.pendingCount = res.pendingCount || 0;
      },
      error: () => this.availableEmployees = {}
    });
  }

  getValidCandidates(job: string): Employee[] {
    return (this.availableEmployees[job] || []).filter(e => e.faceRegistered && e.status === 'APPROVED');
  }

  getInvalidCandidates(job: string): Employee[] {
    return (this.availableEmployees[job] || []).filter(e => !e.faceRegistered || e.status !== 'APPROVED');
  }

  isRequirementMet(job: string): boolean {
    return (this.selectedTeam[job] || []).length >= this.getRequiredCount(job);
  }

  getProgressClass(job: string): string {
    const count = (this.selectedTeam[job] || []).length;
    const required = this.getRequiredCount(job);
    if (count === 0 && required > 0) return 'progress-none';
    if (count < required) return 'progress-partial';
    return 'progress-complete';
  }

  loadTeamSuggestion(departmentId: number): void {

    this.teamLoading = true;
    this.teamMessage = '';

    this.teamService.getProposal(departmentId).subscribe({

      next: (suggestion) => {
        console.log("TEAM PROPOSAL:", suggestion);
        this.teamSuggestion = suggestion;
        this.selectedTeam = this.cloneSuggestion(suggestion);
        this.teamLoading = false;

        if (!this.hasSelectedEmployees) {
          this.teamMessage = "Aucune proposition trouvée.";
        }
      },

      error: (err) => {
        this.teamMessage = this.resolveError(err, 'Erreur suggestion');
        this.teamLoading = false;
      }
    });
  }

  // =========================
  // 🔥 TEAM ACTIONS
  // =========================
  acceptTeam(): void {
    if (!this.department) return;
    const ids = this.flattenSelectedEmployeeIds();
    
    if (ids.length === 0) {
      this.teamMessage = '⚠️ Veuillez sélectionner au moins un employé.';
      return;
    }

    this.teamLoading = true;
    this.teamService.validateTeam(this.department.id, ids).subscribe({
      next: () => {
        this.teamMessage = 'Équipe validée et assignée ✔';
        this.teamLoading = false;
        this.modifyMode = false;
        this.loadManagerData(); // This will now show the counts
      },
      error: (err) => {
        this.teamMessage = '❌ ' + (err.error?.message || 'Erreur validation. Certains employés ne sont peut-être pas approuvés.');
        this.teamLoading = false;
        console.error(err);
      }
    });
  }

  updateTeam(): void {
    if (!this.department) return;
    const ids = this.flattenSelectedEmployeeIds();
    
    if (ids.length === 0) {
      this.teamMessage = '⚠️ Veuillez sélectionner au moins un employé.';
      return;
    }

    this.teamLoading = true;
    this.teamService.updateTeam(this.department.id, ids).subscribe({
      next: () => {
        this.teamMessage = 'Affectation mise à jour ✔';
        this.teamLoading = false;
        this.modifyMode = false;
        this.loadManagerData();
      },
      error: (err) => {
        this.teamMessage = '❌ ' + (err.error?.message || 'Erreur lors de la mise à jour.');
        this.teamLoading = false;
        console.error(err);
      }
    });
  }

  // =========================
  // 🔥 MODIFY TEAM
  // =========================
  removeEmployee(job: string, id?: number): void {
    if (!id) return;
    this.selectedTeam[job] =
      (this.selectedTeam[job] || []).filter(e => e.id !== id);
  }

  addEmployee(job: string, value: string): void {

    const id = Number(value);
    if (!id) return;

    const emp = (this.teamCandidates[job] || []).find(e => e.id === id);
    if (!emp) return;

    const exists = (this.selectedTeam[job] || []).some(e => e.id === emp.id);

    if (!exists) {
      this.selectedTeam[job] = [...(this.selectedTeam[job] || []), emp];
      this.teamMessage = '';
    }
  }

  availableCandidates(job: string): Employee[] {

    const selectedIds = new Set((this.selectedTeam[job] || []).map(e => e.id));

    return this.getValidCandidates(job)
      .filter(e => !selectedIds.has(e.id));
  }

  // =========================
  // 🔥 UTIL
  // =========================
  logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  private flattenSelectedEmployeeIds(): number[] {
    return Object.values(this.selectedTeam)
      .flat()
      .map(e => e.id)
      .filter((id): id is number => id !== undefined);
  }

  getRequiredCount(job: string): number {
    if (!this.department) return 0;
    switch (job) {
      case 'DOCTOR': return this.department.doctors;
      case 'ELECTRICIAN': return this.department.electricians;
      case 'WORKER': return this.department.workers;
      default: return 0;
    }
  }

  private cloneSuggestion(s: TeamSuggestion): TeamSuggestion {
    return Object.keys(s).reduce((c, job) => {
      c[job] = [...s[job]];
      return c;
    }, {} as TeamSuggestion);
  }

  private resolveError(err: any, fallback: string): string {
    return err?.error?.message || fallback;
  }
}