package com.farm.backend.service;

import com.farm.backend.entity.Employee;
import com.farm.backend.entity.User;
import com.farm.backend.exception.FaceException;
import com.farm.backend.repository.EmployeeRepository;
import com.farm.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

@Service
public class FaceService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public FaceService(EmployeeRepository employeeRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void registerFace(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FaceException("Utilisateur non trouvé"));

        String output = runPythonScript("face_register.py", userId.toString());
        
        if (output == null || !output.startsWith("[")) {
            throw new FaceException("Erreur lors de l'enregistrement du visage : sortie invalide");
        }

        // Save to User
        user.setEmbedding(output);
        user.setFaceRegistered(true);
        userRepository.save(user);

        // Sync to Employee if exists (for attendance/tracking)
        employeeRepository.findByEmail(user.getEmail()).stream().findFirst().ifPresent(employee -> {
            employee.setEmbedding(output);
            employee.setFaceRegistered(true);
            employeeRepository.save(employee);
        });
    }

    @Transactional
    public void updateFace(Long userId) {
        // Update is essentially the same as register in this context
        registerFace(userId);
    }

    @Transactional
    public void deleteFace(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new FaceException("Utilisateur non trouvé"));

        Employee employee = employeeRepository.findByEmail(user.getEmail()).stream().findFirst()
                .orElseThrow(() -> new FaceException("Employé non trouvé pour cet utilisateur"));

        employee.setEmbedding(null);
        employee.setFaceRegistered(false);
        employeeRepository.save(employee);

        user.setFaceRegistered(false);
        userRepository.save(user);
    }

    public boolean getFaceStatus(Long userId) {
        return userRepository.findById(userId)
                .map(User::isFaceRegistered)
                .orElse(false);
    }

    @Transactional
    public void registerFaceByEmployeeId(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new FaceException("Employé non trouvé"));

        User user = userRepository.findByEmail(employee.getEmail())
                .orElseThrow(() -> new FaceException("Utilisateur non trouvé pour cet employé"));

        registerFace(user.getId());
    }

    @Transactional
    public void deleteFaceByEmployeeId(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new FaceException("Employé non trouvé"));

        User user = userRepository.findByEmail(employee.getEmail())
                .orElseThrow(() -> new FaceException("Utilisateur non trouvé pour cet employé"));

        deleteFace(user.getId());
    }

    public Map<String, Object> recognizeFace() {
        String output = runPythonScript("recognize_face_login.py", null);

        if (output == null || output.trim().isEmpty() || output.equals("NO_MATCH") || output.equals("ERROR_CAMERA")) {
            throw new FaceException("Face login failed: identification échouée");
        }

        // Output is now an email (since we updated recognize_face_login.py)
        return Map.of("status", "success", "email", output.trim());
    }

    private String runPythonScript(String scriptName, String arg) {
        try {
            String rootPath = System.getProperty("user.dir");
            File aiDir = new File(rootPath, "ai_system");
            
            if (!aiDir.exists()) {
                aiDir = new File(new File(rootPath).getParentFile(), "ai_system");
            }
            
            System.out.println("DEBUG: AI Directory determined as: " + aiDir.getAbsolutePath());
            File scriptFile = new File(aiDir, scriptName);
            if (!scriptFile.exists()) {
                System.err.println("ERROR: Script not found at " + scriptFile.getAbsolutePath());
                return null;
            }

            ProcessBuilder pb;
            if (arg != null) {
                pb = new ProcessBuilder("python", "-u", scriptFile.getAbsolutePath(), arg);
            } else {
                pb = new ProcessBuilder("python", "-u", scriptFile.getAbsolutePath());
            }
            
            pb.directory(aiDir);
            // pb.redirectErrorStream(true);

            Process process = pb.start();
            
            StringBuilder stderr = new StringBuilder();
            new Thread(() -> {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        System.err.println("PYTHON [stderr] " + scriptName + ": " + line);
                        stderr.append(line).append("\n");
                    }
                } catch (Exception e) {}
            }).start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String lastLine = null;
            while ((line = reader.readLine()) != null) {
                System.out.println("PYTHON [stdout] " + scriptName + ": " + line);
                if (!line.trim().isEmpty()) {
                    lastLine = line;
                }
            }
            
            int exitCode = process.waitFor();
            System.out.println("PYTHON EXIT CODE [" + scriptName + "]: " + exitCode);

            if (exitCode != 0) {
                throw new FaceException("Python script failed with exit code " + exitCode + ". Stderr: " + stderr.toString());
            }

            return lastLine;
        } catch (FaceException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error running python script " + scriptName + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

