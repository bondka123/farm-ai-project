package com.farm.backend.repository;

import com.farm.backend.entity.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CameraRepository extends JpaRepository<CameraEntity, Long> {
    List<CameraEntity> findByDepartmentId(Long departmentId);
}
