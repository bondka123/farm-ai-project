package com.farm.backend.repository;

import com.farm.backend.entity.Employee;
import com.farm.backend.entity.EmployeeStatus;
import com.farm.backend.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByStatus(EmployeeStatus status);

    List<Employee> findByDepartmentId(Long departmentId);
    long countByDepartmentId(Long departmentId);

    List<Employee> findByFaceRegisteredFalse();

    List<Employee> findByDepartmentIdAndJob(Long departmentId, String job);

    List<Employee> findByAvailableTrue();

    List<Employee> findByJobAndAvailableTrue(Job job);

    List<Employee> findByJobAndDepartmentIsNullAndAvailableTrue(Job job);

    List<Employee> findByAvailableTrueAndDepartmentIsNull();

    List<Employee> findByJob(Job job);

    List<Employee> findByJobIn(List<Job> jobs);

    java.util.List<Employee> findByEmail(String email);
    List<Employee> findByDepartmentManagerId(Long managerId);

    List<Employee> findByJobAndAvailableTrueAndDepartmentIsNullAndFaceRegisteredTrueAndStatus(Job job, EmployeeStatus status);
}
