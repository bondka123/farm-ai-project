package com.farm.backend.controller;

import com.farm.backend.dto.AIDetectionDTO;
import com.farm.backend.entity.Alert;
import com.farm.backend.entity.CameraEntity;
import com.farm.backend.repository.CameraRepository;
import com.farm.backend.service.AlertService;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin
public class AlertController {

    private final AlertService service;
    private final CameraRepository cameraRepository;

    public AlertController(AlertService service,
                           CameraRepository cameraRepository) {
        this.service = service;
        this.cameraRepository = cameraRepository;
    }

    // 🔥 GET ALL ALERTS
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping
    public List<Alert> getAll() {
        return service.getAll();
    }

    // 🔥 CREATE ALERT (IA)
    @PostMapping
    public void create(@RequestBody Map<String, Object> data) {

        String status = (String) data.get("status");

        if ("alert".equals(status)) {

            Long cameraId = Long.valueOf(data.get("cameraId").toString());

            CameraEntity camera = cameraRepository.findById(cameraId)
                    .orElseThrow(() -> new RuntimeException("Camera not found"));

            Long departmentId = camera.getDepartment() != null
                    ? camera.getDepartment().getId()
                    : null;

            service.create(
                    "AI_ALERT",
                    "Intrusion détectée",
                    "HIGH",
                    cameraId,
                    departmentId
            );
        }
    }

    // 🔥 CREATE ALERT FROM AI SYSTEM
    @PostMapping("/ai-detection")
    public Alert createFromAI(@RequestBody AIDetectionDTO dto) {
        return service.handleAIDetection(dto);
    }

    // 🔥 RESOLVE ALERT
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/resolve")
    public Alert resolveAlert(@PathVariable Long id) {
        return service.resolveAlert(id);
    }
}