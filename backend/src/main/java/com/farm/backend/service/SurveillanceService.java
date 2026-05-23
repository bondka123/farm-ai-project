package com.farm.backend.service;

import com.farm.backend.entity.*;
import com.farm.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class SurveillanceService {

    private final FaceEventRepository faceEventRepository;
    private final RoleEventRepository roleEventRepository;
    private final CameraAlertRepository alertRepository;
    private final UnknownDetectionRepository unknownRepository;
    private final CameraRepository cameraRepository;

    public SurveillanceService(FaceEventRepository faceEventRepository,
                               RoleEventRepository roleEventRepository,
                               CameraAlertRepository alertRepository,
                               UnknownDetectionRepository unknownRepository,
                               CameraRepository cameraRepository) {
        this.faceEventRepository = faceEventRepository;
        this.roleEventRepository = roleEventRepository;
        this.alertRepository = alertRepository;
        this.unknownRepository = unknownRepository;
        this.cameraRepository = cameraRepository;
    }

    @Transactional
    public void recordFaceEvent(Long cameraId, String employeeName, String status, String imagePath) {
        CameraEntity camera = cameraRepository.findById(cameraId).orElse(null);
        if (camera == null) return;

        FaceEvent event = new FaceEvent();
        event.setCamera(camera);
        event.setEmployeeName(employeeName);
        event.setStatus(status);
        event.setImagePath(imagePath);
        faceEventRepository.save(event);
    }

    @Transactional
    public void recordRoleEvent(Long cameraId, String status, String details, Map<String, Integer> counts, String imagePath) {
        CameraEntity camera = cameraRepository.findById(cameraId).orElse(null);
        if (camera == null) return;

        RoleEvent event = new RoleEvent();
        event.setCamera(camera);
        event.setStatus(status);
        event.setDetails(details);
        event.setCounts(counts);
        event.setImagePath(imagePath);
        roleEventRepository.save(event);
    }

    @Transactional
    public void recordAlert(Long cameraId, String severity, String message, String imagePath) {
        CameraEntity camera = cameraRepository.findById(cameraId).orElse(null);
        if (camera == null) return;

        CameraAlert alert = new CameraAlert();
        alert.setCamera(camera);
        alert.setDepartment(camera.getDepartment());
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setImagePath(imagePath);
        alertRepository.save(alert);
    }
}
