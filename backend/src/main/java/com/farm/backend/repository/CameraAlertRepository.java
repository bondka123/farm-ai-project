package com.farm.backend.repository;

import com.farm.backend.entity.CameraAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CameraAlertRepository extends JpaRepository<CameraAlert, Long> {
    List<CameraAlert> findByCameraIdOrderByTimestampDesc(Long cameraId);
    List<CameraAlert> findByDepartmentIdOrderByTimestampDesc(Long departmentId);
}
