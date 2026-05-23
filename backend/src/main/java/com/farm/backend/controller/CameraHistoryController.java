package com.farm.backend.controller;

import com.farm.backend.entity.CameraEvent;
import com.farm.backend.repository.CameraEventRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/camera-events")
@CrossOrigin
public class CameraHistoryController {

    private final CameraEventRepository repository;

    public CameraHistoryController(CameraEventRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}/history")
    public List<CameraEvent> getCameraHistory(@PathVariable Long id) {
        return repository.findByCameraIdOrderByTimestampDesc(id);
    }
}
