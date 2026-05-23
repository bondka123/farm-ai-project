package com.farm.backend.repository;

import com.farm.backend.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findFirstByUniqueHashAndResolvedFalseOrderByTimestampDesc(String uniqueHash);
    List<Alert> findByDepartmentIdOrderByTimestampDesc(Long departmentId);
    void deleteByCameraId(Long cameraId);
}
