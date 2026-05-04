package com.farm.backend.controller;

import com.farm.backend.entity.Employee;
import com.farm.backend.entity.User;
import com.farm.backend.repository.EmployeeRepository;
import com.farm.backend.repository.UserRepository;
import com.farm.backend.service.FaceService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;

@RestController
@RequestMapping("/api/face")
@CrossOrigin
public class FaceController {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final FaceService faceService;

    public FaceController(UserRepository userRepository,
                          EmployeeRepository employeeRepository,
                          FaceService faceService) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.faceService = faceService;
    }

    public static class FaceRegisterRequest {
        public Long userId;
    }

    @PostMapping("/register")
    public Object registerFace(@RequestBody FaceRegisterRequest request) {
        if (request.userId == null) {
            return Map.of("error", "userId is missing");
        }

        Optional<User> userOpt = userRepository.findById(request.userId);
        if (userOpt.isEmpty()) {
            return Map.of("error", "User not found");
        }

        User user = userOpt.get();

        // Find Employee by email
        Optional<Employee> empOpt = employeeRepository.findByEmail(user.getEmail()).stream().findFirst();
        if (empOpt.isEmpty()) {
            return Map.of("error", "Employee not found for this user. Face registration requires an Employee record.");
        }

        Employee emp = empOpt.get();

        // 🔥 Call Python Face Service
        try {
            // Trigger Python script directly as in AuthController or use FaceService
            // Note: FaceService.registerFace(employeeId) calls python API
            // But wait, the python API for register face might not open the camera the same way as `C:/face/register_face.py`
            // Let's use the same approach as AuthController.register()
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "C:/face/register_face.py",
                    String.valueOf(emp.getId())
            );

            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                // Update User and Employee
                user.setFaceRegistered(true);
                userRepository.save(user);

                emp.setFaceRegistered(true);
                employeeRepository.save(emp);

                return Map.of("message", "Face registered successfully");
            } else {
                return Map.of("error", "Failed to register face. Camera process exited with error.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Error starting face registration process: " + e.getMessage());
        }
    }
}
