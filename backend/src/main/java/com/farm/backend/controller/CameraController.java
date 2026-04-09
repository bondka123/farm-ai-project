package com.farm.backend.controller;

import com.farm.backend.entity.CameraEntity;
import com.farm.backend.entity.Department;
import com.farm.backend.repository.CameraRepository;
import com.farm.backend.repository.DepartmentRepository;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cameras")
@CrossOrigin
public class CameraController {

    private final CameraRepository cameraRepository;
    private final DepartmentRepository departmentRepository;

    public CameraController(CameraRepository cameraRepository,
                            DepartmentRepository departmentRepository) {
        this.cameraRepository = cameraRepository;
        this.departmentRepository = departmentRepository;
    }

    // 🔒 ADMIN ONLY
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CameraEntity addCamera(@RequestBody CameraEntity camera) {

        if (camera.getDepartment() != null) {
            Department dept = departmentRepository
                    .findById(camera.getDepartment().getId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            camera.setDepartment(dept);
        }

        return cameraRepository.save(camera);
    }

    // 🔓 CONNECTED USERS
    @GetMapping
    public List<CameraEntity> getAll() {
        return cameraRepository.findAll();
    }

    // 🔒 ADMIN ONLY
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id) {
        cameraRepository.deleteById(id);
        return "Camera deleted";
    }

    // 🔒 ADMIN ONLY
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CameraEntity updateStatus(@PathVariable Long id,
                                     @RequestParam String status) {

        CameraEntity cam = cameraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Camera not found"));

        cam.setStatus(status);

        return cameraRepository.save(cam);
    }
}