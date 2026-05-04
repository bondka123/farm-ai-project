package com.farm.backend.config;

import com.farm.backend.entity.Role;
import com.farm.backend.entity.User;
import com.farm.backend.repository.UserRepository;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class AdminInitializer {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public AdminInitializer(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @PostConstruct
    public void init() {
        repo.findByEmail("admin@farm.com").ifPresentOrElse(
            admin -> {
                admin.setPassword(encoder.encode("admin123"));
                admin.setEnabled(true);
                admin.setRole(Role.ROLE_ADMIN);
                repo.save(admin);
                System.out.println("✅ ADMIN PASSWORD RESET");
            },
            () -> {
                User admin = new User();
                admin.setEmail("admin@farm.com");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole(Role.ROLE_ADMIN);
                admin.setEnabled(true);
                repo.save(admin);
                System.out.println("✅ ADMIN CREATED");
            }
        );
    }
}