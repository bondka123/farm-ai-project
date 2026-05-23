import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, OnDestroy {

  // ==================
  // BASE URL
  // ==================
  private API = 'http://localhost:8081/api';
  private refreshInterval: any;

  // ==================
  // DATA
  // ==================
  departments: any[] = [];
  cameras: any[] = [];
  statuses: any[] = [];
  alerts: any[] = [];

  // ==================
  // STATS CARDS
  // ==================
  totalDepartments = 0;
  totalCameras = 0;
  totalAlerts = 0;
  activeCameras = 0;

  // ==================
  // LOADING
  // ==================
  loading = true;

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.loadAll();
    // Auto-refresh every 10 seconds
    this.refreshInterval = setInterval(() => this.loadAll(), 10000);
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }

  getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  loadAll(): void {
    this.loadDepartments();
    this.loadCameras();
    this.loadStatuses();
  }

  loadDepartments(): void {
    this.http.get<any[]>(`${this.API}/departments/public`).subscribe({
      next: (data) => {
        this.departments = data;
        this.totalDepartments = data.length;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  loadCameras(): void {
    const headers = this.getHeaders();
    this.http.get<any[]>(`${this.API}/cameras`, { headers }).subscribe({
      next: (data) => {
        this.cameras = data;
        this.totalCameras = data.length;
        this.activeCameras = data.filter(c => c.status === 'ACTIVE').length;
      },
      error: () => { }
    });
  }

  loadStatuses(): void {
    this.http.get<any[]>(`${this.API}/department-status`).subscribe({
      next: (data) => {
        this.statuses = data.slice(-20).reverse(); // last 20, newest first
        this.alerts = data.filter(s => s.status === 'alert');
        this.totalAlerts = this.alerts.length;
      },
      error: () => { }
    });
  }

  getStatusClass(status: string): string {
    return status === 'valid' ? 'badge-success' : 'badge-danger';
  }

  getStatusIcon(status: string): string {
    return status === 'valid' ? 'check_circle' : 'warning';
  }

  getCameraStatusClass(status: string): string {
    return status === 'ACTIVE' ? 'camera-online' : 'camera-offline';
  }

  formatTime(timestamp: string): string {
    if (!timestamp) return '';
    return new Date(timestamp).toLocaleTimeString('fr-FR');
  }

  formatDate(timestamp: string): string {
    if (!timestamp) return '';
    return new Date(timestamp).toLocaleDateString('fr-FR');
  }

  getDeptName(deptId: number): string {
    const dept = this.departments.find(d => d.id === deptId);
    return dept ? dept.name : `Dept #${deptId}`;
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    window.location.href = '/login';
  }
}
