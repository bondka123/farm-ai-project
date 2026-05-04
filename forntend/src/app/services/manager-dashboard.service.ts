import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Alert } from './alert.service';
import { Camera } from './camera.service';
import { Employee } from './employee.service';

export interface ManagerDepartment {
  id: number;
  name: string;
  doctors: number;
  electricians: number;
  workers: number;
  assignedEmployees: number;
}

export interface TeamSuggestion {
  [job: string]: Employee[];
}

@Injectable({
  providedIn: 'root'
})
export class ManagerDashboardService {

  private API = 'http://localhost:8081/api/manager';

  constructor(private http: HttpClient) {}

  getDepartment(): Observable<ManagerDepartment> {
    return this.http.get<ManagerDepartment>(`${this.API}/department`);
  }

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.API}/employees`);
  }

  getCameras(): Observable<Camera[]> {
    return this.http.get<Camera[]>(`${this.API}/cameras`);
  }

  getAlerts(): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.API}/alerts`);
  }

  suggestTeam(departmentId: number): Observable<TeamSuggestion> {
    return this.http.get<TeamSuggestion>(`${this.API}/suggest-team/${departmentId}`);
  }

  getTeamCandidates(): Observable<TeamSuggestion> {
    return this.http.get<TeamSuggestion>(`${this.API}/team-candidates`);
  }

  getAvailableEmployees(): Observable<TeamSuggestion> {
    return this.http.get<TeamSuggestion>(`${this.API}/available-employees`);
  }

  confirmTeam(departmentId: number, employees: number[]) {
    return this.http.post(`${this.API}/confirm-team`, { departmentId, employees });
  }
}
