package com.farm.backend.service;

import com.farm.backend.entity.Department;
import com.farm.backend.entity.User;
import com.farm.backend.repository.DepartmentRepository;
import com.farm.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    public CurrentUserService(UserRepository userRepository,
                              DepartmentRepository departmentRepository) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Authenticated user not found");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    public Department getCurrentManagerDepartment() {
        User currentUser = getCurrentUser();
        return departmentRepository.findByManagerId(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Manager has no department assigned"));
    }
}
