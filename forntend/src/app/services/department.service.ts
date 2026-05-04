import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class DepartmentService {

  API = 'http://localhost:8081/api/departments';

  constructor(private http: HttpClient) {}

  getDepartments() {
    return this.http.get<any[]>(this.API);
  }

  create(dep: any) {
    return this.http.post(this.API, dep);
  }

  update(id: number, dep: any) {
    return this.http.put(`${this.API}/${id}`, dep);
  }

  delete(id: number) {
    return this.http.delete(`${this.API}/${id}`);
  }
}