import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

// ✅ INTERFACE OBLIGATOIRE
export interface Employee {
  id?: number;
  name: string;
  email?: string;
  phone?: string;
  job: string;
  status: string;
  faceRegistered: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {

  API = 'http://localhost:8081/api/employees';

  constructor(private http: HttpClient) {}

  // ✅ GET ALL
  getAll(): Observable<Employee[]> {
    return this.http.get<Employee[]>(this.API);
  }

  getMyProfile(email: string): Observable<Employee> {
    return this.http.get<Employee>(`${this.API}/me?email=${email}`);
  }

  // ✅ GET ONLY EMPLOYEES
  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.API}/only-employees`);
  }

  // ✅ GET WITHOUT FACE
  getWithoutFace(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.API}/no-face`);
  }

  // ✅ CREATE
  create(emp: Employee) {
    return this.http.post(`${this.API}/employee`, emp);
  }

  // ✅ DELETE
  delete(id: number) {
    return this.http.delete(`${this.API}/${id}`);
  }

  registerFace(id: number) {
    return this.http.post(`${this.API}/register-face/${id}`, {});
  }

  deleteFace(id: number) {
    return this.http.delete(`${this.API}/delete-face/${id}`);
  }

  // ✅ VALIDATE FACE
  validateFace(id: number) {
    return this.http.put(`${this.API}/validate-face/${id}`, {});
  }
}