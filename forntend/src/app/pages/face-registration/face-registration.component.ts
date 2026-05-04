import { Component, OnInit } from '@angular/core';
import { EmployeeService, Employee } from '../../services/employee.service';

@Component({
  selector: 'app-face-registration',
  templateUrl: './face-registration.component.html'
})
export class FaceRegistrationComponent implements OnInit {

  employees: Employee[] = [];
  selected?: Employee;
  loading = false;
  statusMessage = '';

  constructor(private service: EmployeeService) {}

  ngOnInit(): void {
    this.load();
  }

  load() {
    this.service.getWithoutFace().subscribe(res => this.employees = res);
  }

  select(e: Employee) {
    this.selected = e;
  }

  capture() {
    if (!this.selected?.id) return;
    
    this.loading = true;
    this.statusMessage = 'Initialisation de la caméra système... Regardez l\'objectif 📷';
    
    this.service.registerFace(this.selected.id)
      .subscribe({
        next: () => {
          this.loading = false;
          alert("Face enregistrée avec succès ✅");
          this.load();
          this.selected = undefined;
          this.statusMessage = '';
        },
        error: (err) => {
          this.loading = false;
          alert("Erreur lors de la capture : " + (err.error?.error || "Vérifiez que la caméra est branchée"));
          this.statusMessage = '';
        }
      });
  }
}