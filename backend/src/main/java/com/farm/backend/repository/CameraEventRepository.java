package com.farm.backend.repository;

import com.farm.backend.entity.CameraEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CameraEventRepository extends JpaRepository<CameraEvent, Long> {
    List<CameraEvent> findByCameraIdOrderByTimestampDesc(Long cameraId);
    void deleteByCameraId(Long cameraId);
}
