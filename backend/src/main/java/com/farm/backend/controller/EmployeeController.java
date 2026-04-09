package com.farm.backend.controller;

import com.farm.backend.entity.*;
import com.farm.backend.repository.*;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeController(EmployeeRepository employeeRepository,
                              DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    // 🔥 MANAGER ajoute (PENDING)
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/manager")
    public Employee createByManager(@RequestBody Employee employee) {

        Department dep = departmentRepository
                .findById(employee.getDepartment().getId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        employee.setDepartment(dep);
        employee.setStatus(EmployeeStatus.PENDING);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setFaceRegistered(false);

        return employeeRepository.save(employee);
    }

    // 🔥 ADMIN ajoute direct (APPROVED)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin")
    public Employee createByAdmin(@RequestBody Employee employee) {

        Department dep = departmentRepository
                .findById(employee.getDepartment().getId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        employee.setDepartment(dep);
        employee.setStatus(EmployeeStatus.APPROVED);
        employee.setCreatedAt(LocalDateTime.now());

        return employeeRepository.save(employee);
    }

    // 🔥 ADMIN valide employee
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/approve/{id}")
    public Employee approve(@PathVariable Long id) {

        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        emp.setStatus(EmployeeStatus.APPROVED);
        emp.setApprovedAt(LocalDateTime.now());

        return employeeRepository.save(emp);
    }

    // 🔥 GET ALL
    @GetMapping
    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    // 🔥 GET PENDING (admin)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public List<Employee> getPending() {
        return employeeRepository.findByStatus(EmployeeStatus.PENDING);
    }

    // 🔥 DELETE (admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        employeeRepository.deleteById(id);
        return "Employee deleted";
    }
}