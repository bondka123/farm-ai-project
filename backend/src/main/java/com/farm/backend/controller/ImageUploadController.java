package com.farm.backend.controller;

import com.farm.backend.entity.CameraEntity;
import com.farm.backend.repository.CameraRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin
public class ImageUploadController {

    private final CameraRepository cameraRepository;

    public ImageUploadController(CameraRepository cameraRepository) {
        this.cameraRepository = cameraRepository;
    }

    private static final String UPLOAD_DIR =
            System.getProperty("user.dir") + "/uploads/";

    @PostMapping
    public String upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("cameraId") Long cameraId
    ) {
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(UPLOAD_DIR + filename);
            file.transferTo(dest);

            CameraEntity camera = cameraRepository.findById(cameraId)
                    .orElseThrow(() -> new RuntimeException("Camera not found"));

            // 🔥 Nettoyage de l'ancienne image pour économiser l'espace (Optionnel)
            if (camera.getLastImage() != null) {
                new File(UPLOAD_DIR + camera.getLastImage()).delete();
            }

            camera.setLastSeen(LocalDateTime.now());
            camera.setStatus("ACTIVE");
            camera.setLastImage(filename); // ✅ On stocke le nom

            cameraRepository.save(camera);
            System.out.println("📸 IMAGE → ACTIVE: " + camera.getName());

            return "OK";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }

    @GetMapping("/files/{filename}")
    @ResponseBody
    public byte[] getFile(@PathVariable String filename) throws Exception {
        File file = new File(UPLOAD_DIR + filename);
        if (!file.exists()) return new byte[0];
        return java.nio.file.Files.readAllBytes(file.toPath());
    }
}