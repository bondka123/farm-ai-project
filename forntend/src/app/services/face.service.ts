import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FaceService {
  private authUrl = 'http://localhost:8081/api/auth';
  private employeeUrl = 'http://localhost:8081/api/employees';

  constructor(private http: HttpClient) {}

  faceLogin(): Observable<any> {
    return this.http.post(`${this.authUrl}/face-login`, {});
  }

  registerFace(employeeId: number): Observable<any> {
    return this.http.post(`${this.employeeUrl}/register-face/${employeeId}`, {});
  }

  deleteFace(employeeId: number): Observable<any> {
    return this.http.delete(`${this.employeeUrl}/delete-face/${employeeId}`);
  }
}
