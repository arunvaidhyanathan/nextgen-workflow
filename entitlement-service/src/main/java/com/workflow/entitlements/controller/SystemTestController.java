package com.workflow.entitlements.controller;

import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.entity.User;
import com.workflow.entitlements.entity.UserBusinessAppRole;
import com.workflow.entitlements.service.HybridAuthorizationService;
import com.workflow.entitlements.service.UserService;
import com.workflow.entitlements.service.UserBusinessAppRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * System test controller for validating hybrid schema functionality.
 * This controller provides endpoints to test the complete hybrid authorization system.
 */
@RestController
@RequestMapping("/api/system-test")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class SystemTestController {

    private final UserService userService;
    private final UserBusinessAppRoleService userBusinessAppRoleService;
    private final HybridAuthorizationService hybridAuthorizationService;

    /**
     * Test hybrid schema with UUID user operations
     */
    @GetMapping("/users/uuid-test")
    public ResponseEntity<Object> testUuidUsers() {
        try {
            log.info("Testing hybrid schema with UUID users");
            
            // Test finding users by various criteria
            List<User> allUsers = userService.getAllActiveUsers();
            
            // Test UUID-based lookups
            Optional<User> aliceUser = userService.findByUsername("alice.intake");
            Optional<User> bobUser = userService.findByUsername("bob.investigator");
            
            Map<String, Object> result = Map.of(
                "totalActiveUsers", allUsers.size(),
                "aliceFound", aliceUser.isPresent(),
                "bobFound", bobUser.isPresent(),
                "aliceUserId", aliceUser.map(u -> u.getUserId().toString()).orElse("not found"),
                "bobUserId", bobUser.map(u -> u.getUserId().toString()).orElse("not found"),
                "sampleUser", aliceUser.orElse(null)
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error testing UUID users", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test user-role assignments with UUID
     */
    @GetMapping("/user-roles/uuid-test")
    public ResponseEntity<Object> testUuidUserRoles() {
        try {
            log.info("Testing user-role assignments with UUID");
            
            // Find Alice's user ID
            Optional<User> alice = userService.findByUsername("alice.intake");
            if (alice.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "Alice user not found"));
            }
            
            UUID aliceId = alice.get().getUserId();
            
            // Test UUID-based user role queries
            List<UserBusinessAppRole> aliceRoles = userBusinessAppRoleService.getUserRolesByUserId(aliceId);
            List<UserBusinessAppRole> aliceActiveRoles = userBusinessAppRoleService.getActiveUserRolesByUserId(aliceId);
            
            Map<String, Object> result = Map.of(
                "aliceUserId", aliceId.toString(),
                "totalRoles", aliceRoles.size(),
                "activeRoles", aliceActiveRoles.size(),
                "roleDetails", aliceActiveRoles.stream()
                    .map(role -> Map.of(
                        "roleName", role.getBusinessAppRole().getRoleName(),
                        "applicationName", role.getBusinessAppRole().getBusinessApplication().getBusinessAppName(),
                        "isActive", role.getIsActive()
                    ))
                    .toList()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error testing UUID user roles", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Test authorization engine switching
     */
    @PostMapping("/authorization/engine-test")
    public ResponseEntity<Object> testAuthorizationEngine(@RequestBody Map<String, Object> testRequest) {
        try {
            log.info("Testing authorization engine functionality");
            
            // Get test parameters
            String username = (String) testRequest.getOrDefault("username", "alice.intake");
            String resource = (String) testRequest.getOrDefault("resource", "case");
            String action = (String) testRequest.getOrDefault("action", "create");
            
            // Find user
            Optional<User> user = userService.findByUsername(username);
            if (user.isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "User not found: " + username));
            }
            
            // Build authorization request (simplified)
            AuthorizationCheckRequest.Principal principal = AuthorizationCheckRequest.Principal.builder()
                .id(user.get().getUserId())
                .attributes(user.get().getGlobalAttributes())
                .build();
                
            AuthorizationCheckRequest.Resource resourceObj = AuthorizationCheckRequest.Resource.builder()
                .kind(resource)
                .id("test-" + System.currentTimeMillis())
                .attributes(Map.of("department", "IU", "priority", "HIGH"))
                .build();
                
            AuthorizationCheckRequest authRequest = AuthorizationCheckRequest.builder()
                .principal(principal)
                .resource(resourceObj)
                .action(action)
                .build();
            
            // Test authorization
            AuthorizationCheckResponse authResponse = hybridAuthorizationService.checkAuthorization(authRequest);
            
            Map<String, Object> result = Map.of(
                "username", username,
                "userId", user.get().getUserId().toString(),
                "resource", resource,
                "action", action,
                "authorizationResult", Map.of(
                    "allowed", authResponse.isAllowed(),
                    "message", authResponse.getMessage(),
                    "validationResult", authResponse.getValidationResult() != null ? authResponse.getValidationResult() : "none"
                )
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error testing authorization engine", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get system status and configuration
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getSystemStatus() {
        try {
            long totalUsers = userService.getTotalUserCount();
            long activeUsers = userService.getActiveUserCount();
            long totalRoleAssignments = userBusinessAppRoleService.getTotalUserBusinessAppRoleCount();
            long activeRoleAssignments = userBusinessAppRoleService.getActiveUserBusinessAppRoleCount();
            
            Map<String, Object> status = Map.of(
                "hybridSchema", "active",
                "entityCounts", Map.of(
                    "totalUsers", totalUsers,
                    "activeUsers", activeUsers,
                    "totalRoleAssignments", totalRoleAssignments,
                    "activeRoleAssignments", activeRoleAssignments
                ),
                "systemReady", totalUsers > 0 && activeUsers > 0,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Error getting system status", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}