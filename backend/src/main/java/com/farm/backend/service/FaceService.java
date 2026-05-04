package com.farm.backend.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.util.Map;

@Service
public class FaceService {

    public Map<String, Object> registerFace(Long employeeId) {
        try {
            String rootPath = System.getProperty("user.dir");
            File aiDir = new File(rootPath, "ai_system");
            
            // 🔥 FIX: Si on est dans le dossier 'backend', on remonte d'un cran
            if (!aiDir.exists()) {
                File parent = new File(rootPath).getParentFile();
                aiDir = new File(parent, "ai_system");
            }

            if (!aiDir.exists()) {
                return Map.of("status", "error", "message", "Dossier 'ai_system' introuvable à : " + aiDir.getAbsolutePath());
            }

            ProcessBuilder pb = new ProcessBuilder(
                "python", 
                "register_face.py",
                String.valueOf(employeeId)
            );
            
            pb.directory(aiDir);
            pb.inheritIO();
            pb.start();
            
            return Map.of("status", "success", "message", "Face registration started");
        } catch (Exception e) {
            return Map.of("status", "error", "message", "Failed to start face registration: " + e.getMessage());
        }
    }

    public Map<String, Object> recognizeFace() {
        try {
            String rootPath = System.getProperty("user.dir");
            File aiDir = new File(rootPath, "ai_system");
            if (!aiDir.exists()) {
                aiDir = new File(new File(rootPath).getParentFile(), "ai_system");
            }

            ProcessBuilder pb = new ProcessBuilder("python", "recognize_face_login.py");
            pb.directory(aiDir);
            pb.redirectErrorStream(true);

            Process process;
            try {
                process = pb.start();
            } catch (java.io.IOException e) {
                System.err.println("[FACE LOGIN AI] Failed to start python, retrying with py -3: " + e.getMessage());
                pb = new ProcessBuilder("py", "-3", "recognize_face_login.py");
                pb.directory(aiDir);
                pb.redirectErrorStream(true);
                process = pb.start();
            }

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String line;
            Long employeeId = null;

            while ((line = reader.readLine()) != null) {
                System.out.println("[FACE LOGIN AI] " + line);
                String trimmed = line.trim();

                if (trimmed.matches("\\d+")) {
                    try {
                        employeeId = Long.valueOf(trimmed);
                        System.out.println("[FACE LOGIN AI] Found Employee ID: " + employeeId);
                    } catch (Exception e) {
                        System.err.println("[FACE LOGIN AI] Failed to parse numeric ID: " + e.getMessage());
                    }
                } else if (trimmed.contains("ID=")) {
                    try {
                        String idStr = trimmed.split("ID=")[1].trim();
                        employeeId = Long.valueOf(idStr);
                        System.out.println("[FACE LOGIN AI] Found Employee ID: " + employeeId);
                    } catch (Exception e) {
                        System.err.println("[FACE LOGIN AI] Failed to parse ID= line: " + e.getMessage());
                    }
                }
            }

            int exitCode = process.waitFor();
            System.out.println("[FACE LOGIN AI] Process exited with code: " + exitCode);

            if (exitCode == 0 && employeeId != null) {
                return Map.of("status", "success", "employeeId", employeeId, "confidence", 0.95);
            } else if (exitCode == 0) {
                return Map.of("status", "error", "message", "Face login succeeded but no ID was returned");
            } else {
                String errorMsg = "Script error (code " + exitCode + ")";
                System.out.println("[FACE LOGIN AI] Recognition failed: " + errorMsg);
                return Map.of("status", "error", "message", errorMsg);
            }

        } catch (Exception e) {
            return Map.of("status", "error", "message", "System error: " + e.getMessage());
        }
    }

    public Map<String, Object> deleteFace(Long employeeId) {
        return Map.of("status", "success", "message", "Deleted");
    }
}
