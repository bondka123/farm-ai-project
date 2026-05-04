package com.farm.backend.controller;

import com.farm.backend.entity.Role;
import com.farm.backend.entity.User;
import com.farm.backend.exception.FaceException;
import com.farm.backend.repository.UserRepository;
import com.farm.backend.service.FaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/face")
@CrossOrigin
public class FaceController {

    private final FaceService faceService;
    private final UserRepository userRepository;

    public FaceController(FaceService faceService, UserRepository userRepository) {
        this.faceService = faceService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> registerFace(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        try {
            faceService.registerFace(userId);
            return ResponseEntity.ok(Map.of("message", "Visage enregistré avec succès"));
        } catch (FaceException e) {
            System.err.println("FACE REGISTRATION ERROR: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateFace(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        try {
            faceService.updateFace(userId);
            return ResponseEntity.ok(Map.of("message", "Visage mis à jour avec succès"));
        } catch (FaceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteFace(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        try {
            faceService.deleteFace(userId);
            return ResponseEntity.ok(Map.of("message", "Visage supprimé"));
        } catch (FaceException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getStatus(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        boolean registered = faceService.getFaceStatus(userId);
        return ResponseEntity.ok(Map.of("faceRegistered", registered));
    }

    @GetMapping("/status/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStatusForUser(@PathVariable Long userId) {
        boolean registered = faceService.getFaceStatus(userId);
        return ResponseEntity.ok(Map.of("faceRegistered", registered));
    }

    private Long getUserIdFromAuth(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new FaceException("Utilisateur non trouvé"));
    }
}

