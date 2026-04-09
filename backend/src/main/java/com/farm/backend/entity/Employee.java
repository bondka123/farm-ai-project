package com.farm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String job;

    // 🔗 relation avec department
    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // 🔐 validation admin
    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    // 📸 photo + IA
    private String photoPath;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    private boolean faceRegistered = false;

    // 🧾 tracking
    private String createdBy;
    private String approvedBy;

    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;

    private boolean available = true;

    // ===== GETTERS =====
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getJob() { return job; }
    public Department getDepartment() { return department; }
    public EmployeeStatus getStatus() { return status; }
    public String getPhotoPath() { return photoPath; }
    public String getEmbedding() { return embedding; }
    public boolean isFaceRegistered() { return faceRegistered; }
    public String getCreatedBy() { return createdBy; }
    public String getApprovedBy() { return approvedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public boolean isAvailable() { return available; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setJob(String job) { this.job = job; }
    public void setDepartment(Department department) { this.department = department; }
    public void setStatus(EmployeeStatus status) { this.status = status; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public void setFaceRegistered(boolean faceRegistered) { this.faceRegistered = faceRegistered; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public void setAvailable(boolean available) { this.available = available; }
}