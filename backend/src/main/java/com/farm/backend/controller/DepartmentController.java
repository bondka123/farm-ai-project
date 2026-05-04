package com.farm.backend.controller;

import com.farm.backend.entity.Department;
import com.farm.backend.entity.DepartmentRequirement;
import com.farm.backend.entity.Role;
import com.farm.backend.entity.User;
import com.farm.backend.repository.DepartmentRepository;
import com.farm.backend.repository.DepartmentRequirementRepository;
import com.farm.backend.repository.UserRepository;
import com.farm.backend.service.CurrentUserService;
import com.farm.backend.service.EmailService;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final DepartmentRequirementRepository requirementRepository;
    private final CurrentUserService currentUserService;
    private final com.farm.backend.repository.EmployeeRepository employeeRepository;

    public DepartmentController(DepartmentRepository departmentRepository,
                                UserRepository userRepository,
                                EmailService emailService,
                                DepartmentRequirementRepository requirementRepository,
                                CurrentUserService currentUserService,
                                com.farm.backend.repository.EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.requirementRepository = requirementRepository;
        this.currentUserService = currentUserService;
        this.employeeRepository = employeeRepository;
    }

    // =========================
    // 🔓 PUBLIC
    // =========================
    @GetMapping("/public")
    public List<Department> getPublicDepartments() {
        List<Department> deps = departmentRepository.findAll();
        deps.forEach(d -> d.setAssignedEmployees(employeeRepository.countByDepartmentId(d.getId())));
        return deps;
    }

    // =========================
    // 🔒 CREATE
    // =========================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
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

        if (manager.getDepartment() != null) {
            throw new RuntimeException("Manager déjà affecté ❌");
        }

        department.setManager(manager);

        Department saved = departmentRepository.save(department);

        // 📧 EMAIL CREATE
        try {
            emailService.sendAssignment(
                    manager.getEmail(),
                    department.getName(),
                    department.getStartTime(),
                    department.getEndTime()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }

        return saved;
    }

    // =========================
    // 🔒 UPDATE
    // =========================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public Department updateDepartment(@PathVariable Long id,
                                       @RequestBody Department updated) {

        Department dep = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        dep.setName(updated.getName());
        dep.setStartTime(updated.getStartTime());
        dep.setEndTime(updated.getEndTime());
        dep.setDoctors(updated.getDoctors());
        dep.setElectricians(updated.getElectricians());
        dep.setWorkers(updated.getWorkers());

        if (updated.getManager() != null && updated.getManager().getId() != null) {

            User manager = userRepository.findById(updated.getManager().getId())
                    .orElseThrow(() -> new RuntimeException("Manager not found"));

            if (manager.getRole() != Role.ROLE_MANAGER) {
                throw new RuntimeException("User is not a MANAGER");
            }

            if (manager.getDepartment() != null &&
                !manager.getDepartment().getId().equals(dep.getId())) {

                throw new RuntimeException("Manager déjà utilisé ❌");
            }

            dep.setManager(manager);

            // 📧 EMAIL UPDATE
            try {
                emailService.sendAssignment(
                        manager.getEmail(),
                        dep.getName(),
                        dep.getStartTime(),
                        dep.getEndTime()
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return departmentRepository.save(dep);
    }

    // =========================
    // 🔒 GET ALL
    // =========================
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @GetMapping
    public List<Department> getAllDepartments() {
        List<Department> list;
        if (currentUserService.getCurrentUser().getRole() == Role.ROLE_MANAGER) {
            list = departmentRepository.findByManagerId(currentUserService.getCurrentUser().getId())
                    .map(List::of)
                    .orElse(List.of());
        } else {
            list = departmentRepository.findAll();
        }
        list.forEach(d -> d.setAssignedEmployees(employeeRepository.countByDepartmentId(d.getId())));
        return list;
    }

    // =========================
    // 🔒 DELETE
    // =========================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public String deleteDepartment(@PathVariable Long id) {

        Department dep = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        // 🔥 récupérer email AVANT suppression
        String email = null;
        String depName = dep.getName();

        if (dep.getManager() != null) {
            email = dep.getManager().getEmail();
        }

        departmentRepository.delete(dep);

        // 📧 EMAIL DELETE
        if (email != null) {
            try {
                emailService.sendDelete(email, depName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return "Department deleted successfully";
    }

    // =========================
    // REQUIREMENTS
    // =========================
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/{departmentId}/requirements")
    public DepartmentRequirement addRequirement(@PathVariable Long departmentId, @RequestBody DepartmentRequirement req) {
        Department dep = departmentRepository.findById(departmentId).orElseThrow(() -> new RuntimeException("Department not found"));
        req.setDepartment(dep);
        return requirementRepository.save(req);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{departmentId}/requirements")
    public List<DepartmentRequirement> getRequirements(@PathVariable Long departmentId) {
        return requirementRepository.findByDepartmentId(departmentId);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/requirements/{id}")
    public String deleteRequirement(@PathVariable Long id) {
        requirementRepository.deleteById(id);
        return "Requirement deleted";
    }
}