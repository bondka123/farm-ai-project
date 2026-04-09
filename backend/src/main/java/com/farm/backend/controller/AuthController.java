package com.farm.backend.controller;

import com.farm.backend.entity.*;
import com.farm.backend.repository.*;
import com.farm.backend.config.JwtUtil;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public AuthController(UserRepository repo,
                          PasswordEncoder encoder,
                          EmployeeRepository employeeRepository,
                          DepartmentRepository departmentRepository) {
        this.repo = repo;
        this.encoder = encoder;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    // =========================
    // 🔐 LOGIN
    // =========================
    @PostMapping("/login")
    public Object login(@RequestBody User user) {

        if (user.getUsername() == null || user.getPassword() == null) {
            return Map.of("error", "Username or password is missing");
        }

        var db = repo.findByUsername(user.getUsername());

        if (db.isEmpty()) {
            return Map.of("error", "User not found");
        }

        if (!encoder.matches(user.getPassword(), db.get().getPassword())) {
            return Map.of("error", "Wrong password");
        }

        String token = JwtUtil.generateToken(
                db.get().getUsername(),
                db.get().getRole().name()
        );

        return Map.of(
                "token", token,
                "role", db.get().getRole().name()
        );
    }

    // =========================
    // 🔥 ADMIN CREATE USER
    // =========================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")

    @PostMapping("/register")
    public Object register(@RequestBody User user) {

        try {

            if (user.getUsername() == null || user.getPassword() == null || user.getRole() == null) {
                return Map.of("error", "Missing fields");
            }

            if (repo.findByUsername(user.getUsername()).isPresent()) {
                return Map.of("error", "Username already exists");
            }

            // 🔥 récupérer un département
            Department dep = departmentRepository.findAll()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No department found"));

            // 🔥 créer employee
            Employee emp = new Employee();
            emp.setName(user.getUsername());
            emp.setJob(user.getRole().name());
            emp.setStatus(EmployeeStatus.APPROVED);
            emp.setDepartment(dep);
            emp.setCreatedAt(LocalDateTime.now());
            emp.setFaceRegistered(false);

            employeeRepository.save(emp);

            // 🔥 créer user
            user.setPassword(encoder.encode(user.getPassword()));
            repo.save(user);

            return Map.of("msg", "USER + EMPLOYEE CREATED");

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Server error");
        }
    }

    // =========================
    // 🔥 MANAGER CREATE VIEWER
    // =========================
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/create-viewer")
    public Object createViewer(@RequestBody User user) {

        if (user.getUsername() == null || user.getPassword() == null) {
            return Map.of("error", "Missing fields");
        }

        if (repo.findByUsername(user.getUsername()).isPresent()) {
            return Map.of("error", "Username already exists");
        }

        user.setRole(Role.ROLE_VIEWER);
        user.setPassword(encoder.encode(user.getPassword()));

        repo.save(user);

        return Map.of("msg", "VIEWER CREATED");
    }

    // =========================
    // 🔥 TEST ADMIN
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String admin() {
        return "ADMIN OK";
    }

    // =========================
    // 🔥 TEST MANAGER
    // =========================
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager")
    public String manager() {
        return "MANAGER OK";
    }
}