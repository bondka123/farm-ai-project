import { Component, OnInit, AfterViewInit } from '@angular/core';
import * as Chartist from 'chartist';
import { AnalyticsService } from '../services/analytics.service';

@Component({
  selector: 'app-analytics',
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.scss']
})
export class AnalyticsComponent implements OnInit, AfterViewInit {

  loading = true;
  private analyticsSub?: any;

  // Real Data Holders
  kpis = [
    { id: 'employees', title: 'Employees Present Today', value: '0', icon: 'people', color: 'purple', iconColor: '#9c27b0', trend: 'LIVE UPDATE' },
    { id: 'hours', title: 'Total Working Hours', value: '0h', icon: 'schedule', color: 'cyan', iconColor: '#00bcd4', trend: 'LIVE UPDATE' },
    { id: 'flux', title: 'Entries / Exits', value: '0 / 0', icon: 'swap_horiz', color: 'green', iconColor: '#4caf50', trend: 'LIVE UPDATE' },
    { id: 'cameras', title: 'Active Cameras', value: '0/0', icon: 'videocam', color: 'red', iconColor: '#f44336', trend: 'LIVE UPDATE' }
  ];

  alertsCount = 0;
  aiInsights: string[] = [];
  cameraStatusList: any[] = [];

  colorLegend = [
    { name: 'DOCTOR', color: '#f44336' },
    { name: 'WORKER', color: '#2196f3' },
    { name: 'SECURITY', color: '#ffeb3b' },
    { name: 'OTHER', color: '#9e9e9e' }
  ];

  constructor(private analyticsService: AnalyticsService) { }

  ngOnInit(): void {
    this.fetchData();
    this.initRealTimeConnection();
  }

  ngOnDestroy(): void {
    if (this.analyticsSub) {
      this.analyticsSub.unsubscribe();
    }
  }

  initRealTimeConnection() {
    this.analyticsSub = this.analyticsService.listenToAnalytics().subscribe({
      next: (liveData) => {
        console.log("LIVE ANALYTICS UPDATE:", liveData);
        this.updateStatsFromLive(liveData);
      },
      error: (err) => {
        console.error("WebSocket Connection Failed. Retrying in 5s...", err);
        setTimeout(() => this.initRealTimeConnection(), 5000);
      }
    });
  }

  updateStatsFromLive(data: any) {
    // 🔴 Règle: SI totalEntries == 0 && totalExits == 0 -> No data
    const entries = data.totalEntries || 0;
    const exits = data.totalExits || 0;
    const activeCams = data.activeCameras || 0;
    const totalCams = data.totalCameras || 0;

    let employees = data.currentEmployees || 0;

    if (entries === 0 && exits === 0) {
      employees = 0;
    }

    if (activeCams === 0) {
      employees = 0;
    }

    // Update KPIs
    this.kpis[0].value = employees.toString();
    this.kpis[1].value = (entries * 0.4).toFixed(1) + 'h';
    this.kpis[2].value = `${entries} / ${exits}`;
    this.kpis[3].value = `${activeCams}/${totalCams}`;

    if (data.alertsCount !== undefined) {
      this.alertsCount = data.alertsCount;
    }

    // Update Insights
    if (data.insights) {
      this.aiInsights = data.insights;
    }

    // Update Camera Status List
    if (data.cameraStatusList) {
      this.cameraStatusList = data.cameraStatusList;
    }

    // Update Charts ONLY if real data exists
    if (employees > 0) {
      if (data.hourlyStats) {
        this.updateChartFromLive(data.hourlyStats);
      }
      if (data.uniformStats) {
        this.updateColorChartFromLive(data.uniformStats);
      }
    }
  }

  updateChartFromLive(hourlyStatsMap: any) {
    const hoursLabels = ['08h', '10h', '12h', '14h', '16h', '18h', '20h', '22h'];
    const seriesData = [];

    // Map discrete hours to the labels
    for (let h of [8, 10, 12, 14, 16, 18, 20, 22]) {
      seriesData.push(hourlyStatsMap[h] || 0);
    }

    const dataPresenceChart: any = {
      labels: hoursLabels,
      series: [seriesData]
    };

    const options = {
      lineSmooth: Chartist.Interpolation.cardinal({ tension: 0.4 }),
      low: 0,
      high: Math.max(...seriesData, 5),
      showArea: true,
      chartPadding: { top: 20, right: 20, bottom: 0, left: 0 },
      axisY: { onlyInteger: true }
    };

    new Chartist.Line('#presenceChart', dataPresenceChart, options);
  }

  updateColorChartFromLive(uniformStats: any) {
    const series = [
      uniformStats.doctor || 0,
      uniformStats.worker || 0,
      uniformStats.security || 0,
      uniformStats.other || 0
    ];

    const dataColorChart = { series };

    const optionsColorChart = {
      donut: true,
      donutWidth: 30,
      startAngle: 270,
      showLabel: false
    };

    new Chartist.Pie('#colorChart', dataColorChart, optionsColorChart);
  }

  ngAfterViewInit() {
    // Initial empty state
  }

  fetchData() {
    this.analyticsService.getDashboardData().subscribe({
      next: (data) => {
        this.processInitialData(data);
        this.loading = false;
      },
      error: (err) => {
        console.error("Error fetching analytics data:", err);
        this.loading = false;
      }
    });
  }

  processInitialData(data: any) {
    const stats = data.stats || {};
    this.updateStatsFromLive(stats);
  }
}
