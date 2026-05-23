package com.farm.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CameraRequest {

    private String name;
    private String aiType;
    private String url;
    private String location;
    private Long departmentId;
    private Integer linePosition;
    private Boolean aiEnabled;


    public CameraRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAiType() {
        return aiType;
    }

    @JsonAlias({"type"})
    public void setAiType(String aiType) {
        this.aiType = aiType;
    }

    public String getUrl() {
        return url;
    }

    @JsonAlias({"source"})
    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Integer getLinePosition() {
        return linePosition;
    }

    public void setLinePosition(Integer linePosition) {
        this.linePosition = linePosition;
    }

    public Boolean getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(Boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }
}
