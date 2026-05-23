package com.farm.backend.repository;

import com.farm.backend.entity.RoleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface RoleEventRepository extends JpaRepository<RoleEvent, Long> {
    List<RoleEvent> findByCameraIdAndTimestampBetweenOrderByTimestampDesc(Long cameraId, LocalDateTime start, LocalDateTime end);
    List<RoleEvent> findByCameraIdOrderByTimestampDesc(Long cameraId);
}
