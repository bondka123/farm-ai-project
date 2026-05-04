package com.farm.backend.controller;

import com.farm.backend.entity.Alert;
import com.farm.backend.entity.CameraEntity;
import com.farm.backend.entity.Department;
import com.farm.backend.entity.DepartmentRequirement;
import com.farm.backend.entity.Employee;
import com.farm.backend.entity.Job;
import com.farm.backend.repository.AlertRepository;
import com.farm.backend.repository.CameraRepository;
import com.farm.backend.repository.DepartmentRequirementRepository;
import com.farm.backend.repository.EmployeeRepository;
import com.farm.backend.service.CurrentUserService;
import com.farm.backend.service.TeamSuggestionService;
import com.farm.backend.service.EmailService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/manager")
@CrossOrigin
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final CurrentUserService currentUserService;
    private final EmployeeRepository employeeRepository;
    private final CameraRepository cameraRepository;
    private final AlertRepository alertRepository;
    private final TeamSuggestionService teamSuggestionService;
    private final DepartmentRequirementRepository requirementRepository;
    private final EmailService emailService;

    public ManagerController(CurrentUserService currentUserService,
                             EmployeeRepository employeeRepository,
                             CameraRepository cameraRepository,
                             AlertRepository alertRepository,
                             TeamSuggestionService teamSuggestionService,
                             DepartmentRequirementRepository requirementRepository,
                             EmailService emailService) {
        this.currentUserService = currentUserService;
        this.employeeRepository = employeeRepository;
        this.cameraRepository = cameraRepository;
        this.alertRepository = alertRepository;
        this.teamSuggestionService = teamSuggestionService;
        this.requirementRepository = requirementRepository;
        this.emailService = emailService;
    }

    @GetMapping("/department")
    public Map<String, Object> getMyDepartment() {
        Department department = currentUserService.getCurrentManagerDepartment();
        return Map.of(
                "id", department.getId(),
                "name", department.getName(),
                "doctors", department.getDoctors(),
                "electricians", department.getElectricians(),
                "workers", department.getWorkers(),
                "assignedEmployees", employeeRepository.countByDepartmentId(department.getId())
        );
    }

    @GetMapping("/employees")
    public List<Employee> getMyEmployees() {
        Department department = currentUserService.getCurrentManagerDepartment();
        return employeeRepository.findByDepartmentId(department.getId());
    }

    @GetMapping("/cameras")
    public List<CameraEntity> getMyCameras() {
        Department department = currentUserService.getCurrentManagerDepartment();
        return cameraRepository.findByDepartmentId(department.getId());
    }

    @GetMapping("/alerts")
    public List<Alert> getMyAlerts() {
        Department department = currentUserService.getCurrentManagerDepartment();
        return alertRepository.findByDepartmentIdOrderByTimestampDesc(department.getId());
    }

    @GetMapping("/suggest-team/{departmentId}")
    public Map<String, List<Employee>> suggestTeam(@PathVariable Long departmentId) {
        Department managerDepartment = currentUserService.getCurrentManagerDepartment();
        if (!managerDepartment.getId().equals(departmentId)) {
            throw new RuntimeException("Managers can only suggest teams for their own department");
        }

        return teamSuggestionService.suggestTeam(departmentId);
    }

    @GetMapping("/team-candidates")
    public Map<String, List<Employee>> getTeamCandidates() {
        Department dep = currentUserService.getCurrentManagerDepartment();
        Map<String, List<Employee>> candidates = new LinkedHashMap<>();

        candidates.put("DOCTOR", teamSuggestionService.findAvailableEmployeesForJob(Job.DOCTOR));
        candidates.put("ELECTRICIAN", teamSuggestionService.findAvailableEmployeesForJob(Job.ELECTRICIAN));
        candidates.put("WORKER", teamSuggestionService.findAvailableEmployeesForJob(Job.WORKER));

        return candidates;
    }

    @GetMapping("/available-employees")
    public Map<String, Object> getAvailableEmployees() {
        List<Employee> allAvailable = employeeRepository.findByAvailableTrueAndDepartmentIsNull();
        
        long pendingCount = allAvailable.stream()
                .filter(e -> e.getStatus() == com.farm.backend.entity.EmployeeStatus.PENDING)
                .count();

        Map<String, List<Employee>> byJob = allAvailable.stream()
                .collect(Collectors.groupingBy(
                        employee -> employee.getJob() != null ? employee.getJob().name() : "UNKNOWN",
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return Map.of(
                "candidates", byJob,
                "pendingCount", pendingCount
        );
    }

    @PostMapping("/confirm-team")
    public Map<String, Object> confirmTeam(@RequestBody ConfirmTeamRequest request) {
        Department managerDepartment = currentUserService.getCurrentManagerDepartment();
        if (request.departmentId == null || !managerDepartment.getId().equals(request.departmentId)) {
            throw new RuntimeException("Managers can only confirm teams for their own department");
        }

        if (request.employees == null || request.employees.isEmpty()) {
            throw new RuntimeException("No employees selected");
        }

        List<Employee> employees = employeeRepository.findAllById(request.employees);
        if (employees.size() != request.employees.size()) {
            throw new RuntimeException("One or more employees were not found");
        }

        employees.forEach(employee -> {
            if (!employee.isAvailable()
                    && (employee.getDepartment() == null || !managerDepartment.getId().equals(employee.getDepartment().getId()))) {
                throw new RuntimeException("Employee " + employee.getId() + " is not available");
            }
        });

        employees.forEach(employee -> {
            employee.setDepartment(managerDepartment);
            employee.setAvailable(false);
        });

        employeeRepository.saveAll(employees);

        String managerName = managerDepartment.getManager() != null
                ? managerDepartment.getManager().getFirstName() + " " + managerDepartment.getManager().getLastName()
                : "Manager";

        LocalTime start = managerDepartment.getStartTime();
        LocalTime end = managerDepartment.getEndTime();
        String workSchedule = (start != null && end != null)
                ? start + " - " + end
                : "To be defined";

        employees.forEach(employee -> {
            emailService.sendAssignmentNotification(
                    employee.getEmail(),
                    employee.getName(),
                    managerDepartment.getName(),
                    managerName,
                    workSchedule
            );
        });

        return Map.of(
                "message", "Team confirmed",
                "departmentId", managerDepartment.getId(),
                "employeeCount", employees.size()
        );
    }

    public static class ConfirmTeamRequest {
        public Long departmentId;
        public List<Long> employees;
    }
}
