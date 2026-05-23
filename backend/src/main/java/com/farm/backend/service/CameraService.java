package com.farm.backend.service;

import com.farm.backend.entity.CameraEntity;
import com.farm.backend.repository.AlertRepository;
import com.farm.backend.repository.AttendanceRepository;
import com.farm.backend.repository.CameraEventRepository;
import com.farm.backend.repository.CameraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.HashMap;

@Service
public class CameraService {

    private final CameraRepository repository;
    private final CameraEventRepository eventRepository;
    private final AttendanceRepository attendanceRepository;
    private final AlertRepository alertRepository;
    private final StreamProtocolDetector protocolDetector;
    private final AIService aiService;

    public CameraService(CameraRepository repository,
                         CameraEventRepository eventRepository,
                         AttendanceRepository attendanceRepository,
                         AlertRepository alertRepository,
                         StreamProtocolDetector protocolDetector,
                         AIService aiService) {
        this.repository = repository;
        this.eventRepository = eventRepository;
        this.attendanceRepository = attendanceRepository;
        this.alertRepository = alertRepository;
        this.protocolDetector = protocolDetector;
        this.aiService = aiService;
    }

    @PostConstruct
    public void resetCamerasOnStartup() {
        List<CameraEntity> cameras = repository.findAll();
        for (CameraEntity cam : cameras) {
            cam.setStatus("OFFLINE");
            repository.save(cam);
        }
        System.out.println("✅ All cameras reset to OFFLINE on startup.");
    }

    public List<CameraEntity> getAll() {
        return repository.findAll();
    }

    public Optional<CameraEntity> getById(Long id) {
        return repository.findById(id);
    }

    public CameraEntity save(CameraEntity camera) {
        if (camera.getId() != null) {
            aiService.stopAI(camera.getId());
        }
        String protocol = protocolDetector.detectProtocol(camera.getUrl());
        camera.setProtocol(protocol);
        camera.setStatus("OFFLINE");
        camera.setLastImage(null);
        return repository.save(camera);
    }

    public Map<String, Object> activateCamera(Long id) {
        CameraEntity camera = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Camera not found"));

        System.out.println("🚀 ACTIVATING CAMERA ID: " + id + " | NAME: " + camera.getName());
        Map<String, Object> result = new HashMap<>();

        if (!camera.isAiEnabled()) {
            result.put("success", false);
            result.put("message", "AI is disabled for this camera.");
            return result;
        }

        try {
            camera.setStatus("STARTING");
            repository.save(camera);

            String protocol = protocolDetector.detectProtocol(camera.getUrl());
            camera.setProtocol(protocol);
            System.out.println("📡 DETECTED PROTOCOL: " + protocol + " FOR URL: " + camera.getUrl());

            if ("LOCAL".equals(protocol)) {
                System.out.println("✅ LOCAL WEBCAM DETECTED - SKIPPING STREAM PROBE");
                camera.setResolution("640x480");
                camera.setFps(30);
                camera.setStatus("STARTING");
            } else {
                System.out.println("🔍 PROBING REMOTE STREAM...");
                StreamInfo info = validateStreamDetails(camera.getUrl());
                if (info == null || !info.isHealthy()) {
                    camera.setStatus("ERROR");
                    repository.save(camera);
                    result.put("success", false);
                    result.put("message", "Stream validation failed: check URL or connection.");
                    return result;
                }
                camera.setResolution(info.getResolution());
                camera.setFps(info.getFps());
                camera.setStatus("STARTING");
            }

            repository.save(camera);
            String launchResult = aiService.startAI(camera.getId(), camera.getUrl(), camera.getAiType());
            if (!aiService.isRunning(camera.getId())) {
                camera.setStatus("ERROR");
                repository.save(camera);
                result.put("success", false);
                result.put("message", "AI process did not start: " + launchResult);
                return result;
            }

            result.put("success", true);
            result.put("status", "STARTING");
            result.put("message", launchResult);
            return result;

        } catch (Exception e) {
            System.err.println("❌ ACTIVATION FAILED: " + e.getMessage());
            camera.setStatus("ERROR");
            repository.save(camera);
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        }
    }

    public void stopCamera(Long id) {
        CameraEntity camera = repository.findById(id).orElse(null);
        if (camera != null) {
            aiService.stopAI(id);
            camera.setStatus("OFFLINE");
            repository.save(camera);
        }
    }

    @Transactional
    public void delete(Long id) {
        aiService.stopAI(id);
        eventRepository.deleteByCameraId(id);
        alertRepository.deleteByCameraId(id);
        repository.deleteById(id);
    }

    private StreamInfo validateStreamDetails(String url) {
        try {
            String rootPath = System.getProperty("user.dir");
            String pythonPath = "python";
            String scriptPath = rootPath + "/ai_system/stream_probe.py";

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, "--url", url);
            Process process = pb.start();

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();

            if (output != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> map = mapper.readValue(output, Map.class);

                StreamInfo info = new StreamInfo();
                info.setHealthy(Boolean.TRUE.equals(map.get("healthy")));
                if (info.isHealthy()) {
                    info.setResolution((String) map.get("resolution"));
                    Object fpsObj = map.get("fps");
                    if (fpsObj instanceof Number) {
                        info.setFps(((Number) fpsObj).intValue());
                    } else {
                        info.setFps(30);
                    }
                }
                return info;
            }
        } catch (Exception e) {
            System.err.println("❌ Stream Probing Error: " + e.getMessage());
        }
        return null;
    }

    public static class StreamInfo {
        private boolean healthy;
        private String resolution;
        private Integer fps;

        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
        public Integer getFps() { return fps; }
        public void setFps(Integer fps) { this.fps = fps; }
    }
}
