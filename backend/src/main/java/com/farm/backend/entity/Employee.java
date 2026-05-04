package com.farm.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Enumerated(EnumType.STRING)
    private Job job;

    private String email;
    private String phone;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = true)
    @JsonIgnoreProperties({"cameras", "manager", "employees", "requirements"})
    private Department department;

    @Enumerated(EnumType.STRING)
    private EmployeeStatus status;

    private String photoPath;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    private boolean faceRegistered = false;

    // 🔥 AJOUT IMPORTANT
    private boolean available = true;

    private LocalDateTime createdAt;

    // ===== GETTERS =====
    public Long getId() { return id; }
    public String getName() { return name; }
    public Job getJob() { return job; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Department getDepartment() { return department; }
    public EmployeeStatus getStatus() { return status; }
    public boolean isFaceRegistered() { return faceRegistered; }
    public boolean isAvailable() { return available; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setJob(Job job) { this.job = job; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setDepartment(Department department) { this.department = department; }
    public void setStatus(EmployeeStatus status) { this.status = status; }
    public void setFaceRegistered(boolean faceRegistered) { this.faceRegistered = faceRegistered; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}