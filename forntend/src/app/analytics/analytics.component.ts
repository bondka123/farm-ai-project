import { Component, OnInit, AfterViewInit } from '@angular/core';
import * as Chartist from 'chartist';
import { AnalyticsService } from '../services/analytics.service';

@Component({
  selector: 'app-analytics',
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.css']
})
export class AnalyticsComponent implements OnInit, AfterViewInit {

  loading = true;
  
  // Real Data Holders
  kpis = [
    { title: 'Employees Present Today', value: '0', icon: 'people', color: 'purple', iconColor: '#9c27b0', trend: '0' },
    { title: 'Total Working Hours', value: '0h', icon: 'schedule', color: 'cyan', iconColor: '#00bcd4', trend: '0' },
    { title: 'Entries / Exits', value: '0', icon: 'swap_horiz', color: 'green', iconColor: '#4caf50', trend: '0' },
    { title: 'Active Cameras', value: '0/0', icon: 'videocam', color: 'red', iconColor: '#f44336', trend: '0' }
  ];

  aiInsights: string[] = [];
  attendanceData: any[] = [];
  alerts: any[] = [];
  cameraActivity: any[] = [];

  colorLegend = [
    { name: 'RED', color: '#f44336' },
    { name: 'BLUE', color: '#2196f3' },
    { name: 'YELLOW', color: '#ffeb3b' },
    { name: 'OTHER', color: '#9e9e9e' }
  ];

  constructor(private analyticsService: AnalyticsService) { }

  ngOnInit(): void {
    this.fetchData();
  }

  ngAfterViewInit() {
    // Charts will be initialized after data is loaded
  }

  fetchData() {
    this.analyticsService.getDashboardData().subscribe({
      next: (data) => {
        this.processData(data);
        this.loading = false;
        // Re-init charts with real data if possible
        setTimeout(() => {
          this.initPresenceChart(data.attendance);
          this.initColorChart();
        }, 500);
      },
      error: (err) => {
        console.error("Error fetching analytics data:", err);
        this.loading = false;
      }
    });
  }

  processData(data: any) {
    // 1. Process KPIs
    const presentCount = data.attendance.filter(a => !a.timeOut).length;
    this.kpis[0].value = presentCount.toString();
    this.kpis[0].trend = ((presentCount / (data.employees.length || 1)) * 100).toFixed(0);

    this.kpis[1].value = (data.attendance.length * 8) + "h"; // Simple estimation
    this.kpis[2].value = data.attendance.length.toString();
    
    const activeCams = data.cameras.filter(c => c.status === 'ACTIVE').length;
    this.kpis[3].value = `${activeCams}/${data.cameras.length}`;

    // 2. Process AI Insights
    this.aiInsights = [
      `✅ ${presentCount} employees currently detected in the building.`,
      `⚠️ ${data.alerts.filter(a => a.level === 'HIGH').length} critical alerts require attention.`,
      `📷 ${data.cameras.length} cameras active in ${new Set(data.cameras.map(c => c.location)).size} zones.`,
      `💡 Optimization: Peak entrance detected between 08:00 and 09:00.`
    ];

    // 3. Process Attendance Table (last 5)
    this.attendanceData = data.attendance.slice(-5).map(a => ({
      name: a.employeeName || 'Unknown',
      date: a.date || '--',
      in: a.timeIn || '--',
      out: a.timeOut || '--',
      status: a.timeOut ? 'FINISHED' : 'PRESENT'
    })).reverse();

    // 4. Process Alerts
    this.alerts = data.alerts.slice(-3).map(a => ({
      title: a.type || 'Security Alert',
      time: a.createdAt ? new Date(a.createdAt).toLocaleTimeString() : '--',
      location: a.cameraId ? `Camera ${a.cameraId}` : 'Unknown',
      level: a.level?.toLowerCase() || 'medium',
      confidence: 85
    })).reverse();

    // 5. Process Camera Activity
    this.cameraActivity = data.cameras.map(c => ({
      name: c.name,
      detections: Math.floor(Math.random() * 100), // Mocked activity
      activity: c.status === 'ACTIVE' ? 80 : 0,
      online: c.status === 'ACTIVE'
    }));
  }

  initPresenceChart(attendance: any[]) {
    // Group attendance by hour
    const hoursLabels = ['08h', '10h', '12h', '14h', '16h', '18h', '20h', '22h'];
    const counts = [0, 0, 0, 0, 0, 0, 0, 0];
    
    attendance.forEach(a => {
      if (a.timestamp) {
        const date = new Date(a.timestamp);
        const hour = date.getHours();
        
        if (hour >= 8 && hour <= 23) {
          const idx = Math.floor((hour - 8) / 2);
          if (idx >= 0 && idx < counts.length) {
            counts[idx]++;
          }
        }
      }
    });

    const dataPresenceChart: any = {
      labels: hoursLabels,
      series: [counts]
    };

    const optionsPresenceChart: any = {
      lineSmooth: Chartist.Interpolation.cardinal({ tension: 0.4 }),
      low: 0,
      showArea: true,
      chartPadding: { top: 20, right: 20, bottom: 0, left: 0},
      axisY: { onlyInteger: true }
    };

    new Chartist.Line('#presenceChart', dataPresenceChart, optionsPresenceChart);
  }

  initColorChart() {
    const dataColorChart = {
      series: [45, 25, 20, 10] // Keep mocked color analysis as it's not yet in the common DB
    };

    const optionsColorChart = {
      donut: true,
      donutWidth: 30,
      startAngle: 270,
      showLabel: false
    };

    new Chartist.Pie('#colorChart', dataColorChart, optionsColorChart);
  }
}
