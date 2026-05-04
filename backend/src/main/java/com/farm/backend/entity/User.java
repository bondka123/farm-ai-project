package com.farm.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users")
public class User {

    // =========================
    // 🔥 ID
    // =========================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // 🔥 INFOS
    // =========================
    private String firstName;
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String password;

    // =========================
    // 🔥 ROLE
    // =========================
    @Enumerated(EnumType.STRING)
    private Role role;

    // =========================
    // 🔥 IA / SYSTEM
    // =========================
    private boolean faceRegistered = false;

    // 🔥 activation compte (viewer)
    private boolean enabled = true;

    private String activationToken;

    // =========================
    // 🔥 RELATION
    // =========================
    @OneToOne(mappedBy = "manager")
    @JsonIgnore
    private Department department;

    // =========================
    // 🔥 GETTERS
    // =========================
    public Long getId() { return id; }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public String getEmail() { return email; }

    public String getPhone() { return phone; }

    public String getPassword() { return password; }

    public Role getRole() { return role; }

    public boolean isFaceRegistered() { return faceRegistered; }

    public boolean isEnabled() { return enabled; }

    public String getActivationToken() { return activationToken; }

    public Department getDepartment() { return department; }

    // =========================
    // 🔥 SETTERS
    // =========================
    public void setId(Long id) { this.id = id; }

    public void setFirstName(String firstName) { this.firstName = firstName; }

    public void setLastName(String lastName) { this.lastName = lastName; }

    public void setEmail(String email) { this.email = email; }

    public void setPhone(String phone) { this.phone = phone; }

    public void setPassword(String password) { this.password = password; }

    public void setRole(Role role) { this.role = role; }

    public void setFaceRegistered(boolean faceRegistered) { this.faceRegistered = faceRegistered; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public void setActivationToken(String activationToken) { this.activationToken = activationToken; }

    public void setDepartment(Department department) { this.department = department; }
}