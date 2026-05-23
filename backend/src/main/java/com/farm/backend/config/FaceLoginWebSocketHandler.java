package com.farm.backend.config;

import com.farm.backend.entity.User;
import com.farm.backend.repository.UserRepository;
import com.farm.backend.service.FaceService;
import com.farm.backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

@Component
public class FaceLoginWebSocketHandler extends AbstractWebSocketHandler {

    private final FaceService faceService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);
    private final Map<String, Long> sessionCooldowns = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, java.util.List<String>> sessionConsecutiveMatches = new java.util.concurrent.ConcurrentHashMap<>();

    public FaceLoginWebSocketHandler(FaceService faceService, UserRepository userRepository, JwtService jwtService) {
        this.faceService = faceService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("WS - New face-login connection. Session: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        System.out.println("WS - Connection closed. Status: " + status + " | Session: " + session.getId());
        sessionConsecutiveMatches.remove(session.getId());
        sessionCooldowns.remove(session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        System.out.println("WS - Message received. Type: " + message.getClass().getSimpleName()
                + " | Size: " + message.getPayloadLength());
        super.handleMessage(session, message);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            Map<String, String> data = objectMapper.readValue(payload, Map.class);
            String action = data.get("action");
            System.out.println("WS - Action received: " + action);

            if ("identify".equals(action)) {
                String image = data.get("image");
                if (image == null || image.isEmpty()) {
                    return;
                }

                System.out.println("WS - Running face AI on image of " + image.length() + " bytes");
                Map<String, Object> recognition = faceService.identifyFaceFromImage(image);
                sendRecognitionResponse(session, recognition);
            }
        } catch (Exception e) {
            System.err.println("WS ERROR: " + e.getMessage());
            sendRecognitionError(session, "Erreur interne du serveur");
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        executor.submit(() -> {
            try {
                if (!session.isOpen()) {
                    return;
                }

                Long lastAttempt = sessionCooldowns.get(session.getId());
                if (lastAttempt != null && System.currentTimeMillis() - lastAttempt < 1000) {
                    return;
                }
                sessionCooldowns.put(session.getId(), System.currentTimeMillis());

                ByteBuffer payload = message.getPayload();
                int length = payload.remaining();
                byte[] imageBytes = new byte[length];
                payload.get(imageBytes);

                String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

                long startTime = System.currentTimeMillis();
                Map<String, Object> recognition = faceService.identifyFaceAsync(base64Image);
                long duration = System.currentTimeMillis() - startTime;

                if (recognition.containsKey("proc_ms")) {
                    System.out.println("FACE ANALYSIS: " + recognition.get("proc_ms")
                            + "ms (Python) | " + duration + "ms (Total)");
                }

                sendRecognitionResponse(session, recognition);
            } catch (Exception e) {
                System.err.println("WS ASYNC ERROR: " + e.getMessage());
            }
        });
    }

    private void sendRecognitionResponse(WebSocketSession session, Map<String, Object> recognition) throws Exception {
        String sessionId = session.getId();
        java.util.List<String> matches = sessionConsecutiveMatches.computeIfAbsent(
                sessionId,
                k -> new java.util.ArrayList<>()
        );

        if (!"success".equals(recognition.get("status"))) {
            matches.clear();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "status", "error",
                    "message", recognition.getOrDefault("message", "Face not recognized")
            ))));
            return;
        }

        String email = (String) recognition.get("email");
        System.out.println("WS - Recognized: " + email);

        if (!matches.isEmpty() && !matches.get(matches.size() - 1).equals(email)) {
            matches.clear();
        }
        matches.add(email);

        if (matches.size() < 5) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "status", "processing",
                    "message", "Face detected, confirming..."
            ))));
            return;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            matches.clear();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "status", "error",
                    "message", "User account not found"
            ))));
            return;
        }

        User user = userOpt.get();
        if (!user.isEnabled() || !user.isFaceRegistered()) {
            matches.clear();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "status", "error",
                    "message", !user.isEnabled() ? "Account not activated" : "Face not registered"
            ))));
            return;
        }

        System.out.println("WS - Authenticated user: " + email + " with role: " + user.getRole().name());
        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        Map<String, Object> response = Map.of(
                "status", "success",
                "token", token,
                "role", user.getRole().name(),
                "email", user.getEmail(),
                "firstName", user.getFirstName() != null ? user.getFirstName() : "",
                "faceRegistered", user.isFaceRegistered(),
                "userId", user.getId()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        matches.clear();
    }

    private void sendRecognitionError(WebSocketSession session, String message) throws Exception {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "status", "error",
                "message", message
        ))));
    }
}
