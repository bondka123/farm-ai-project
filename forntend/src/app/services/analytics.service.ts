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
      attendance: this.http.get<any[]>(`${this.baseUrl}/attendance`),
      stats: this.http.get<any>(`${this.baseUrl}/ai/current-stats`)
    });
  }

  // Real-time WebSocket connection
  listenToAnalytics(): Observable<any> {
    return new Observable(observer => {
      // Use standard WebSocket for simplicity
      const socket = new WebSocket('ws://localhost:8081/ws/analytics');
      
      // Note: In a real STOMP setup, we'd use a STOMP library.
      // Here we provide a mock/simple implementation or expect the user to have stompjs.
      // For this task, I will implement a robust standard WebSocket fallback.
      
      socket.onmessage = (event) => {
        try {
          // If it's raw JSON (from our custom controller)
          const data = JSON.parse(event.data);
          observer.next(data);
        } catch (e) {
          console.warn("Received non-JSON message", event.data);
        }
      };

      socket.onerror = (err) => observer.error(err);
      socket.onclose = () => observer.complete();

      return () => socket.close();
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
