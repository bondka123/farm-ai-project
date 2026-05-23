package com.farm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String message;
    private String severity;

    private Long cameraId;
    private Long departmentId;
    
    private Boolean resolved = false;
    private String location;
    private String imagePath;
    private String uniqueHash;
    private Long employeeId;
    private Integer count = 1;

    // 🔥 IMPORTANT → pas stocké en base (juste pour affichage)
    @Transient
    private String departmentName;

    private LocalDateTime timestamp;

    // =========================
    // GETTERS
    // =========================
    public Long getId() { return id; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public String getSeverity() { return severity; }
    public Long getCameraId() { return cameraId; }
    public Long getDepartmentId() { return departmentId; }
    public Boolean isResolved() { return resolved; }
    public String getLocation() { return location; }
    public String getImagePath() { return imagePath; }
    public String getUniqueHash() { return uniqueHash; }
    public Long getEmployeeId() { return employeeId; }
    public Integer getCount() { return count; }
    public String getDepartmentName() { return departmentName; }
    public LocalDateTime getTimestamp() { return timestamp; }

    // =========================
    // SETTERS
    // =========================
    public void setId(Long id) { this.id = id; }
    public void setType(String type) { this.type = type; }
    public void setMessage(String message) { this.message = message; }
    public void setSeverity(String severity) { this.severity = severity; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }
    public void setLocation(String location) { this.location = location; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setUniqueHash(String uniqueHash) { this.uniqueHash = uniqueHash; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public void setCount(Integer count) { this.count = count; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    // 🔥 TRÈS IMPORTANT (SINON RIEN NE S’AFFICHE)
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }
}