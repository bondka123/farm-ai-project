import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Camera {
  id?: number;
  name: string;
  type: string;
  source: string;
  status: string;
  location: string;
  lastImage?: string;
  linePosition?: number;
  fps?: number;
  protocol?: string;
  aiEnabled?: boolean;
  aiType?: string;
  department?: {
    id: number;
    name?: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class CameraService {

  API = 'http://localhost:8081/api/cameras';

  constructor(private http: HttpClient) { }

  getAll(): Observable<Camera[]> {
    return this.http.get<Camera[]>(this.API);
  }

  create(camera: Camera): Observable<Camera> {
    return this.http.post<Camera>(this.API, camera);
  }

  update(id: number, camera: Camera) {
    return this.http.put(`${this.API}/${id}`, camera);
  }

  delete(id: number) {
    return this.http.delete(`${this.API}/${id}`);
  }
}