package com.farm.backend.config;

import com.farm.backend.entity.User;
import com.farm.backend.entity.Role;
import com.farm.backend.repository.UserRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

	@Bean
	CommandLineRunner initAdmin(UserRepository repo, PasswordEncoder encoder) {
	    return args -> {

	        if (repo.findByUsername("admin").isEmpty()) {

	            User admin = new User();
	            admin.setUsername("admin");
	            admin.setPassword(encoder.encode("123456"));
	            admin.setRole(Role.ROLE_ADMIN);

	            repo.save(admin);

	            System.out.println("ADMIN CREATED");
	        }
	    };
	
	
    }
}