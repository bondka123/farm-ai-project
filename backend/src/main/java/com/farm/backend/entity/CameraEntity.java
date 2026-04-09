package com.farm.backend.entity;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "cameras")
public class CameraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String status;
    private String location;

    // 🔥 CORRECTION ICI
    @ManyToOne
    @JoinColumn(name = "department_id")
    @JsonIgnoreProperties({"cameras"}) // 🚀 coupe boucle
    private Department department;

    // ===== GETTERS =====
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }
    public Department getDepartment() { return department; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setStatus(String status) { this.status = status; }
    public void setLocation(String location) { this.location = location; }
    public void setDepartment(Department department) { this.department = department; }
}