package com.farm.backend.service;

import com.farm.backend.entity.CameraEntity;
import com.farm.backend.repository.CameraRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CameraMonitorService {

    private final CameraRepository cameraRepository;
    private final CameraHealthService healthService;

    public CameraMonitorService(CameraRepository cameraRepository,
                                CameraHealthService healthService) {
        this.cameraRepository = cameraRepository;
        this.healthService = healthService;
    }

    @Scheduled(fixedRate = 5000)
    public void monitor() {

        List<CameraEntity> cameras = cameraRepository.findAll();

        for (CameraEntity cam : cameras) {

            boolean reachable = false;

            // 🔥 TEST URL
            if (cam.getSource() != null && !cam.getSource().isEmpty()) {
                reachable = healthService.isReachable(cam.getSource());
            }

            // 🔥 TEST IMAGE (Python)
            boolean recentImage = false;

            if (cam.getLastSeen() != null) {
                recentImage = cam.getLastSeen()
                        .isAfter(LocalDateTime.now().minusSeconds(30));
            }

            // 🔥 LOGIQUE FINALE
            if (reachable || recentImage) {
                cam.setStatus("ACTIVE");
            } else {
                cam.setStatus("OFF");
            }

            cameraRepository.save(cam);

            System.out.println("📷 " + cam.getName() + " → " + cam.getStatus());
        }
    }
}