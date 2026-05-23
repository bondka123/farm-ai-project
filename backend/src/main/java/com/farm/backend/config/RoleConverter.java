package com.farm.backend.config;

import com.farm.backend.entity.Role;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoleConverter implements AttributeConverter<Role, String> {

    @Override
    public String convertToDatabaseColumn(Role role) {
        if (role == null) return null;
        return role.name();
    }

    @Override
    public Role convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        String clean = dbData.trim().toUpperCase();

        switch (clean) {
            case "ROLE_ADMIN":
            case "ADMIN":
                return Role.ROLE_ADMIN;
            case "ROLE_MANAGER":
            case "MANAGER":
                return Role.ROLE_MANAGER;
            case "ROLE_VIEWER":
            case "VIEWER":
                return Role.ROLE_VIEWER;
            case "ROLE_EMPLOYEE":
            case "EMPLOYEE":
                return Role.ROLE_EMPLOYEE;
            default:
                try {
                    return Role.valueOf(clean);
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }
}
