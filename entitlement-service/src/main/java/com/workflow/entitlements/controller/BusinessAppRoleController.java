package com.workflow.entitlements.controller;

import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.entity.User;
import com.workflow.entitlements.entity.UserBusinessAppRole;
import com.workflow.entitlements.service.BusinessAppRoleService;
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
 * REST Controller for Business Application Role management operations.
 * Now uses BusinessAppRoleService instead of direct repository access.
 */
@RestController
@RequestMapping("/api/entitlements/business-app-roles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class BusinessAppRoleController {

    private final BusinessAppRoleService businessAppRoleService;

    /**
     * Get all business app roles with optional pagination
     */
    @GetMapping
    public ResponseEntity<List<BusinessAppRole>> getAllBusinessAppRoles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            if (page < 0 || size <= 0) {
                // If invalid pagination, return all active roles
                List<BusinessAppRole> roles = businessAppRoleService.getAllActiveBusinessAppRoles();
                return ResponseEntity.ok(roles);
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<BusinessAppRole> rolePage = businessAppRoleService.getAllBusinessAppRoles(pageable);
            return ResponseEntity.ok(rolePage.getContent());
            
        } catch (Exception e) {
            log.error("Error retrieving all business app roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get business app role by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessAppRole> getBusinessAppRoleById(@PathVariable Long id) {
        try {
            Optional<BusinessAppRole> role = businessAppRoleService.findById(id);
            return role.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving business app role by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get business app role by role name and application
     */
    @GetMapping("/application/{applicationId}/role/{roleName}")
    public ResponseEntity<BusinessAppRole> getBusinessAppRoleByNameAndApplication(
            @PathVariable Long applicationId, @PathVariable String roleName) {
        try {
            Optional<BusinessAppRole> role = businessAppRoleService.findByRoleNameAndApplication(roleName, applicationId);
            return role.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving business app role by name and application: {}, {}", roleName, applicationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get roles for specific application
     */
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<BusinessAppRole>> getRolesByApplication(@PathVariable Long applicationId) {
        try {
            List<BusinessAppRole> roles = businessAppRoleService.getRolesByApplication(applicationId);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error retrieving roles for application: {}", applicationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active roles for specific application
     */
    @GetMapping("/application/{applicationId}/active")
    public ResponseEntity<List<BusinessAppRole>> getActiveRolesByApplication(@PathVariable Long applicationId) {
        try {
            List<BusinessAppRole> roles = businessAppRoleService.getActiveRolesByApplication(applicationId);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error retrieving active roles for application: {}", applicationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all active business app roles
     */
    @GetMapping("/active")
    public ResponseEntity<List<BusinessAppRole>> getActiveBusinessAppRoles() {
        try {
            List<BusinessAppRole> activeRoles = businessAppRoleService.getAllActiveBusinessAppRoles();
            return ResponseEntity.ok(activeRoles);
        } catch (Exception e) {
            log.error("Error retrieving active business app roles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get users assigned to role
     */
    @GetMapping("/{id}/users")
    public ResponseEntity<List<User>> getRoleUsers(@PathVariable Long id) {
        try {
            List<User> users = businessAppRoleService.getRoleUsers(id);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error retrieving users for role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get role statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getBusinessAppRoleStats() {
        try {
            long totalRoles = businessAppRoleService.getTotalBusinessAppRoleCount();
            long activeRoles = businessAppRoleService.getActiveBusinessAppRoleCount();
            
            var stats = java.util.Map.of(
                "totalRoles", totalRoles,
                "activeRoles", activeRoles,
                "inactiveRoles", totalRoles - activeRoles
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving business app role statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new business app role
     */
    @PostMapping
    public ResponseEntity<Object> createBusinessAppRole(@RequestBody BusinessAppRole role) {
        try {
            BusinessAppRole savedRole = businessAppRoleService.createBusinessAppRole(role);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRole);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid business app role creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (NoSuchElementException e) {
            log.warn("Business application not found for role creation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error creating business app role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to create business app role"));
        }
    }

    /**
     * Update existing business app role
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBusinessAppRole(@PathVariable Long id, @RequestBody BusinessAppRole roleDetails) {
        try {
            BusinessAppRole updatedRole = businessAppRoleService.updateBusinessAppRole(id, roleDetails);
            return ResponseEntity.ok(updatedRole);
            
        } catch (NoSuchElementException e) {
            log.warn("Business app role not found for update: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid business app role update request for {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error updating business app role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to update business app role"));
        }
    }

    /**
     * Soft delete business app role (deactivate)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusinessAppRole(@PathVariable Long id) {
        try {
            businessAppRoleService.deleteBusinessAppRole(id);
            return ResponseEntity.noContent().build();
            
        } catch (NoSuchElementException e) {
            log.warn("Business app role not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting business app role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate business app role
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Object> activateBusinessAppRole(@PathVariable Long id) {
        try {
            BusinessAppRole activatedRole = businessAppRoleService.activateBusinessAppRole(id);
            return ResponseEntity.ok(activatedRole);
            
        } catch (NoSuchElementException e) {
            log.warn("Business app role not found for activation: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error activating business app role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to activate business app role"));
        }
    }

    /**
     * Deactivate business app role
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Object> deactivateBusinessAppRole(@PathVariable Long id) {
        try {
            BusinessAppRole deactivatedRole = businessAppRoleService.deactivateBusinessAppRole(id);
            return ResponseEntity.ok(deactivatedRole);
            
        } catch (NoSuchElementException e) {
            log.warn("Business app role not found for deactivation: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deactivating business app role: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to deactivate business app role"));
        }
    }

    /**
     * Assign role to user
     */
    @PostMapping("/{roleId}/assign")
    public ResponseEntity<Object> assignRoleToUser(@PathVariable Long roleId, @RequestParam UUID userId) {
        try {
            UserBusinessAppRole assignment = businessAppRoleService.assignRoleToUser(userId, roleId);
            return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
            
        } catch (NoSuchElementException e) {
            log.warn("User or role not found for assignment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error assigning role {} to user {}", roleId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to assign role to user"));
        }
    }

    /**
     * Remove role from user
     */
    @DeleteMapping("/{roleId}/assign")
    public ResponseEntity<Object> removeRoleFromUser(@PathVariable Long roleId, @RequestParam UUID userId) {
        try {
            businessAppRoleService.removeRoleFromUser(userId, roleId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            log.error("Error removing role {} from user {}", roleId, userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to remove role from user"));
        }
    }

    /**
     * Check if business app role exists
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Object> checkBusinessAppRoleExists(@PathVariable Long id) {
        try {
            boolean exists = businessAppRoleService.existsById(id);
            return ResponseEntity.ok(java.util.Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking if business app role exists: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to check business app role existence"));
        }
    }
}