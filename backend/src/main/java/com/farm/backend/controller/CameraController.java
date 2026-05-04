package com.farm.backend.controller;

import com.farm.backend.entity.CameraEntity;
import com.farm.backend.entity.Department;
import com.farm.backend.repository.CameraRepository;
import com.farm.backend.repository.DepartmentRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

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

    // =========================
    // ADD CAMERA
    // =========================
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping
    public CameraEntity addCamera(@RequestBody CameraEntity camera) {

        if (camera.getDepartment() != null) {
            Department dept = departmentRepository
                    .findById(camera.getDepartment().getId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            camera.setDepartment(dept);
        }

        camera.setStatus("OFF");

        return cameraRepository.save(camera);
    }

    // =========================
    // GET ALL
    // =========================
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public List<CameraEntity> getAll() {
        return cameraRepository.findAll();
    }

    // =========================
    // DELETE
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
    cameraRepository.deleteById(id);
   }

    

    // =========================
    // UPDATE CAMERA
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public CameraEntity updateCamera(@PathVariable Long id,
                                     @RequestBody CameraEntity updated) {

        CameraEntity cam = cameraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Camera not found"));

        cam.setName(updated.getName());
        cam.setType(updated.getType());
        cam.setSource(updated.getSource());
        cam.setLocation(updated.getLocation());

        cam.setStatus("OFF");

        return cameraRepository.save(cam);
    }
}