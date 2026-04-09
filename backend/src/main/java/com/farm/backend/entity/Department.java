package com.farm.backend.entity;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalTime startTime;
    private LocalTime endTime;

    private int doctors;
    private int electricians;
    private int workers;

    // 🔗 manager
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    // 🔥🔥🔥 CORRECTION ICI
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL)
    @JsonIgnore   // 🚀 IMPORTANT → coupe la boucle
    private List<CameraEntity> cameras;

    // ===== GETTERS =====
    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getDoctors() { return doctors; }
    public int getElectricians() { return electricians; }
    public int getWorkers() { return workers; }
    public User getManager() { return manager; }
    public List<CameraEntity> getCameras() { return cameras; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setDoctors(int doctors) { this.doctors = doctors; }
    public void setElectricians(int electricians) { this.electricians = electricians; }
    public void setWorkers(int workers) { this.workers = workers; }
    public void setManager(User manager) { this.manager = manager; }
    public void setCameras(List<CameraEntity> cameras) { this.cameras = cameras; }
}