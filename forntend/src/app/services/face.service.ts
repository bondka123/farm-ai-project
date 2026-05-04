import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FaceService {
  private faceUrl = 'http://localhost:8081/api/face';
  private authUrl = 'http://localhost:8081/api/auth';
  private employeeUrl = 'http://localhost:8081/api/employees';

  constructor(private http: HttpClient) {}

  faceLogin(): Observable<any> {
    return this.http.post(`${this.authUrl}/face-login`, {});
  }

  registerFace(): Observable<any> {
    return this.http.post(`${this.faceUrl}/register`, {});
  }

  updateFace(): Observable<any> {
    return this.http.put(`${this.faceUrl}/update`, {});
  }

  deleteFace(): Observable<any> {
    return this.http.delete(`${this.faceUrl}/delete`);
  }

  getStatus(): Observable<{faceRegistered: boolean}> {
    return this.http.get<{faceRegistered: boolean}>(`${this.faceUrl}/status`);
  }

  // Admin/Viewer management methods
  registerFaceByEmployeeId(id: number): Observable<any> {
    return this.http.post(`${this.employeeUrl}/register-face/${id}`, {});
  }

  deleteFaceByEmployeeId(id: number): Observable<any> {
    return this.http.delete(`${this.employeeUrl}/delete-face/${id}`);
  }
}


