package com.farm.backend.controller;

import com.farm.backend.service.AIService;
import com.farm.backend.service.SurveillanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AIController {

    private final AIService aiService;
    private final SurveillanceService surveillanceService;

    public AIController(AIService aiService, SurveillanceService surveillanceService) {
        this.aiService = aiService;
        this.surveillanceService = surveillanceService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/start")
    public ResponseEntity<?> startAI(@RequestBody Map<String, Object> payload) {
        Long cameraId = Long.valueOf(payload.get("cameraId").toString());
        String source = payload.get("source").toString();
        String type = payload.getOrDefault("type", "DEFAULT").toString();
        
        String result = aiService.startAI(cameraId, source, type);
        return ResponseEntity.ok(Map.of("message", result, "running", aiService.isRunning(cameraId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/stop")
    public ResponseEntity<?> stopAI(@RequestBody Map<String, Long> payload) {
        Long cameraId = payload.get("cameraId");
        String result = aiService.stopAI(cameraId);
        return ResponseEntity.ok(Map.of("message", result, "running", aiService.isRunning(cameraId)));
    }

    @GetMapping("/status/{cameraId}")
    public ResponseEntity<?> getStatus(@PathVariable Long cameraId) {
        return ResponseEntity.ok(Map.of("running", aiService.isRunning(cameraId)));
    }

    // =========================
    // AI EVENT RECEIVERS
    // =========================
    @PostMapping("/events/face")
    public ResponseEntity<?> recordFaceEvent(@RequestBody Map<String, Object> payload) {
        surveillanceService.recordFaceEvent(
            Long.valueOf(payload.get("cameraId").toString()),
            payload.get("employeeName").toString(),
            payload.get("status").toString(),
            (String) payload.get("imagePath")
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/role")
    public ResponseEntity<?> recordRoleEvent(@RequestBody Map<String, Object> payload) {
        surveillanceService.recordRoleEvent(
            Long.valueOf(payload.get("cameraId").toString()),
            payload.get("status").toString(),
            payload.get("details").toString(),
            (Map<String, Integer>) payload.get("counts"),
            (String) payload.get("imagePath")
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/alert")
    public ResponseEntity<?> recordAlert(@RequestBody Map<String, Object> payload) {
        surveillanceService.recordAlert(
            Long.valueOf(payload.get("cameraId").toString()),
            payload.get("severity").toString(),
            payload.get("message").toString(),
            (String) payload.get("imagePath")
        );
        return ResponseEntity.ok().build();
    }
}
