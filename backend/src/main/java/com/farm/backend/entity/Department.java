package com.farm.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "department")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalTime startTime;
    private LocalTime endTime;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private User manager;

    private int doctors;
    private int electricians;
    private int workers;

    @Transient
    private long assignedEmployees;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<CameraEntity> cameras;

    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DepartmentRequirement> requirements;

    @OneToMany(mappedBy = "department")
    @JsonIgnore
    private List<Employee> employees;

    // ===== GETTERS =====
    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public User getManager() { return manager; }
    public List<CameraEntity> getCameras() { return cameras; }
    public List<DepartmentRequirement> getRequirements() { return requirements; }
    public List<Employee> getEmployees() { return employees; }
    public long getAssignedEmployees() { return assignedEmployees; }
    public void setAssignedEmployees(long assignedEmployees) { this.assignedEmployees = assignedEmployees; }

    public int getDoctors() { return doctors; }
    public void setDoctors(int doctors) { this.doctors = doctors; }
    public int getElectricians() { return electricians; }
    public void setElectricians(int electricians) { this.electricians = electricians; }
    public int getWorkers() { return workers; }
    public void setWorkers(int workers) { this.workers = workers; }


    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setManager(User manager) { this.manager = manager; }
    public void setCameras(List<CameraEntity> cameras) { this.cameras = cameras; }
    public void setRequirements(List<DepartmentRequirement> requirements) { this.requirements = requirements; }
    public void setEmployees(List<Employee> employees) { this.employees = employees; }

}
