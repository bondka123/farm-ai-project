import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TeamService {

  private apiUrl = 'http://localhost:8081/api/team';

  constructor(private http: HttpClient) { }

  getProposal(departmentId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/proposal/${departmentId}`);
  }

  validateTeam(departmentId: number, employeeIds: number[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/validate`, { departmentId, employees: employeeIds }, { responseType: 'text' });
  }

  updateTeam(departmentId: number, employeeIds: number[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/update`, { departmentId, employees: employeeIds }, { responseType: 'text' });
  }
}
