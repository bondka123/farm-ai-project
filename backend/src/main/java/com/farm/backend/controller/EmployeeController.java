package com.farm.backend.controller;

import com.farm.backend.entity.*;
import com.farm.backend.repository.*;
import com.farm.backend.service.EmailService;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmailService emailService;
    private final com.farm.backend.service.FaceService faceService;

    // ✅ CONSTRUCTEUR
    public EmployeeController(EmployeeRepository employeeRepository,
                              DepartmentRepository departmentRepository,
                              EmailService emailService,
                              com.farm.backend.service.FaceService faceService) {

        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.emailService = emailService;
        this.faceService = faceService;
    }

    // =====================================================
    // 🔥 CREATE EMPLOYEE (PENDING + NO FACE)
    // =====================================================
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PostMapping("/employee")
    public Employee createEmployee(@RequestBody Employee employee) {

        employee.setDepartment(null);

        // ✅ LOGIQUE CORRECTE
        employee.setStatus(EmployeeStatus.PENDING);

        employee.setCreatedAt(LocalDateTime.now());
        employee.setJob(normalizeEmployeeJob(employee.getJob().name()));
        employee.setFaceRegistered(false);
        employee.setAvailable(true);

        Employee saved = employeeRepository.save(employee);

        // 📧 Send creation email asynchronously (non-blocking)
        emailService.sendEmployeeCreated(saved.getEmail(), saved.getName());

        return saved;
    }

    // =====================================================
    // 🔥 VALIDATION FACE (PAR VIEWER)
    // =====================================================
    @PutMapping("/validate-face/{id}")
    public Employee validateFace(@PathVariable Long id) {

        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        emp.setFaceRegistered(true);
        // emp.setStatus(EmployeeStatus.APPROVED); // ❌ Separated from approval

        return employeeRepository.save(emp);
    }

    // =====================================================
    // 🔥 APPROVAL (PAR ADMIN)
    // =====================================================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/approve/{id}")
    public Employee approveEmployee(@PathVariable Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        emp.setStatus(EmployeeStatus.APPROVED);
        Employee saved = employeeRepository.save(emp);

        // 📧 Trigger Welcome Email
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());

        return saved;
    }

    // =====================================================
    // 🔥 CREATE MANAGER (PENDING)
    // =====================================================
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    @PostMapping("/manager")
    public Employee createByManager(@RequestBody Employee employee) {

        Department dep = departmentRepository
                .findById(employee.getDepartment().getId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        employee.setDepartment(dep);
        employee.setStatus(EmployeeStatus.PENDING);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setFaceRegistered(false);
        employee.setAvailable(true);
        employee.setJob(Job.OTHER);

        return employeeRepository.save(employee);
    }

    // =====================================================
    // 🔥 ADMIN CREATE MANAGER
    // =====================================================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/admin")
    public Employee createByAdmin(@RequestBody Employee employee) {

        Department dep = departmentRepository
                .findById(employee.getDepartment().getId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        employee.setDepartment(dep);
        employee.setStatus(EmployeeStatus.APPROVED);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setAvailable(true);
        employee.setJob(Job.OTHER);

        return employeeRepository.save(employee);
    }

    // =====================================================
    // 🔥 GET ONLY EMPLOYEES
    // =====================================================
    @GetMapping("/only-employees")
    public List<Employee> getOnlyEmployees() {
        return employeeRepository.findByJobIn(Arrays.asList(Job.DOCTOR, Job.ELECTRICIAN, Job.WORKER));
    }

    private Job normalizeEmployeeJob(String job) {
        if (job == null || job.trim().isEmpty()) {
            return Job.WORKER;
        }

        String normalized = job.trim().toUpperCase();
        if ("DOCTEUR".equals(normalized) || "MEDECIN".equals(normalized) || "MÉDECIN".equals(normalized)) {
            return Job.DOCTOR;
        }
        if ("ELECTRICIEN".equals(normalized) || "ÉLECTRICIEN".equals(normalized)) {
            return Job.ELECTRICIAN;
        }
        if ("OUVRIER".equals(normalized) || "EMPLOYE".equals(normalized) || "EMPLOYÉ".equals(normalized)) {
            return Job.WORKER;
        }

        try {
            return Job.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return Job.WORKER;
        }
    }

    // =====================================================
    // 🔥 GET BY MANAGER
    // =====================================================
    @GetMapping("/manager/{id}")
    public List<Employee> getManagerEmployees(@PathVariable Long id) {
        return employeeRepository.findByDepartmentManagerId(id);
    }

    // =====================================================
    // 🔥 GET ALL
    // =====================================================
    @GetMapping
    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    // 🔥 NEW: GET MY PROFILE (Find or create for Admin/User)
    @GetMapping("/me")
    public Employee getMyProfile(@RequestParam String email) {
        return employeeRepository.findAll().stream()
                .filter(e -> e.getEmail() != null && e.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseGet(() -> {
                    // Create a default employee record if none exists for this user
                    Employee newEmp = new Employee();
                    newEmp.setEmail(email);
                    newEmp.setName(email.split("@")[0]);
                    newEmp.setJob(Job.OTHER);
                    newEmp.setStatus(EmployeeStatus.APPROVED);
                    newEmp.setFaceRegistered(false);
                    newEmp.setCreatedAt(LocalDateTime.now());
                    return employeeRepository.save(newEmp);
                });
    }

    // =====================================================
    // 🔥 GET PENDING
    // =====================================================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/pending")
    public List<Employee> getPending() {
        return employeeRepository.findByStatus(EmployeeStatus.PENDING);
    }

    // =====================================================
    // 🔥 DELETE
    // =====================================================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        employeeRepository.deleteById(id);
        return "Employee deleted";
    }

    // =====================================================
    // 🔥 EMPLOYEES SANS FACE
    // =====================================================
    @GetMapping("/no-face")
    public List<Employee> getWithoutFace() {
        return employeeRepository.findByFaceRegisteredFalse();
    }

    // =====================================================
    // 🎭 FACE MANAGEMENT (PYTHON INTEGRATION)
    // =====================================================
    @PostMapping("/register-face/{id}")
    public Object registerFace(@PathVariable Long id) {
        faceService.registerFaceByEmployeeId(id);
        return Map.of("status", "success", "message", "Face registered");
    }

    @DeleteMapping("/delete-face/{id}")
    public Object deleteFace(@PathVariable Long id) {
        faceService.deleteFaceByEmployeeId(id);
        return Map.of("status", "success", "message", "Face deleted");
    }
}
