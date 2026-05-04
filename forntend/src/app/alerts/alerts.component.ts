import { Component, OnInit } from '@angular/core';
import { AlertService, Alert } from '../services/alert.service';

@Component({
  selector: 'app-alerts',
  templateUrl: './alerts.component.html',
  styleUrls: ['./alerts.component.scss']
})
export class AlertsComponent implements OnInit {

  alerts: Alert[] = [];
  serverUrl = 'http://localhost:8081';
  private previousAlertIds = new Set<number>();

  constructor(private service: AlertService) {}

  ngOnInit(): void {
    this.load();

    // 🔥 refresh automatique
    setInterval(() => this.load(), 3000);
  }

  load(): void {
    this.service.getAll().subscribe({
      next: (res) => {
        // Only show unresolved alerts
        this.alerts = res.filter(a => !a.resolved);
        
        // Check for new alerts to play sound
        let hasNewAlert = false;
        const currentIds = new Set<number>();
        
        this.alerts.forEach(alert => {
            if (alert.id) {
                currentIds.add(alert.id);
                if (!this.previousAlertIds.has(alert.id)) {
                    if (alert.severity === 'CRITICAL' || alert.severity === 'HIGH') {
                        hasNewAlert = true;
                    }
                }
            }
        });
        
        if (hasNewAlert && this.previousAlertIds.size > 0) {
            this.playSound();
        }
        
        this.previousAlertIds = currentIds;
      },
      error: (err) => {
        console.error(err);
      }
    });
  }
  
  playSound(): void {
    try {
        const audio = new Audio('assets/notification.mp3');
        audio.play().catch(e => console.log('Audio play blocked by browser', e));
    } catch (e) {}
  }

  resolve(id?: number): void {
    if (!id) return;
    this.service.resolveAlert(id).subscribe({
      next: () => this.load(),
      error: (err) => console.error(err)
    });
  }
}
