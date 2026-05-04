package com.farm.backend.controller;

import com.farm.backend.entity.Role;
import com.farm.backend.entity.User;
import com.farm.backend.entity.Employee;
import com.farm.backend.entity.EmployeeStatus;
import com.farm.backend.entity.Department;
import com.farm.backend.repository.UserRepository;
import com.farm.backend.repository.EmployeeRepository;
import com.farm.backend.repository.DepartmentRepository;
import com.farm.backend.service.EmailService;
import com.farm.backend.entity.Job;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public UserController(UserRepository repo,
                          PasswordEncoder encoder,
                          EmailService emailService,
                          EmployeeRepository employeeRepository,
                          DepartmentRepository departmentRepository) {
        this.repo = repo;
        this.encoder = encoder;
        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    // =========================
    // 🔥 VIEWERS
    // =========================

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/viewers")
    public List<User> getViewers() {
        return repo.findByRole(Role.ROLE_VIEWER);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/viewers")
    public User createViewer(@RequestBody User user) {

        if (repo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email déjà utilisé ❌");
        }

        String password = UUID.randomUUID().toString().substring(0, 8);
        String token = UUID.randomUUID().toString();

        user.setPassword(encoder.encode(password));
        user.setRole(Role.ROLE_VIEWER);
        user.setEnabled(false);
        user.setActivationToken(token);

        User saved = repo.save(user);

        // 🔥 create associated Employee so face registration works
        try {
            Employee emp = new Employee();
            emp.setName(user.getFirstName() != null ? user.getFirstName() + " " + user.getLastName() : user.getEmail());
            emp.setEmail(user.getEmail());
            emp.setJob(Job.OTHER);
            emp.setStatus(EmployeeStatus.APPROVED);
            emp.setCreatedAt(LocalDateTime.now());
            emp.setFaceRegistered(false);
            emp.setAvailable(true);
            employeeRepository.save(emp);
        } catch (Exception e) {
            System.out.println("❌ ERREUR CREATION EMPLOYEE VIEWER: " + e.getMessage());
        }

        // 📧 EMAIL VIEWER
        emailService.sendViewerAccount(user.getEmail(), password, token);

        return saved;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/viewers/{id}")
    public User updateViewer(@PathVariable Long id, @RequestBody User updated) {

        User user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(updated.getFirstName());
        user.setLastName(updated.getLastName());
        user.setPhone(updated.getPhone());

        return repo.save(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/viewers/{id}")
    public ResponseEntity<Void> deleteViewer(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }

    // =========================
    // 🔥 MANAGERS
    // =========================

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/managers")
    public List<User> getManagers(org.springframework.security.core.Authentication auth) {
        System.out.println("User role: " + (auth != null ? auth.getAuthorities() : "NULL AUTH"));
        return repo.findByRole(Role.ROLE_MANAGER);
    }

    // =========================
    // 🔥 CREATE MANAGER + EMAIL
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/managers")
    public User createManager(@RequestBody User user) {

        if (repo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email déjà utilisé ❌");
        }

        String password = UUID.randomUUID().toString().substring(0, 8);
        String token = UUID.randomUUID().toString();

        user.setPassword(encoder.encode(password));
        user.setRole(Role.ROLE_MANAGER);

        // 🔥 IMPORTANT
        user.setEnabled(false); // bloque login avant activation
        user.setActivationToken(token);

        User saved = repo.save(user);

        // 🔥 create associated Employee so face registration works
        try {
            Department dep = departmentRepository.findAll().stream().findFirst().orElse(null);
            Employee emp = new Employee();
            emp.setName(user.getFirstName() != null ? user.getFirstName() + " " + user.getLastName() : user.getEmail());
            emp.setEmail(user.getEmail());
            emp.setJob(Job.OTHER);
            emp.setStatus(EmployeeStatus.APPROVED);
            emp.setDepartment(dep);
            emp.setCreatedAt(LocalDateTime.now());
            emp.setFaceRegistered(false);
            emp.setAvailable(true);
            employeeRepository.save(emp);
        } catch (Exception e) {
            System.out.println("❌ ERREUR CREATION EMPLOYEE: " + e.getMessage());
        }

        // 📧 EMAIL MANAGER (AUTO)
        try {
            emailService.sendManagerAccount(
                    user.getEmail(),
                    password,
                    token
            );
        } catch (Exception e) {
            System.out.println("❌ ERREUR EMAIL: " + e.getMessage());
        }

        return saved;
    }

    // =========================
    // 🔥 UPDATE MANAGER
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/managers/{id}")
    public User updateManager(@PathVariable Long id, @RequestBody User updated) {

        User user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(updated.getFirstName());
        user.setLastName(updated.getLastName());
        user.setPhone(updated.getPhone());

        return repo.save(user);
    }

    // =========================
    // 🔥 DELETE MANAGER
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/managers/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }



    // =========================
    // 🔥 TEST EMAIL (DEV ONLY)
    // =========================
    @GetMapping("/test-mail")
    public String testMail() {

        emailService.sendManagerAccount(
                "bondkaamine9@gmail.com",
                "123456",
                "TEST-TOKEN"
        );

        return "EMAIL SENT ✅";
    }

    // =========================
    // ⚙️ SETTINGS
    // =========================

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@RequestBody User dto, org.springframework.security.core.Authentication auth) {
        String email = auth.getName();
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        // We might want to allow changing email too, but it's used as username
        // user.setEmail(dto.getEmail()); 

        return ResponseEntity.ok(repo.save(user));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody com.farm.backend.dto.PasswordDto dto, org.springframework.security.core.Authentication auth) {
        String email = auth.getName();
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(encoder.encode(dto.getNewPassword()));
        repo.save(user);
        return ResponseEntity.ok("Password updated ✅");
    }

    // =========================
    // ⚠️ METTRE TOUJOURS EN DERNIER
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public User getOne(@PathVariable Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}