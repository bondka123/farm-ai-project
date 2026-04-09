package com.farm.backend.controller;

import com.farm.backend.dto.AttendanceRequest;
import com.farm.backend.entity.AttendanceRecord;
import com.farm.backend.service.AttendanceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    // 🔥 POST (Python → Backend)
    @PostMapping
    public AttendanceRecord createAttendance(@RequestBody AttendanceRequest request) {
        return service.saveAttendance(request);
    }

    // 🔥 GET
    @GetMapping
    public List<AttendanceRecord> getAll() {
        return service.getAllAttendances();
    }
}