package com.workflow.entitlements.controller;

import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.entity.UserBusinessAppRole;
import com.workflow.entitlements.service.UserBusinessAppRoleService;
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
 * REST Controller for User Business Application Role management operations.
 * Now uses UserBusinessAppRoleService instead of direct repository access.
 */
@RestController
@RequestMapping("/api/entitlements/user-business-app-roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class UserBusinessAppRoleController {

    private final UserBusinessAppRoleService userBusinessAppRoleService;

    /**
     * Get all user business app roles with optional pagination
     */
    @GetMapping
    public ResponseEntity<List<UserBusinessAppRole>> getAllUserBusinessAppRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            if (page < 0 || size <= 0) {
                // If invalid pagination, return all active assignments
                List<UserBusinessAppRole> userRoles = userBusinessAppRoleService.getAllActiveUserBusinessAppRoles();
                return ResponseEntity.ok(userRoles);
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<UserBusinessAppRole> userRolePage = userBusinessAppRoleService.getAllUserBusinessAppRoles(pageable);
            return ResponseEntity.ok(userRolePage.getContent());
            
        } catch (Exception e) {
            log.error("Error retrieving all user business app roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user business app role by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserBusinessAppRole> getUserBusinessAppRoleById(@PathVariable Long id) {
        try {
            Optional<UserBusinessAppRole> userRole = userBusinessAppRoleService.findById(id);
            return userRole.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving user business app role by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user roles by user ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserBusinessAppRole>> getUserRolesByUserId(@PathVariable UUID userId) {
        try {
            List<UserBusinessAppRole> userRoles = userBusinessAppRoleService.getUserRolesByUserId(userId);
            return ResponseEntity.ok(userRoles);
        } catch (Exception e) {
            log.error("Error retrieving roles for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active user roles by user ID
     */
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<UserBusinessAppRole>> getActiveUserRolesByUserId(@PathVariable UUID userId) {
        try {
            List<UserBusinessAppRole> activeUserRoles = userBusinessAppRoleService.getActiveUserRolesByUserId(userId);
            return ResponseEntity.ok(activeUserRoles);
        } catch (Exception e) {
            log.error("Error retrieving active roles for user: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get users by role ID
     */
    @GetMapping("/role/{roleId}")
    public ResponseEntity<List<UserBusinessAppRole>> getUsersByRoleId(@PathVariable Long roleId) {
        try {
            List<UserBusinessAppRole> userRoles = userBusinessAppRoleService.getUsersByRoleId(roleId);
            return ResponseEntity.ok(userRoles);
        } catch (Exception e) {
            log.error("Error retrieving users for role: {}", roleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active users by role ID
     */
    @GetMapping("/role/{roleId}/active")
    public ResponseEntity<List<UserBusinessAppRole>> getActiveUsersByRoleId(@PathVariable Long roleId) {
        try {
            List<UserBusinessAppRole> activeUserRoles = userBusinessAppRoleService.getActiveUsersByRoleId(roleId);
            return ResponseEntity.ok(activeUserRoles);
        } catch (Exception e) {
            log.error("Error retrieving active users for role: {}", roleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user business app role by user and role ID
     */
    @GetMapping("/user/{userId}/role/{roleId}")
    public ResponseEntity<UserBusinessAppRole> getUserBusinessAppRoleByUserAndRole(
            @PathVariable UUID userId, @PathVariable Long roleId) {
        try {
            Optional<UserBusinessAppRole> userRole = userBusinessAppRoleService.findByUserIdAndRoleId(userId, roleId);
            return userRole.map(ResponseEntity::ok)
                          .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving user business app role for user {} and role {}", userId, roleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all active user business app roles
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserBusinessAppRole>> getActiveUserBusinessAppRoles() {
        try {
            List<UserBusinessAppRole> activeUserRoles = userBusinessAppRoleService.getAllActiveUserBusinessAppRoles();
            return ResponseEntity.ok(activeUserRoles);
        } catch (Exception e) {
            log.error("Error retrieving active user business app roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user business app role statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getUserBusinessAppRoleStats() {
        try {
            long totalAssignments = userBusinessAppRoleService.getTotalUserBusinessAppRoleCount();
            long activeAssignments = userBusinessAppRoleService.getActiveUserBusinessAppRoleCount();
            
            var stats = java.util.Map.of(
                "totalAssignments", totalAssignments,
                "activeAssignments", activeAssignments,
                "inactiveAssignments", totalAssignments - activeAssignments
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving user business app role statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new user business app role
     */
    @PostMapping
    public ResponseEntity<Object> createUserBusinessAppRole(@RequestBody UserBusinessAppRole userBusinessAppRole) {
        try {
            UserBusinessAppRole savedUserRole = userBusinessAppRoleService.createUserBusinessAppRole(userBusinessAppRole);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUserRole);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user business app role creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (NoSuchElementException e) {
            log.warn("User or role not found for assignment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error creating user business app role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to create user business app role"));
        }
    }

    /**
     * Update existing user business app role
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUserBusinessAppRole(@PathVariable Long id, @RequestBody UserBusinessAppRole userRoleDetails) {
        try {
            UserBusinessAppRole updatedUserRole = userBusinessAppRoleService.updateUserBusinessAppRole(id, userRoleDetails);
            return ResponseEntity.ok(updatedUserRole);
            
        } catch (NoSuchElementException e) {
            log.warn("User business app role not found for update: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user business app role update request for {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error updating user business app role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to update user business app role"));
        }
    }

    /**
     * Delete user business app role
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserBusinessAppRole(@PathVariable Long id) {
        try {
            userBusinessAppRoleService.deleteUserBusinessAppRole(id);
            return ResponseEntity.noContent().build();
            
        } catch (NoSuchElementException e) {
            log.warn("User business app role not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting user business app role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate user business app role
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Object> activateUserBusinessAppRole(@PathVariable Long id) {
        try {
            UserBusinessAppRole activatedUserRole = userBusinessAppRoleService.activateUserBusinessAppRole(id);
            return ResponseEntity.ok(activatedUserRole);
            
        } catch (NoSuchElementException e) {
            log.warn("User business app role not found for activation: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error activating user business app role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to activate user business app role"));
        }
    }

    /**
     * Deactivate user business app role
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Object> deactivateUserBusinessAppRole(@PathVariable Long id) {
        try {
            UserBusinessAppRole deactivatedUserRole = userBusinessAppRoleService.deactivateUserBusinessAppRole(id);
            return ResponseEntity.ok(deactivatedUserRole);
            
        } catch (NoSuchElementException e) {
            log.warn("User business app role not found for deactivation: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deactivating user business app role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to deactivate user business app role"));
        }
    }

    /**
     * Assign role to user
     */
    @PostMapping("/assign")
    public ResponseEntity<Object> assignRoleToUser(@RequestParam UUID userId, @RequestParam Long roleId) {
        try {
            UserBusinessAppRole assignment = userBusinessAppRoleService.assignRoleToUser(userId, roleId);
            return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
            
        } catch (NoSuchElementException e) {
            log.warn("User or role not found for assignment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role assignment request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error assigning role {} to user {}", roleId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to assign role to user"));
        }
    }

    /**
     * Revoke role from user
     */
    @PostMapping("/revoke")
    public ResponseEntity<Object> revokeRoleFromUser(@RequestParam UUID userId, @RequestParam Long roleId) {
        try {
            userBusinessAppRoleService.revokeRoleFromUser(userId, roleId);
            return ResponseEntity.noContent().build();
            
        } catch (NoSuchElementException e) {
            log.warn("User business app role assignment not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role revocation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error revoking role {} from user {}", roleId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to revoke role from user"));
        }
    }

    /**
     * Check if user business app role exists
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Object> checkUserBusinessAppRoleExists(@PathVariable Long id) {
        try {
            boolean exists = userBusinessAppRoleService.existsById(id);
            return ResponseEntity.ok(java.util.Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking if user business app role exists: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to check user business app role existence"));
        }
    }
}