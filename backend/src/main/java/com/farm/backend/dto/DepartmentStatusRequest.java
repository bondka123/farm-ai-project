package com.farm.backend.dto;

import java.util.Map;

public class DepartmentStatusRequest {
    private Long cameraId;
    private Map<String, Integer> counts; // medecin, worker, electricien
    private boolean unknown;
    private double timestamp;

    public Long getCameraId() { return cameraId; }
    public void setCameraId(Long cameraId) { this.cameraId = cameraId; }
    public Map<String, Integer> getCounts() { return counts; }
    public void setCounts(Map<String, Integer> counts) { this.counts = counts; }
    public boolean isUnknown() { return unknown; }
    public void setUnknown(boolean unknown) { this.unknown = unknown; }
    public double getTimestamp() { return timestamp; }
    public void setTimestamp(double timestamp) { this.timestamp = timestamp; }
}
