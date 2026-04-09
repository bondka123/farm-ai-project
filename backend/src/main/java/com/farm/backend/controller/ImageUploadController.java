package com.farm.backend.controller;

import com.farm.backend.entity.Department;
import com.farm.backend.entity.User;
import com.farm.backend.repository.DepartmentRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin
public class ImageUploadController {

    private final DepartmentRepository departmentRepository;

    public ImageUploadController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping
    public String upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("departmentId") Long departmentId
    ) {
        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) dir.mkdirs();

            String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(UPLOAD_DIR + filename);

            file.transferTo(dest);

            System.out.println("📸 IMAGE SAVED: " + filename);

            Department dept = departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));

            User manager = dept.getManager();

            System.out.println("🏢 Department: " + dept.getName());
            System.out.println("👤 Manager: " + manager.getUsername());

            return "OK";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}