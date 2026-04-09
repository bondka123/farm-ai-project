package com.farm.backend.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/department-status")
@CrossOrigin
public class DepartmentStatusController {

    @PostMapping
    public String receiveStatus(@RequestBody Map<String, Object> data) {

        System.out.println("📡 DATA FROM AI: " + data);

        return "OK";
    }
}