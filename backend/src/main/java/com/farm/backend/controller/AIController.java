package com.farm.backend.controller;

import com.farm.backend.service.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
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
}
