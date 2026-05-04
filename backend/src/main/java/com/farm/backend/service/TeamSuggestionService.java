package com.farm.backend.service;

import com.farm.backend.entity.Department;
import com.farm.backend.entity.DepartmentRequirement;
import com.farm.backend.entity.Employee;
import com.farm.backend.entity.EmployeeStatus;
import com.farm.backend.entity.Job;
import com.farm.backend.repository.DepartmentRepository;
import com.farm.backend.repository.DepartmentRequirementRepository;
import com.farm.backend.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TeamSuggestionService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRequirementRepository requirementRepository;

    public TeamSuggestionService(DepartmentRepository departmentRepository,
                                 EmployeeRepository employeeRepository,
                                 DepartmentRequirementRepository requirementRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.requirementRepository = requirementRepository;
    }

    public Map<String, List<Employee>> suggestTeam(Long departmentId) {
        Department dep = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));
        
        Map<String, List<Employee>> suggestion = new LinkedHashMap<>();

        // DOCTORS
        suggestion.put("DOCTOR", findAvailableEmployeesForJob(Job.DOCTOR)
                .stream().limit(Math.max(dep.getDoctors(), 0)).toList());

        // ELECTRICIANS
        suggestion.put("ELECTRICIAN", findAvailableEmployeesForJob(Job.ELECTRICIAN)
                .stream().limit(Math.max(dep.getElectricians(), 0)).toList());

        // WORKERS
        suggestion.put("WORKER", findAvailableEmployeesForJob(Job.WORKER)
                .stream().limit(Math.max(dep.getWorkers(), 0)).toList());

        return suggestion;
    }

    public List<Employee> findAvailableEmployeesForJob(Job job) {
        return employeeRepository.findByJobAndAvailableTrueAndDepartmentIsNullAndFaceRegisteredTrueAndStatus(job, EmployeeStatus.APPROVED);
    }

    public void validateAndAssignTeam(Long departmentId, List<Long> employeeIds) {
        Department dep = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("Department not found"));

        for (Long id : employeeIds) {
            Employee emp = employeeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Employee not found: " + id));

            if (!emp.isAvailable()) {
                throw new RuntimeException("Employee already assigned: " + emp.getName());
            }

            // 🔥 STRICT VALIDATION
            if (!emp.isFaceRegistered() || emp.getStatus() != EmployeeStatus.APPROVED) {
                throw new RuntimeException("Employee not validated: " + emp.getName());
            }

            emp.setDepartment(dep);
            emp.setAvailable(false);
            emp.setStatus(EmployeeStatus.APPROVED); // 🔥 Garder APPROVED pour éviter l'erreur MySQL ENUM si la table n'est pas à jour
            employeeRepository.save(emp);
        }
    }

    public void releaseEmployeesFromDepartment(Long departmentId) {
        List<Employee> employees = employeeRepository.findByDepartmentId(departmentId);
        for (Employee emp : employees) {
            emp.setDepartment(null);
            emp.setAvailable(true);
            emp.setStatus(EmployeeStatus.APPROVED);
            employeeRepository.save(emp);
        }
    }
}