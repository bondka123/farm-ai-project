package com.farm.backend.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
public class AIService {

    private Map<Long, Process> processes = new HashMap<>();
    private final String PYTHON_PATH = "python"; 
    private final String SCRIPT_PATH = "camera_ai_stream.py";

    public synchronized String startAI(Long cameraId, String source, String type) {
        if (processes.containsKey(cameraId) && processes.get(cameraId).isAlive()) {
            return "AI for camera " + cameraId + " is already running";
        }

        try {
            String rootPath = System.getProperty("user.dir");
            
            String scriptName = "camera_ai_stream.py"; // Default
            if ("FACE".equalsIgnoreCase(type)) {
                scriptName = "face_ai.py";
            } else if ("COLOR".equalsIgnoreCase(type)) {
                scriptName = "color_ai.py";
            }

            ProcessBuilder pb = new ProcessBuilder(
                PYTHON_PATH, 
                scriptName,
                "--source", source,
                "--camera_id", String.valueOf(cameraId)
            );
            
            File aiDir = new File(rootPath, "ai_system");
            if (!aiDir.exists()) {
                aiDir = new File(new File(rootPath).getParentFile(), "ai_system");
            }

            pb.directory(aiDir);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            processes.put(cameraId, process);

            CompletableFuture.runAsync(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("[AI " + type + " CAM " + cameraId + "] " + line);
                    }
                } catch (Exception e) {
                    System.err.println("Error reading AI logs for cam " + cameraId + ": " + e.getMessage());
                }
            });

            return "AI (" + type + ") Started for camera " + cameraId;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error starting AI: " + e.getMessage();
        }
    }

    public synchronized String stopAI(Long cameraId) {
        if (processes.containsKey(cameraId)) {
            Process p = processes.get(cameraId);
            if (p.isAlive()) p.destroy();
            processes.remove(cameraId);
            return "AI Stopped for camera " + cameraId;
        }
        return "AI was not running for camera " + cameraId;
    }

    public synchronized boolean isRunning(Long cameraId) {
        return processes.containsKey(cameraId) && processes.get(cameraId).isAlive();
    }
}
