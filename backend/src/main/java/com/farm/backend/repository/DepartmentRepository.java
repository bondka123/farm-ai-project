package com.farm.backend.repository;

import com.farm.backend.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByNameContainingIgnoreCase(String name);

    List<Department> findByManagerUsernameContainingIgnoreCase(String username);
}