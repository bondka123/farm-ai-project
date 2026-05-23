package com.farm.backend.repository;

import com.farm.backend.entity.FaceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface FaceEventRepository extends JpaRepository<FaceEvent, Long> {
    List<FaceEvent> findByCameraIdAndTimestampBetweenOrderByTimestampDesc(Long cameraId, LocalDateTime start, LocalDateTime end);
    List<FaceEvent> findByCameraIdOrderByTimestampDesc(Long cameraId);
}
