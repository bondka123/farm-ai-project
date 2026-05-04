package com.farm.backend.dto;

public class AIDetectionDTO {
    private String type;
    private String location;
    private String timestamp;
    private String imageBase64;
    private String embeddingHash;
    private Long employeeId;
    
    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getEmbeddingHash() { return embeddingHash; }
    public void setEmbeddingHash(String embeddingHash) { this.embeddingHash = embeddingHash; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
}
