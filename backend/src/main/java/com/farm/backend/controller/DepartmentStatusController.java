package com.farm.backend.controller;

import com.farm.backend.entity.*;
import com.farm.backend.repository.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/department-status")
@CrossOrigin
public class DepartmentStatusController {

    private final CameraRepository cameraRepository;
    private final DepartmentRepository departmentRepository;
    private final AlertRepository alertRepository;

    // Stores the latest counts and timestamp per camera: Map<CameraId, StateData>
    private final Map<Long, Map<String, Object>> cameraStates = new ConcurrentHashMap<>();

    public DepartmentStatusController(CameraRepository cameraRepository,
                                      DepartmentRepository departmentRepository,
                                      AlertRepository alertRepository) {
        this.cameraRepository = cameraRepository;
        this.departmentRepository = departmentRepository;
        this.alertRepository = alertRepository;
    }

    @PostMapping
    public Map<String, Object> receiveStatus(@RequestBody Map<String, Object> data) {
        if (!data.containsKey("cameraId")) {
            return Map.of("error", "cameraId missing");
        }

        Long cameraId = Long.valueOf(data.get("cameraId").toString());
        cameraStates.put(cameraId, data);

        // Find camera and department
        var cameraOpt = cameraRepository.findById(cameraId);
        if (cameraOpt.isEmpty()) return Map.of("error", "Camera not found");

        CameraEntity camera = cameraOpt.get();
        if (camera.getDepartment() == null) return Map.of("error", "Camera has no department");

        Department dept = camera.getDepartment();
        Long deptId = dept.getId();

        // 1. AGGREGATION
        Map<String, Integer> totalCounts = new HashMap<>();
        totalCounts.put("medecin", 0);
        totalCounts.put("worker", 0);
        totalCounts.put("electricien", 0);

        // Sum counts from all cameras in this department
        for (CameraEntity c : dept.getCameras()) {
            Map<String, Object> state = cameraStates.get(c.getId());
            if (state != null) {
                Map<String, Object> counts = (Map<String, Object>) state.get("counts");
                if (counts != null) {
                    totalCounts.put("medecin", totalCounts.get("medecin") + (int) counts.getOrDefault("medecin", 0));
                    totalCounts.put("worker", totalCounts.get("worker") + (int) counts.getOrDefault("worker", 0));
                    totalCounts.put("electricien", totalCounts.get("electricien") + (int) counts.getOrDefault("electricien", 0));
                }
            }
        }

        // 2. VALIDATION & ALERTS
        List<String> details = new ArrayList<>();
        boolean hasAlert = false;
        String severity = "INFO";

        // Check Unknown
        if (Boolean.TRUE.equals(data.get("unknown"))) {
            hasAlert = true;
            severity = "CRITICAL";
            details.add("🚨 UNKNOWN PERSON DETECTED!");
        }

        // Compare with requirements
        checkRequirement("doctor", totalCounts.get("medecin"), dept.getDoctors(), details);
        checkRequirement("worker", totalCounts.get("worker"), dept.getWorkers(), details);
        checkRequirement("electrician", totalCounts.get("electricien"), dept.getElectricians(), details);

        if (details.size() > (Boolean.TRUE.equals(data.get("unknown")) ? 1 : 0)) {
            hasAlert = true;
            if (!"CRITICAL".equals(severity)) severity = "WARNING";
        }

        // 3. PERSIST ALERT (Debounce)
        if (hasAlert) {
            String message = String.join(" | ", details);
            String hash = deptId + "_" + severity + "_" + (Boolean.TRUE.equals(data.get("unknown")) ? "UNK" : "COUNT");

            var existing = alertRepository.findFirstByUniqueHashAndResolvedFalseOrderByTimestampDesc(hash);
            if (existing.isEmpty()) {
                Alert alert = new Alert();
                alert.setDepartmentId(deptId);
                alert.setCameraId(cameraId);
                alert.setType("STAFFING_MISMATCH");
                alert.setMessage(message);
                alert.setSeverity(severity);
                alert.setUniqueHash(hash);
                alert.setTimestamp(java.time.LocalDateTime.now());
                alertRepository.save(alert);
            }
        }

        return Map.of(
            "status", hasAlert ? severity : "OK",
            "aggregatedCounts", totalCounts,
            "details", details
        );
    }

    private void checkRequirement(String role, int actual, int expected, List<String> details) {
        if (actual < expected) {
            details.add("MISSING " + role + ": " + actual + "/" + expected);
        } else if (actual > expected) {
            details.add("EXCESS " + role + ": " + actual + "/" + expected);
        }
    }

    @GetMapping("/health")
    public Map<Long, Boolean> getCameraHealth() {
        Map<Long, Boolean> health = new HashMap<>();
        long now = System.currentTimeMillis();
        cameraStates.forEach((id, state) -> {
            // Heartbeat check (assume sent every 10s, timeout after 30s)
            // Note: We need a timestamp in the 'state' map
        });
        return health;
    }
}