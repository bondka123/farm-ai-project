package com.farm.backend.repository;

import com.farm.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByNameContainingIgnoreCase(String name);

    List<Department> findByManagerEmailContainingIgnoreCase(String email);

    Optional<Department> findByManagerId(Long managerId);

    Optional<Department> findByManagerEmail(String email);
}
