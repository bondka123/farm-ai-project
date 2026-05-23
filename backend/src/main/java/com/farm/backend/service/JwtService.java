package com.farm.backend.service;

import com.farm.backend.config.JwtUtil;
import com.farm.backend.entity.Role;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    /**
     * Generate a JWT for the given user email and role.
     * The role is stored without the "ROLE_" prefix; the prefix is added lazily in the filter.
     */
    public String generateToken(String email, Role role) {
        return JwtUtil.generateToken(email, role.name());
    }

    /** Validate token and return true if still valid. */
    public boolean isValid(String token) {
        return JwtUtil.isValid(token);
    }

    /** Extract username (email) from token. */
    public String getUsername(String token) {
        return JwtUtil.getUsername(token);
    }

    /** Extract raw role name from token (e.g., ADMIN, MANAGER, VIEWER). */
    public String getRole(String token) {
        return JwtUtil.getRole(token);
    }
}
