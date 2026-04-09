package com.farm.backend.controller;

import com.farm.backend.entity.Department;
import com.farm.backend.entity.Role;
import com.farm.backend.entity.User;
import com.farm.backend.repository.DepartmentRepository;
import com.farm.backend.repository.UserRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DepartmentController(DepartmentRepository departmentRepository,
                                UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    // =========================
    // 🔓 PUBLIC (Python / AI)
    // =========================
    @GetMapping("/public")
    public List<Department> getPublicDepartments() {
        return departmentRepository.findAll();
    }

    // =========================
    // 🔒 CREATE (ADMIN)
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Department createDepartment(@RequestBody Department department) {

        if (department.getManager() == null || department.getManager().getId() == null) {
            throw new RuntimeException("Manager is required");
        }

        User manager = userRepository.findById(department.getManager().getId())
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (manager.getRole() != Role.ROLE_MANAGER) {
            throw new RuntimeException("Selected user is not a MANAGER");
        }

        department.setManager(manager);

        return departmentRepository.save(department);
    }

    // =========================
    // 🔒 GET ALL
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    // =========================
    // 🔒 GET ONE
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public Department getOne(@PathVariable Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));
    }

    // =========================
    // 🔒 UPDATE
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Department updateDepartment(@PathVariable Long id,
                                       @RequestBody Department updated) {

        Department dep = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // update fields
        dep.setName(updated.getName());
        dep.setStartTime(updated.getStartTime());
        dep.setEndTime(updated.getEndTime());
        dep.setDoctors(updated.getDoctors());
        dep.setElectricians(updated.getElectricians());
        dep.setWorkers(updated.getWorkers());

        // update manager (optional)
        if (updated.getManager() != null && updated.getManager().getId() != null) {

            User manager = userRepository.findById(updated.getManager().getId())
                    .orElseThrow(() -> new RuntimeException("Manager not found"));

            if (manager.getRole() != Role.ROLE_MANAGER) {
                throw new RuntimeException("User is not a MANAGER");
            }

            dep.setManager(manager);
        }

        return departmentRepository.save(dep);
    }

    // =========================
    // 🔒 DELETE
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteDepartment(@PathVariable Long id) {

        Department dep = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        departmentRepository.delete(dep);

        return "Department deleted successfully";
    }

    // =========================
    // 🔍 SEARCH BY NAME
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search")
    public List<Department> searchByName(@RequestParam String name) {
        return departmentRepository.findByNameContainingIgnoreCase(name);
    }

    // =========================
    // 🔍 SEARCH BY MANAGER NAME
    // =========================
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/search/manager")
    public List<Department> searchByManager(@RequestParam String username) {
        return departmentRepository.findByManagerUsernameContainingIgnoreCase(username);
    }
}