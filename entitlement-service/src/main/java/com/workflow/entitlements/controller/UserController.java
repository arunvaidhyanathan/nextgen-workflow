package com.workflow.entitlements.controller;

import com.workflow.entitlements.entity.User;
import com.workflow.entitlements.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for User management operations.
 * Now uses UserService instead of direct repository access.
 */
@RestController
@RequestMapping("/api/entitlements/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Get all users with optional pagination
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            if (page < 0 || size <= 0) {
                // If invalid pagination, return all active users
                List<User> users = userService.getAllActiveUsers();
                return ResponseEntity.ok(users);
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<User> userPage = userService.getAllUsers(pageable);
            return ResponseEntity.ok(userPage.getContent());
            
        } catch (Exception e) {
            log.error("Error retrieving all users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        try {
            Optional<User> user = userService.findById(id);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving user by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        try {
            Optional<User> user = userService.findByUsername(username);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving user by username: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by email
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        try {
            Optional<User> user = userService.findByEmail(email);
            return user.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving user by email: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all active users
     */
    @GetMapping("/active")
    public ResponseEntity<List<User>> getActiveUsers() {
        try {
            List<User> activeUsers = userService.getAllActiveUsers();
            return ResponseEntity.ok(activeUsers);
        } catch (Exception e) {
            log.error("Error retrieving active users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search users by keyword
     */
    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String q) {
        try {
            List<User> users = userService.searchUsers(q);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error searching users with query: {}", q, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getUserStats() {
        try {
            long totalUsers = userService.getTotalUserCount();
            long activeUsers = userService.getActiveUserCount();
            
            var stats = java.util.Map.of(
                "totalUsers", totalUsers,
                "activeUsers", activeUsers,
                "inactiveUsers", totalUsers - activeUsers
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving user statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new user
     */
    @PostMapping
    public ResponseEntity<Object> createUser(@RequestBody User user) {
        try {
            User savedUser = userService.createUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to create user"));
        }
    }

    /**
     * Update existing user
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUser(@PathVariable UUID id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
            
        } catch (NoSuchElementException e) {
            log.warn("User not found for update: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user update request for {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error updating user: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to update user"));
        }
    }

    /**
     * Soft delete user (deactivate)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
            
        } catch (NoSuchElementException e) {
            log.warn("User not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting user: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate user
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Object> activateUser(@PathVariable UUID id) {
        try {
            User activatedUser = userService.activateUser(id);
            return ResponseEntity.ok(activatedUser);
            
        } catch (NoSuchElementException e) {
            log.warn("User not found for activation: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error activating user: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to activate user"));
        }
    }

    /**
     * Deactivate user
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Object> deactivateUser(@PathVariable UUID id) {
        try {
            User deactivatedUser = userService.deactivateUser(id);
            return ResponseEntity.ok(deactivatedUser);
            
        } catch (NoSuchElementException e) {
            log.warn("User not found for deactivation: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deactivating user: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to deactivate user"));
        }
    }

    /**
     * Check if user exists
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Object> checkUserExists(@PathVariable UUID id) {
        try {
            boolean exists = userService.existsById(id);
            return ResponseEntity.ok(java.util.Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking if user exists: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to check user existence"));
        }
    }

    /**
     * Check if username is available
     */
    @GetMapping("/username/{username}/available")
    public ResponseEntity<Object> checkUsernameAvailable(@PathVariable String username) {
        try {
            Optional<User> existingUser = userService.findByUsername(username);
            boolean available = existingUser.isEmpty();
            return ResponseEntity.ok(java.util.Map.of("available", available));
        } catch (Exception e) {
            log.error("Error checking username availability: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to check username availability"));
        }
    }

    /**
     * Check if email is available
     */
    @GetMapping("/email/{email}/available")
    public ResponseEntity<Object> checkEmailAvailable(@PathVariable String email) {
        try {
            Optional<User> existingUser = userService.findByEmail(email);
            boolean available = existingUser.isEmpty();
            return ResponseEntity.ok(java.util.Map.of("available", available));
        } catch (Exception e) {
            log.error("Error checking email availability: {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to check email availability"));
        }
    }
}