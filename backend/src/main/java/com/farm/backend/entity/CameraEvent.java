package com.farm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "camera_events")
public class CameraEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "camera_id")
    private CameraEntity camera;

    private String type; // ENTRY, EXIT, ALERT, UNKNOWN
    private String employeeName;
    private Long employeeId;
    
    private String message;
    private String imagePath;
    
    private LocalDateTime timestamp;

    public CameraEvent() {
        this.timestamp = LocalDateTime.now();
    }

    // GETTERS & SETTERS
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CameraEntity getCamera() { return camera; }
    public void setCamera(CameraEntity camera) { this.camera = camera; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
