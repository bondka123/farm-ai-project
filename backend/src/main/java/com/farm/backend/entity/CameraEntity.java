package com.farm.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@Entity
@Table(name = "cameras")
public class CameraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String type;   // HTTP / RTSP / LOCAL
    private String source; // URL

    private String status; // ACTIVE / OFF
    private String location;

    private LocalDateTime lastSeen;
    private String lastImage;

    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({"cameras"})
    private Department department;

    public CameraEntity() {
        this.status = "OFF";
    }

    // GETTERS
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }
    public Department getDepartment() { return department; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public String getLastImage() { return lastImage; }

    // SETTERS
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setSource(String source) { this.source = source; }
    public void setStatus(String status) { this.status = status; }
    public void setLocation(String location) { this.location = location; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    public void setDepartment(Department department) { this.department = department; }
    public void setLastImage(String lastImage) { this.lastImage = lastImage; }
}