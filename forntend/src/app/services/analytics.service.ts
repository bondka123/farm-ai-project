import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {

  private baseUrl = 'http://localhost:8081/api';

  constructor(private http: HttpClient) { }

  getDashboardData(): Observable<any> {
    return forkJoin({
      employees: this.http.get<any[]>(`${this.baseUrl}/employees`),
      cameras: this.http.get<any[]>(`${this.baseUrl}/cameras`),
      alerts: this.http.get<any[]>(`${this.baseUrl}/alerts`),
      attendance: this.http.get<any[]>(`${this.baseUrl}/attendance`)
    });
  }

  // Specific analytics methods if needed
  getAlerts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/alerts`);
  }

  getAttendance(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/attendance`);
  }
}
