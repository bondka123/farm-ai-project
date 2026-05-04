package com.farm.backend.service;

import com.farm.backend.entity.Alert;
import com.farm.backend.entity.Department;
import com.farm.backend.repository.AlertRepository;
import com.farm.backend.repository.DepartmentRepository;
import com.farm.backend.dto.AIDetectionDTO;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository repo;
    private final DepartmentRepository departmentRepository;

    public AlertService(AlertRepository repo,
                        DepartmentRepository departmentRepository) {
        this.repo = repo;
        this.departmentRepository = departmentRepository;
    }

    // =========================
    // 🔥 CREATE ALERT
    // =========================
    public Alert create(String type, String message, String severity,
                        Long cameraId, Long departmentId) {

        Alert alert = new Alert();
        alert.setType(type);
        alert.setMessage(message);
        alert.setSeverity(severity);
        alert.setCameraId(cameraId);
        alert.setDepartmentId(departmentId);
        alert.setTimestamp(LocalDateTime.now());

        return repo.save(alert);
    }

    // =========================
    // 🔥 GET ALL ALERTS (WITH DEPARTMENT NAME)
    // =========================
    public List<Alert> getAll() {

        List<Alert> alerts = repo.findAll();

        for (Alert alert : alerts) {

            Long deptId = alert.getDepartmentId();

            if (deptId != null) {

                departmentRepository.findById(deptId).ifPresent(dept -> {
                    alert.setDepartmentName(dept.getName()); // 🔥 KEY FIX
                });
            }
        }

        return alerts;
    }

    // =========================
    // 🔥 HANDLE AI DETECTION
    // =========================
    public Alert handleAIDetection(AIDetectionDTO dto) {
        String uniqueHash = dto.getType() + "_" + dto.getLocation() + "_";
        if (dto.getEmbeddingHash() != null) {
            uniqueHash += dto.getEmbeddingHash();
        } else if (dto.getEmployeeId() != null) {
            uniqueHash += dto.getEmployeeId();
        }

        Optional<Alert> existingOpt = repo.findFirstByUniqueHashAndResolvedFalseOrderByTimestampDesc(uniqueHash);
        if (existingOpt.isPresent()) {
            Alert existing = existingOpt.get();
            if (existing.getTimestamp().isAfter(LocalDateTime.now().minusMinutes(5))) {
                // Update timestamp and count
                existing.setTimestamp(LocalDateTime.now());
                existing.setCount(existing.getCount() + 1);
                return repo.save(existing);
            }
        }

        // Determine severity
        String severity = "LOW";
        String message = "Alerte générée";
        if ("UNKNOWN_PERSON".equals(dto.getType())) {
            severity = "HIGH";
            message = "Personne inconnue détectée";
        } else if ("NO_FACE".equals(dto.getType())) {
            severity = "MEDIUM";
            message = "Employé reconnu mais sans visage enregistré";
        } else if ("INTRUSION".equals(dto.getType())) {
            severity = "CRITICAL";
            message = "Intrusion dans une zone sécurisée";
        }

        // Handle Image
        String imagePath = null;
        if (dto.getImageBase64() != null && !dto.getImageBase64().isEmpty()) {
            try {
                String base64Data = dto.getImageBase64();
                if (base64Data.contains(",")) {
                    base64Data = base64Data.split(",")[1]; // remove prefix e.g. data:image/jpeg;base64,
                }
                byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

                String uploadDir = System.getProperty("user.dir") + "/uploads/alerts/";
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();

                String filename = UUID.randomUUID().toString() + ".jpg";
                Path path = Paths.get(uploadDir + filename);
                Files.write(path, decodedBytes);
                
                imagePath = "/uploads/alerts/" + filename;
            } catch (Exception e) {
                System.out.println("Error saving AI image: " + e.getMessage());
            }
        }

        Alert newAlert = new Alert();
        newAlert.setType(dto.getType());
        newAlert.setMessage(message);
        newAlert.setSeverity(severity);
        newAlert.setLocation(dto.getLocation());
        newAlert.setUniqueHash(uniqueHash);
        newAlert.setEmployeeId(dto.getEmployeeId());
        newAlert.setImagePath(imagePath);
        newAlert.setTimestamp(LocalDateTime.now());
        newAlert.setResolved(false);
        newAlert.setCount(1);

        return repo.save(newAlert);
    }

    // =========================
    // 🔥 RESOLVE ALERT
    // =========================
    public Alert resolveAlert(Long id) {
        Alert alert = repo.findById(id).orElseThrow(() -> new RuntimeException("Alert not found"));
        alert.setResolved(true);
        return repo.save(alert);
    }
}