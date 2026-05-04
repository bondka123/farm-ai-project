package com.farm.backend.repository;

import com.farm.backend.entity.User;
import com.farm.backend.entity.Role;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    // 🔍 LOGIN
    Optional<User> findByEmail(String email);

    // 🔍 CHECK EMAIL
    boolean existsByEmail(String email);

    // 🔍 BY ROLE
    List<User> findByRole(Role role);

    // 🔥 MANAGERS DISPONIBLES
    List<User> findByRoleAndDepartmentIsNull(Role role);

    // 🔥 MANAGERS AVEC FACE
    List<User> findByRoleAndFaceRegisteredTrue(Role role);

    // 🔥 ACTIVATION VIEWER
    Optional<User> findByActivationToken(String token);
}