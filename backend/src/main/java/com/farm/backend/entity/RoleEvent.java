package com.farm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "role_events")
public class RoleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "camera_id")
    private CameraEntity camera;

    private String status; // SAFE / ALERT / CRITICAL
    private String details;
    
    @ElementCollection
    @CollectionTable(name = "role_event_counts", joinColumns = @JoinColumn(name = "role_event_id"))
    @MapKeyColumn(name = "role_name")
    @Column(name = "count")
    private Map<String, Integer> counts;

    private String imagePath;
    private LocalDateTime timestamp;

    public RoleEvent() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CameraEntity getCamera() { return camera; }
    public void setCamera(CameraEntity camera) { this.camera = camera; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public Map<String, Integer> getCounts() { return counts; }
    public void setCounts(Map<String, Integer> counts) { this.counts = counts; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
