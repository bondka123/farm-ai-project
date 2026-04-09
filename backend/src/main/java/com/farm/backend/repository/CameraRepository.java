package com.farm.backend.repository;

import com.farm.backend.entity.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraRepository extends JpaRepository<CameraEntity, Long> {
}