package com.farm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_name")
    private String employeeName;

    private String status;

    private boolean unknown;

    @Column(name = "image_path")
    private String imagePath;

    private LocalDateTime timestamp;

    // 👉 auto date
    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    // ===== GETTERS =====

    public Long getId() {
        return id;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getStatus() {
        return status;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public String getImagePath() {
        return imagePath;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    // ===== SETTERS =====

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUnknown(boolean unknown) {
        this.unknown = unknown;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}