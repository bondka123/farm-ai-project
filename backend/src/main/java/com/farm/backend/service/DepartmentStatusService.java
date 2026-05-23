package com.farm.backend.service;

import com.farm.backend.dto.DepartmentStatusRequest;
import com.farm.backend.entity.CameraEntity;
import com.farm.backend.entity.CameraEvent;
import com.farm.backend.entity.Department;
import com.farm.backend.repository.CameraEventRepository;
import com.farm.backend.repository.CameraRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DepartmentStatusService {

    private final CameraRepository cameraRepository;
    private final CameraEventRepository eventRepository;
    private final AlertService alertService;

    private final Map<String, Long> lastAlertTime = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN = 60000; // 1 minute

    public DepartmentStatusService(CameraRepository cameraRepository,
                                   CameraEventRepository eventRepository,
                                   AlertService alertService) {
        this.cameraRepository = cameraRepository;
        this.eventRepository = eventRepository;
        this.alertService = alertService;
    }

    public void processStatusUpdate(DepartmentStatusRequest request) {
        CameraEntity camera = cameraRepository.findById(request.getCameraId()).orElse(null);
        if (camera == null || camera.getDepartment() == null) return;

        Department dept = camera.getDepartment();
        Map<String, Integer> counts = request.getCounts() != null ? request.getCounts() : Map.of();
        List<String> issues = new ArrayList<>();

        validateStaffing(dept, counts, "medecin", dept.getDoctors(), camera.getId(), issues);
        validateStaffing(dept, counts, "worker", dept.getWorkers(), camera.getId(), issues);
        validateStaffing(dept, counts, "electricien", dept.getElectricians(), camera.getId(), issues);

        if (request.isUnknown()) {
            issues.add("Tenue non reconnue");
            sendAlert("UNAUTHORIZED_UNIFORM",
                    "Tenue non reconnue detectee dans " + dept.getName(),
                    "MEDIUM", dept.getId(), camera.getId());
        }

        saveColorHistory(camera, counts, issues);
    }

    private void validateStaffing(Department dept,
                                  Map<String, Integer> counts,
                                  String role,
                                  int required,
                                  Long cameraId,
                                  List<String> issues) {
        int actual = counts.getOrDefault(role, 0);
        if (actual < required) {
            issues.add("Manque " + role + ": " + actual + "/" + required);
            sendAlert("LOW_STAFFING",
                    "Manque de personnel (" + role + ") dans " + dept.getName() + ": " + actual + "/" + required,
                    "HIGH", dept.getId(), cameraId);
        } else if (actual > required) {
            issues.add("Surcharge " + role + ": " + actual + "/" + required);
            sendAlert("OVER_STAFFING",
                    "Surcharge de personnel (" + role + ") dans " + dept.getName() + ": " + actual + "/" + required,
                    "MEDIUM", dept.getId(), cameraId);
        }
    }

    private void saveColorHistory(CameraEntity camera, Map<String, Integer> counts, List<String> issues) {
        CameraEvent event = new CameraEvent();
        event.setCamera(camera);
        event.setType("COLOR_STATUS");
        event.setEmployeeName("Camera couleur");

        String countsText = "Medecin: " + counts.getOrDefault("medecin", 0)
                + " | Worker: " + counts.getOrDefault("worker", 0)
                + " | Electricien: " + counts.getOrDefault("electricien", 0);
        String statusText = issues.isEmpty() ? "Normal" : String.join(" ; ", issues);
        event.setMessage(statusText + " - " + countsText);

        eventRepository.save(event);
    }

    private void sendAlert(String type, String message, String severity, Long deptId, Long camId) {
        String key = type + "_" + deptId + (camId != null ? "_" + camId : "");
        long now = System.currentTimeMillis();
        if (now - lastAlertTime.getOrDefault(key, 0L) > ALERT_COOLDOWN) {
            alertService.create(type, message, severity, camId, deptId);
            lastAlertTime.put(key, now);
        }
    }
}
