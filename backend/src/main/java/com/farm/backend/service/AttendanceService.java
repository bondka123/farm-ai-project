package com.farm.backend.service;

import com.farm.backend.dto.AttendanceRequest;
import com.farm.backend.entity.AttendanceRecord;
import com.farm.backend.repository.AttendanceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository repository;

    public AttendanceService(AttendanceRepository repository) {
        this.repository = repository;
    }

    // 🔥 SAVE
    public AttendanceRecord saveAttendance(AttendanceRequest request) {

        System.out.println("DATA RECUE: " + request.getEmployeeName());

        AttendanceRecord record = new AttendanceRecord();

        record.setEmployeeName(request.getEmployeeName());
        record.setStatus(request.getStatus());
        record.setUnknown(request.isUnknown());
        record.setImagePath(request.getImagePath());

        return repository.save(record);
    }

    // 🔥 GET ALL
    public List<AttendanceRecord> getAllAttendances() {
        return repository.findAll();
    }
}