import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Alert {
  id?: number;
  type: string;
  message: string;
  severity: string;
  cameraId?: number;
  departmentId?: number;
  departmentName?: string; // 🔥 AJOUT IMPORTANT
  timestamp: string;

  // AI Alert Fields
  resolved?: boolean;
  location?: string;
  imagePath?: string;
  uniqueHash?: string;
  employeeId?: number;
  count?: number;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {

  private API = 'http://localhost:8081/api/alerts';

  constructor(private http: HttpClient) { }

  getAll(): Observable<Alert[]> {
    return this.http.get<Alert[]>(this.API);
  }

  // 🔥 RESOLVE ALERT
  resolveAlert(id: number): Observable<Alert> {
    return this.http.put<Alert>(`${this.API}/${id}/resolve`, {});
  }
}