package com.workflow.entitlements.controller;

import com.workflow.entitlements.entity.User;
import com.workflow.entitlements.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    
    // Track active sessions (in production, use Redis or database)
    private Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    
    // Session info class
    public static class SessionInfo {
        private UUID userId;
        private String username;
        private Instant createdAt;
        private Instant lastAccessedAt;
        
        public SessionInfo(UUID userId, String username) {
            this.userId = userId;
            this.username = username;
            this.createdAt = Instant.now();
            this.lastAccessedAt = Instant.now();
        }
        
        public void updateLastAccessed() {
            this.lastAccessedAt = Instant.now();
        }
        
        public boolean isExpired(long sessionTimeoutMinutes) {
            return lastAccessedAt.plusSeconds(sessionTimeoutMinutes * 60).isBefore(Instant.now());
        }
        
        // Getters
        public UUID getUserId() { return userId; }
        public String getUsername() { return username; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastAccessedAt() { return lastAccessedAt; }
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");
        
        if (username == null || password == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Username and password are required"));
        }
        
        // Find user by username
        Optional<User> optionalUser = userService.findByUsername(username);
        
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "Invalid credentials"));
        }
        
        User user = optionalUser.get();
        
        // Check if user is active
        if (!user.getIsActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "User account is inactive"));
        }
        
        // For demo purposes, accept any password for existing users
        // In production, this would verify against a proper password hash
        
        // Generate session ID
        String sessionId = UUID.randomUUID().toString();
        
        // Store session (in production, use Redis or database)
        activeSessions.put(sessionId, new SessionInfo(user.getUserId(), user.getUsername()));
        
        // Create user info for response
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getUserId().toString()); // Convert UUID to string for JSON
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("isActive", user.getIsActive());
        userInfo.put("attributes", user.getGlobalAttributes());
        
        // Create response
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sessionId", sessionId);
        response.put("user", userInfo);
        response.put("message", "Login successful");
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestBody(required = false) Map<String, String> body) {
        
        // Remove session if provided in header
        if (sessionId != null) {
            activeSessions.remove(sessionId);
        }
        
        // Remove session if provided in body
        if (body != null && body.containsKey("sessionId")) {
            String bodySessionId = body.get("sessionId");
            activeSessions.remove(bodySessionId);
        }
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Logout successful"));
    }
    
    @GetMapping("/validate-session")
    public ResponseEntity<Map<String, Object>> validateSession(@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "No session ID provided"));
        }
        
        // Check if session exists
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "Invalid session"));
        }
        
        // Check if session is expired (30 minutes timeout)
        if (sessionInfo.isExpired(30)) {
            activeSessions.remove(sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "Session expired"));
        }
        
        // Update last accessed time
        sessionInfo.updateLastAccessed();
        
        // Verify user still exists and is active
        Optional<User> optionalUser = userService.findById(sessionInfo.getUserId());
        if (optionalUser.isEmpty() || !optionalUser.get().getIsActive()) {
            activeSessions.remove(sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "User no longer active"));
        }
        
        User user = optionalUser.get();
        
        // Create user info for response
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getUserId().toString()); // Convert UUID to string for JSON
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("isActive", user.getIsActive());
        userInfo.put("attributes", user.getGlobalAttributes());
        
        return ResponseEntity.ok(Map.of("success", true, "user", userInfo, "message", "Session is valid"));
    }
    
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "No session ID provided"));
        }
        
        // Check if session exists
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo == null || sessionInfo.isExpired(30)) {
            if (sessionInfo != null) activeSessions.remove(sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "Invalid or expired session"));
        }
        
        // Update last accessed time
        sessionInfo.updateLastAccessed();
        
        // Get user details
        Optional<User> optionalUser = userService.findById(sessionInfo.getUserId());
        if (optionalUser.isEmpty() || !optionalUser.get().getIsActive()) {
            activeSessions.remove(sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "User no longer active"));
        }
        
        User user = optionalUser.get();
        
        // Create user info for response
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getUserId().toString()); // Convert UUID to string for JSON
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("isActive", user.getIsActive());
        userInfo.put("attributes", user.getGlobalAttributes());
        
        return ResponseEntity.ok(Map.of("success", true, "user", userInfo));
    }
    
    // Backward compatibility endpoint for API Gateway
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateUser(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "No user ID provided"));
        }
        
        UUID userUuid;
        try {
            userUuid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", "Invalid user ID format"));
        }
        
        // Verify user exists and is active
        Optional<User> optionalUser = userService.findById(userUuid);
        if (optionalUser.isEmpty() || !optionalUser.get().getIsActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "User not found or inactive"));
        }
        
        User user = optionalUser.get();
        
        // Create user info for response
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getUserId().toString()); // Convert UUID to string for JSON
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("isActive", user.getIsActive());
        userInfo.put("attributes", user.getGlobalAttributes());
        
        return ResponseEntity.ok(Map.of("success", true, "user", userInfo, "message", "User is valid"));
    }
}