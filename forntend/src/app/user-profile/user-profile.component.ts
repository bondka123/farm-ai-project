import { Component, OnInit } from '@angular/core';
import { FaceService } from '../services/face.service';
import { EmployeeService, Employee } from '../services/employee.service';

@Component({
  selector: 'app-user-profile',
  templateUrl: './user-profile.component.html',
  styleUrls: ['./user-profile.component.css']
})
export class UserProfileComponent implements OnInit {

  currentEmployee: Employee | null = null;
  loading = false;
  message = '';
  isError = false;
  faceLoginEnabled = true;
  showEditModal = false;

  openEditModal() {
    this.showEditModal = true;
    document.body.classList.add('modal-open');
  }

  closeEditModal() {
    this.showEditModal = false;
    document.body.classList.remove('modal-open');
  }

  // Edit Profile fields
  editName = '';
  editEmail = '';

  // Change Password fields
  oldPassword = '';
  newPassword = '';
  confirmPassword = '';

  constructor(
    private faceService: FaceService,
    private employeeService: EmployeeService
  ) { }

  ngOnInit() {
    this.loadCurrentEmployee();
  }

  loadCurrentEmployee() {
    const email = localStorage.getItem('email');
    if (!email) return;

    this.employeeService.getMyProfile(email).subscribe(employee => {
      console.log("MY PROFILE LOADED:", employee);
      this.currentEmployee = employee;
      if (this.currentEmployee) {
        this.editName = this.currentEmployee.name;
        this.editEmail = this.currentEmployee.email || '';
      }
    }, err => {
      console.error("ERROR LOADING PROFILE:", err);
      this.message = "❌ Error: Could not load or create your profile.";
      this.isError = true;
    });
  }

  registerFace() {
    console.log("REGISTER FACE CLICKED. ID:", this.currentEmployee?.id);
    if (!this.currentEmployee?.id) {
      this.message = "❌ Profile Not Linked: No employee record found for your email. Please contact an administrator to link your account.";
      this.isError = true;
      return;
    }
    
    this.loading = true;
    this.message = 'Opening system camera... Please look at the camera.';
    this.isError = false;

    this.faceService.registerFace().subscribe((res: any) => {
      this.loading = false;
      this.message = '✅ Face data saved successfully!';
      this.loadCurrentEmployee();
    }, err => {
      this.loading = false;
      this.message = err.error?.error || 'Error connecting to face recognition service.';
      this.isError = true;
    });
  }

  updateFace() {
    this.loading = true;
    this.message = 'Opening system camera... Please look at the camera.';
    this.isError = false;

    this.faceService.updateFace().subscribe((res: any) => {
      this.loading = false;
      this.message = '✅ Face data updated successfully!';
      this.loadCurrentEmployee();
    }, err => {
      this.loading = false;
      this.message = err.error?.error || 'Error updating face recognition data.';
      this.isError = true;
    });
  }

  deleteFace() {
    if (!this.currentEmployee?.id) return;
    if (!confirm('Are you sure you want to delete your biometric data?')) return;

    this.loading = true;
    this.faceService.deleteFace().subscribe((res: any) => {
      this.loading = false;
      this.message = 'Face data deleted.';
      this.loadCurrentEmployee();
    }, err => {
      this.loading = false;
      this.message = 'Error deleting face data.';
      this.isError = true;
    });
  }

  toggleFaceLogin() {
    // This could call a backend endpoint to update preference
    this.message = `Face login ${this.faceLoginEnabled ? 'enabled' : 'disabled'}`;
    setTimeout(() => this.message = '', 3000);
  }

  changePassword() {
    if (!this.newPassword || this.newPassword !== this.confirmPassword) {
      this.message = 'Passwords do not match or are empty.';
      this.isError = true;
      return;
    }

    this.loading = true;
    this.message = 'Updating password...';
    // Mocking password update call
    setTimeout(() => {
      this.loading = false;
      this.message = 'Password updated successfully!';
      this.isError = false;
      this.oldPassword = '';
      this.newPassword = '';
      this.confirmPassword = '';
    }, 1500);
  }
}

