package com.farm.backend.controller;

import com.farm.backend.entity.User;
import com.farm.backend.repository.UserRepository;
import com.farm.backend.service.FaceService;
import com.farm.backend.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class FaceAuthController {

    private final FaceService faceService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public FaceAuthController(FaceService faceService, UserRepository userRepository, JwtService jwtService) {
        this.faceService = faceService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * 🚀 Intelligent Face Login
     * Triggers recognition and returns JWT + role if successful.
     */
    @PostMapping("/face-login-trigger")
    public ResponseEntity<?> faceLoginTrigger() {
        try {
            // This currently uses the old method of opening camera on server
            // We will transition to frame-based streaming in the next steps
            Map<String, Object> recognition = faceService.recognizeFace();
            
            if (!"success".equals(recognition.get("status"))) {
                return ResponseEntity.status(401).body(Map.of("error", "Identification échouée"));
            }

            String email = (String) recognition.get("email");
            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Utilisateur non trouvé"));
            }

            User user = userOpt.get();
            
            if (!user.isFaceRegistered()) {
                return ResponseEntity.status(400).body(Map.of("error", "Visage non enregistré"));
            }

            // Generate JWT
            String token = jwtService.generateToken(user.getEmail(), user.getRole());

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole().name(),
                    "email", user.getEmail(),
                    "userId", user.getId(),
                    "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                    "lastName", user.getLastName() != null ? user.getLastName() : ""
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Erreur serveur : " + e.getMessage()));
        }
    }
}
