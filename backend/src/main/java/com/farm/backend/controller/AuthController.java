package com.farm.backend.controller;

import com.farm.backend.entity.*;
import com.farm.backend.repository.*;
import com.farm.backend.config.JwtUtil;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final com.farm.backend.service.FaceService faceService;

    public AuthController(UserRepository repo,
                          PasswordEncoder encoder,
                          EmployeeRepository employeeRepository,
                          DepartmentRepository departmentRepository,
                          com.farm.backend.service.FaceService faceService) {
        this.repo = repo;
        this.encoder = encoder;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.faceService = faceService;
    }

    // =========================
    // 🔐 LOGIN
    // =========================
    @PostMapping("/login")
    public Object login(@RequestBody User user) {

        if (user.getEmail() == null || user.getPassword() == null) {
            return Map.of("error", "Email or password is missing");
        }

        var db = repo.findByEmail(user.getEmail());

        if (db.isEmpty()) {
            return Map.of("error", "User not found");
        }

        if (!encoder.matches(user.getPassword(), db.get().getPassword())) {
            return Map.of("error", "Wrong password");
        }

        // 🔥 check if enabled
        if (!db.get().isEnabled()) {
            return Map.of("error", "Account not activated");
        }   

        String token = JwtUtil.generateToken(
                db.get().getEmail(),   // 🔥 CORRECTION
                db.get().getRole().name()
        );

        return Map.of(
                "token", token,
                "role", db.get().getRole().name(),
                "email", db.get().getEmail(),
                "faceRegistered", db.get().isFaceRegistered(),
                "userId", db.get().getId()
        );
    }

    // =========================
    // 🔗 ACTIVATE
    // =========================
    @PostMapping("/activate")
    public Object activate(@RequestParam String token) {
        var db = repo.findByActivationToken(token);
        if (db.isEmpty()) {
            return Map.of("error", "Invalid activation token");
        }
        User user = db.get();
        user.setEnabled(true);
        user.setActivationToken(null);
        repo.save(user);

        String jwt = JwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return Map.of(
                "message", "Activated successfully",
                "userId", user.getId(),
                "role", user.getRole().name(),
                "faceRegistered", user.isFaceRegistered(),
                "token", jwt
        );
    }

    public static class RegisterRequest {
        public String email;
        public String password;
        public String role;
        public String job;
    }

    public static class FaceRegisterRequest {
        public Long userId;
    }

    @PostMapping("/face/register")
    public Object registerFace(@RequestBody FaceRegisterRequest request) {
        if (request.userId == null) {
            return Map.of("error", "userId is missing");
        }

        var userOpt = repo.findById(request.userId);
        if (userOpt.isEmpty()) {
            return Map.of("error", "User not found");
        }

        User user = userOpt.get();
        var employeeOpt = employeeRepository.findByEmail(user.getEmail()).stream().findFirst();
        if (employeeOpt.isEmpty()) {
            return Map.of("error", "Employee not found for this user");
        }

        Employee employee = employeeOpt.get();
        Map<String, Object> faceResponse = faceService.registerFace(employee.getId());
        if ("error".equals(faceResponse.get("status"))) {
            return faceResponse;
        }

        user.setFaceRegistered(true);
        repo.save(user);

        employee.setFaceRegistered(true);
        employeeRepository.save(employee);

        return Map.of(
                "message", "Face registered successfully",
                "userId", user.getId(),
                "role", user.getRole().name(),
                "faceRegistered", true
        );
    }

    // =========================
    // 👤 FACE LOGIN
    // =========================
    @PostMapping("/face-login")
    public Object faceLogin() {
        Map<String, Object> recognition = faceService.recognizeFace();

        if (!"success".equals(recognition.get("status"))) {
            return Map.of(
                "status", "error",
                "message", recognition.get("message") != null ? recognition.get("message") : "Face not recognized"
            );
        }

        Number employeeIdNum = (Number) recognition.get("employeeId");
        if (employeeIdNum == null) {
            return Map.of("status", "error", "message", "Unknown face");
        }

        var employee = employeeRepository.findById(employeeIdNum.longValue());
        if (employee.isEmpty()) {
            return Map.of("status", "error", "message", "Employee not found in database");
        }

        // 🔥 CHECK IF FACE IS REGISTERED (redundant but safe)
        if (!employee.get().isFaceRegistered()) {
            return Map.of("status", "error", "message", "Face not registered. Please register your face in Settings first.");
        }

        var user = repo.findByEmail(employee.get().getEmail());
        if (user.isEmpty()) {
            return Map.of("status", "error", "message", "User account not found for this employee");
        }

        String token = JwtUtil.generateToken(
                user.get().getEmail(),
                user.get().getRole().name()
        );

        return Map.of(
                "status", "success",
                "token", token,
                "role", user.get().getRole().name(),
                "email", user.get().getEmail(),
                "faceRegistered", user.get().isFaceRegistered(),
                "userId", user.get().getId(),
                "confidence", recognition.get("confidence")
        );
    }

    // =========================
    // 🔥 ADMIN CREATE USER
    // =========================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/register")
    public Object register(@RequestBody RegisterRequest request) {

        try {

            if (request.email == null || request.password == null || request.role == null || request.job == null) {
                return Map.of("error", "Missing fields");
            }

            if (repo.findByEmail(request.email).isPresent()) {
                return Map.of("error", "Email already exists");
            }

            Department dep = departmentRepository.findAll()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No department found"));

            // 🔥 employee
            Employee emp = new Employee();
            emp.setName(request.email); 
            emp.setEmail(request.email); // 🔥 FIX: Set email field

            emp.setJob(Job.valueOf(request.job.toUpperCase()));
            emp.setStatus(EmployeeStatus.APPROVED);
            emp.setDepartment(dep);
            emp.setCreatedAt(LocalDateTime.now());
            emp.setFaceRegistered(false);

            employeeRepository.save(emp);

            // 🔥 user
            User user = new User();
            user.setEmail(request.email);
            user.setPassword(encoder.encode(request.password));
            user.setRole(Role.valueOf(request.role));
            user.setFaceRegistered(false);
            repo.save(user);

            // 🔥 LANCER PYTHON (via FaceService)
            faceService.registerFace(emp.getId());

            return Map.of("msg", "USER CREATED + FACE STARTED");

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

        if (user.getEmail() == null || user.getPassword() == null) {
            return Map.of("error", "Missing fields");
        }

        if (repo.findByEmail(user.getEmail()).isPresent()) {
            return Map.of("error", "Email already exists");
        }

        user.setRole(Role.ROLE_VIEWER);
        user.setPassword(encoder.encode(user.getPassword()));
        user.setFaceRegistered(false);

        repo.save(user);

        return Map.of("msg", "VIEWER CREATED");
    }
}
