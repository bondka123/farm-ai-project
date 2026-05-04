import { Component, OnInit } from '@angular/core';
import { EmployeeService, Employee } from '../../services/employee.service';

@Component({
  selector: 'app-face-registration',
  templateUrl: './face-registration.component.html'
})
export class FaceRegistrationComponent implements OnInit {

  employees: Employee[] = [];
  selected?: Employee;
  video: any;

  constructor(private service: EmployeeService) {}

  ngOnInit(): void {
    this.load();
    this.startCamera();
  }

  load() {
    this.service.getWithoutFace().subscribe(res => this.employees = res);
  }

  startCamera() {
    navigator.mediaDevices.getUserMedia({ video: true })
      .then(stream => {
        const video = document.querySelector('video');
        if (video) {
          video.srcObject = stream;
        }
      });
  }

  select(e: Employee) {
    this.selected = e;
  }

  capture() {
    const canvas = document.createElement('canvas');
    const video: any = document.querySelector('video');

    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;

    const ctx = canvas.getContext('2d');
    ctx?.drawImage(video, 0, 0);

    canvas.toBlob(blob => {
      if (blob && this.selected?.id) {
        const file = new File([blob], 'face.jpg');

        this.service.registerFace(this.selected.id, file)
          .subscribe(() => {
            alert("Face enregistrée ✅");
            this.load();
          });
      }
    });
  }
}