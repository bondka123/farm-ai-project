import { Component, OnInit, OnDestroy, AfterViewInit, ViewChildren, QueryList, ElementRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { CameraService, Camera } from '../services/camera.service';
import { DepartmentService } from '../services/department.service';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-cameras',
  templateUrl: './cameras.component.html',
  styleUrls: ['./cameras.component.scss']
})
export class CamerasComponent implements OnInit, OnDestroy {

  @ViewChildren('videoPlayer') videoPlayers!: QueryList<ElementRef<HTMLVideoElement>>;

  cameras: Camera[] = [];
  departments: any[] = [];
  sharedStream: MediaStream | null = null;
  private isCapturingStream = false;
  cameraInitialized = false;
  imgTimestamp = Date.now();
  aiStatus: { [key: number]: boolean } = {}; // 🔥 Suivi par caméra

  editMode = false;
  editId?: number;

  refreshSub?: Subscription; // 🔥 REFRESH

  cameraForm: any = {
    name: '',
    type: '',
    location: '',
    source: '',
    departmentId: null
  };

  constructor(
    private cameraService: CameraService,
    private departmentService: DepartmentService,
    private http: HttpClient,
    private sanitizer: DomSanitizer
  ) { }

  // =========================
  // INIT
  // =========================
  ngOnInit(): void {
    this.loadCameras();
    this.loadDepartments();
    this.updateAllAIStatus();

    // 🔁 REFRESH AUTOMATIQUE (Plus rapide pour le flux image)
    this.refreshSub = interval(3000).subscribe(() => {
      this.imgTimestamp = Date.now();
      this.loadCameras();
      this.updateAllAIStatus();
    });
  }

  ngAfterViewInit() {
    // Detect when video elements appear/change in the DOM
    this.videoPlayers.changes.subscribe(() => {
      this.assignSharedStream();
    });
  }

  // =========================
  // DESTROY (important)
  // =========================
  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
    this.stopSharedStream();
  }


  // =========================
  // LOAD CAMERAS
  // =========================
  loadCameras() {
    this.cameraService.getAll().subscribe(res => {
      this.cameras = res;

      // ✅ On ne tente la capture locale QUE si on n'a pas déjà un flux d'images (AI)
      const hasLocalActiveDirect = this.cameras.some(c => c.status === 'ACTIVE' && this.isLocal(c.source) && !c.lastImage);

      if (hasLocalActiveDirect && !this.sharedStream) {
        this.initSharedWebcam();
      } else if (this.sharedStream) {
        // Si le flux direct existe déjà, on l'assigne aux nouveaux éléments
        setTimeout(() => this.assignSharedStream(), 100);
      }
    });
  }

  // =========================
  // LOAD DEPARTMENTS
  // =========================
  loadDepartments() {
    this.departmentService.getDepartments()
      .subscribe(res => this.departments = res);
  }

  // =========================
  // EDIT
  // =========================
  edit(c: Camera) {
    this.cameraForm = {
      name: c.name,
      type: c.type,
      location: c.location,
      source: c.source,
      departmentId: c.department?.id
    };

    this.editMode = true;
    this.editId = c.id;
  }

  // =========================
  // SAVE / UPDATE
  // =========================
  save() {

    const payload: Camera = {
      name: this.cameraForm.name,
      type: this.cameraForm.type,
      source: this.cameraForm.source,
      status: 'OFF',
      location: this.cameraForm.location,
      department: {
        id: Number(this.cameraForm.departmentId)
      }
    };

    if (this.editMode && this.editId) {

      this.cameraService.update(this.editId, payload)
        .subscribe(() => {
          alert("Camera modifiée ✅");
          this.resetForm();
          this.loadCameras();
        });

    } else {

      this.cameraService.create(payload)
        .subscribe(() => {
          alert("Camera ajoutée ✅");
          this.resetForm();
          this.loadCameras();
        });
    }
  }

  // =========================
  // DELETE
  // =========================
  delete(id?: number) {
    if (!id) return;

    if (confirm("Supprimer ?")) {
      this.cameraService.delete(id)
        .subscribe(() => this.loadCameras());
    }
  }

  // =========================
  // RESET
  // =========================
  resetForm() {
    this.cameraForm = {
      name: '',
      type: '',
      location: '',
      source: '',
      departmentId: null
    };
    this.editMode = false;
    this.editId = undefined;
  }

  // =========================
  // AI CONTROL (PER CAMERA)
  // =========================
  updateAllAIStatus() {
    this.cameras.forEach(cam => {
      if (cam.id) this.checkAIStatus(cam.id);
    });
  }

  checkAIStatus(cameraId: number) {
    this.http.get<{ running: boolean }>(`http://localhost:8081/api/ai/status/${cameraId}`)
      .subscribe(res => this.aiStatus[cameraId] = res.running);
  }

  toggleAI(cam: Camera) {
    if (!cam.id) return;

    const isRunning = this.aiStatus[cam.id];
    const endpoint = isRunning ? 'stop' : 'start';
    const payload = isRunning ? { cameraId: cam.id } : { cameraId: cam.id, source: cam.source, type: cam.type };

    this.http.post<{ running: boolean }>(`http://localhost:8081/api/ai/${endpoint}`, payload)
      .subscribe(res => {
        this.aiStatus[cam.id!] = res.running;
        this.loadCameras();
      });
  }

  // =========================
  // CAMERA STREAM LOGIC
  // =========================
  isUrl(source: string): boolean {
    return source?.startsWith('http') || source?.startsWith('ws');
  }

  isLocal(source: string): boolean {
    return source === '0' || source === '1' || source?.toLowerCase() === 'local';
  }

  getSafeUrl(source: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(source);
  }

  initSharedWebcam() {
    if (this.sharedStream || this.isCapturingStream) return;

    this.isCapturingStream = true;
    console.log("📸 CAPTURING SHARED WEBCAM STREAM...");

    navigator.mediaDevices.getUserMedia({
      video: { width: 1280, height: 720 },
      audio: false
    })
      .then(stream => {
        this.sharedStream = stream;
        this.isCapturingStream = false;
        this.assignSharedStream();
      })
      .catch(err => {
        this.isCapturingStream = false;
        console.error("❌ Erreur d'accès à la webcam locale", err);
      });
  }

  assignSharedStream() {
    if (!this.sharedStream || !this.videoPlayers) return;

    const players = this.videoPlayers.toArray();

    // 🔥 Si aucun élément <video> n'est dans le DOM, on peut libérer le flux
    if (players.length === 0) {
      this.stopSharedStream();
      return;
    }

    players.forEach(player => {
      const video = player.nativeElement;
      if (!video.srcObject) {
        console.log("🔗 ATTACHING STREAM TO VIDEO ELEMENT:", video.id);
        video.srcObject = this.sharedStream;
        video.play().catch(e => console.warn("Auto-play blocked or failed:", e));
      }
    });
  }

  stopSharedStream() {
    if (this.sharedStream) {
      this.sharedStream.getTracks().forEach(track => track.stop());
      this.sharedStream = null;
    }
  }
}
