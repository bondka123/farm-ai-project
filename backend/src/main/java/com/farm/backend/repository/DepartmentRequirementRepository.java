package com.farm.backend.repository;

import com.farm.backend.entity.DepartmentRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartmentRequirementRepository extends JpaRepository<DepartmentRequirement, Long> {
    List<DepartmentRequirement> findByDepartmentId(Long departmentId);
}