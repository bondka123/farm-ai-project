package com.farm.backend.controller;

import com.farm.backend.entity.CameraEntity;
import com.farm.backend.entity.Department;
import com.farm.backend.repository.CameraRepository;
import com.farm.backend.repository.*;
import com.farm.backend.entity.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/cameras")
@CrossOrigin
public class CameraController {

    private final CameraRepository cameraRepository;
    private final DepartmentRepository departmentRepository;
    private final FaceEventRepository faceEventRepository;
    private final RoleEventRepository roleEventRepository;
    private final CameraAlertRepository alertRepository;
    private final UnknownDetectionRepository unknownRepository;

    public CameraController(CameraRepository cameraRepository,
                            DepartmentRepository departmentRepository,
                            FaceEventRepository faceEventRepository,
                            RoleEventRepository roleEventRepository,
                            CameraAlertRepository alertRepository,
                            UnknownDetectionRepository unknownRepository) {
        this.cameraRepository = cameraRepository;
        this.departmentRepository = departmentRepository;
        this.faceEventRepository = faceEventRepository;
        this.roleEventRepository = roleEventRepository;
        this.alertRepository = alertRepository;
        this.unknownRepository = unknownRepository;
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

    // =========================
    // ENTERPRISE HISTORY
    // =========================
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable Long id) {
        Map<String, Object> history = new HashMap<>();
        history.put("faceEvents", faceEventRepository.findByCameraIdOrderByTimestampDesc(id));
        history.put("roleEvents", roleEventRepository.findByCameraIdOrderByTimestampDesc(id));
        history.put("alerts", alertRepository.findByCameraIdOrderByTimestampDesc(id));
        history.put("unknowns", unknownRepository.findByCameraIdOrderByTimestampDesc(id));
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/today")
    public ResponseEntity<?> getTodayHistory(@PathVariable Long id) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        
        Map<String, Object> history = new HashMap<>();
        history.put("faceEvents", faceEventRepository.findByCameraIdAndTimestampBetweenOrderByTimestampDesc(id, startOfDay, endOfDay));
        history.put("roleEvents", roleEventRepository.findByCameraIdAndTimestampBetweenOrderByTimestampDesc(id, startOfDay, endOfDay));
        
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/alerts")
    public List<CameraAlert> getAlerts(@PathVariable Long id) {
        return alertRepository.findByCameraIdOrderByTimestampDesc(id);
    }
}