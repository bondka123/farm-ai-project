package com.farm.backend.controller;

import com.farm.backend.entity.Employee;
import com.farm.backend.entity.Department;
import com.farm.backend.service.TeamSuggestionService;
import com.farm.backend.service.EmailService;
import com.farm.backend.repository.EmployeeRepository;
import com.farm.backend.repository.DepartmentRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team")
@CrossOrigin
public class TeamController {

    private final TeamSuggestionService teamSuggestionService;
    private final EmailService emailService;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final com.farm.backend.repository.UserRepository userRepository;

    public TeamController(TeamSuggestionService teamSuggestionService,
                          EmailService emailService,
                          EmployeeRepository employeeRepository,
                          DepartmentRepository departmentRepository,
                          com.farm.backend.repository.UserRepository userRepository) {
        this.teamSuggestionService = teamSuggestionService;
        this.emailService = emailService;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @GetMapping("/proposal/{departmentId}")
    public Map<String, List<Employee>> getProposal(@PathVariable Long departmentId) {
        return teamSuggestionService.suggestTeam(departmentId);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_MANAGER')")
    @PostMapping("/validate")
    public String validateTeam(@RequestBody TeamRequest request) {
        // 🔥 SECURITY: Only Admin can re-validate or modify an existing team
        Department dep = departmentRepository.findById(request.getDepartmentId()).orElse(null);
        if (dep != null && dep.getEmployees() != null && !dep.getEmployees().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                throw new RuntimeException("Seul l'administrateur peut modifier une équipe déjà validée.");
            }
        }

        teamSuggestionService.validateAndAssignTeam(request.getDepartmentId(), request.getEmployees());
        
        // Send emails
        sendEmails(request.getDepartmentId(), request.getEmployees());
        
        return "Team validated successfully";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/update")
    public String updateTeam(@RequestBody TeamRequest request) {
        // 1. Release previous employees
        teamSuggestionService.releaseEmployeesFromDepartment(request.getDepartmentId());
        
        // 2. Assign new ones
        teamSuggestionService.validateAndAssignTeam(request.getDepartmentId(), request.getEmployees());
        
        // 3. Send update emails
        sendUpdateEmails(request.getDepartmentId(), request.getEmployees());
        
        return "Team updated successfully";
    }

    private void sendEmails(Long departmentId, List<Long> employeeIds) {
        Department dep = departmentRepository.findById(departmentId).orElse(null);
        if (dep == null) return;

        List<Employee> assignedEmployees = employeeRepository.findAllById(employeeIds);
        List<String> employeeNames = assignedEmployees.stream().map(Employee::getName).toList();
        String schedule = dep.getStartTime() + " - " + dep.getEndTime();
        String managerName = dep.getManager() != null ? dep.getManager().getFirstName() + " " + dep.getManager().getLastName() : "Manager";

        // 📧 EMAIL 2 -> ADMIN
        userRepository.findByRole(com.farm.backend.entity.Role.ROLE_ADMIN).stream()
                .findFirst()
                .ifPresent(admin -> emailService.sendTeamValidationToAdmin(
                        admin.getEmail(),
                        managerName,
                        dep.getName(),
                        employeeNames,
                        schedule
                ));

        // 📧 EMAIL 3 -> MANAGER
        if (dep.getManager() != null) {
            emailService.sendManagerConfirmation(
                    dep.getManager().getEmail(),
                    dep.getName(),
                    employeeNames
            );
        }

        // 📧 EMAIL 4 -> EMPLOYEES
        for (Employee emp : assignedEmployees) {
            emailService.sendAssignmentNotification(
                    emp.getEmail(),
                    emp.getName(),
                    dep.getName(),
                    managerName,
                    schedule
            );
        }
    }

    private void sendUpdateEmails(Long departmentId, List<Long> employeeIds) {
        Department dep = departmentRepository.findById(departmentId).orElse(null);
        if (dep == null) return;

        for (Long id : employeeIds) {
            Employee emp = employeeRepository.findById(id).orElse(null);
            if (emp != null) {
                emailService.sendAssignmentUpdated(
                        emp.getEmail(),
                        emp.getName(),
                        dep.getName()
                );
            }
        }
    }

    public static class TeamRequest {
        private Long departmentId;
        private List<Long> employees;

        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public List<Long> getEmployees() { return employees; }
        public void setEmployees(List<Long> employees) { this.employees = employees; }
    }
}
