package com.workflow.entitlements.controller;

import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.service.HybridAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for authorization checks
 */
@RestController
@RequestMapping("/api/entitlements")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationController {
    
    private final HybridAuthorizationService hybridAuthorizationService;
    
    /**
     * Primary authorization check endpoint
     */
    @PostMapping("/check")
    public ResponseEntity<AuthorizationCheckResponse> checkAuthorization(
            @RequestBody AuthorizationCheckRequest request) {
        
        try {
            log.debug("Authorization check request: userId={}, resourceKind={}, resourceId={}, action={}", 
                request.getPrincipal().getId(), 
                request.getResource().getKind(), 
                request.getResource().getId(), 
                request.getAction());
            
            AuthorizationCheckResponse response = hybridAuthorizationService.checkAuthorization(request);
            
            log.debug("Authorization check response: allowed={}, message={}", 
                response.isAllowed(), response.getMessage());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Authorization check failed", e);
            
            AuthorizationCheckResponse errorResponse = AuthorizationCheckResponse.builder()
                .allowed(false)
                .message("Authorization check failed: " + e.getMessage())
                .build();
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Simplified authorization check endpoint (for backward compatibility)
     */
    @PostMapping("/check-simple")
    public ResponseEntity<AuthorizationCheckResponse> checkSimpleAuthorization(
            @RequestBody SimpleAuthorizationRequest simpleRequest) {
        
        try {
            log.debug("Simple authorization check: userId={}, resourceType={}, resourceId={}, action={}", 
                simpleRequest.getUserId(), simpleRequest.getResourceType(), 
                simpleRequest.getResourceId(), simpleRequest.getAction());
            
            UUID userId = UUID.fromString(simpleRequest.getUserId());
            
            AuthorizationCheckResponse response = hybridAuthorizationService.checkUserPermission(
                userId, 
                simpleRequest.getResourceType(), 
                simpleRequest.getResourceId(), 
                simpleRequest.getAction()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Simple authorization check failed", e);
            
            AuthorizationCheckResponse errorResponse = AuthorizationCheckResponse.builder()
                .allowed(false)
                .message("Authorization check failed: " + e.getMessage())
                .build();
            
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * Health check for authorization service
     */
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        try {
            HybridAuthorizationService.AuthorizationInfo info = hybridAuthorizationService.getAuthorizationInfo();
            
            return ResponseEntity.ok(java.util.Map.of(
                "status", info.isHealthy() ? "UP" : "DOWN",
                "service", "authorization",
                "engine", info.getEngineType(),
                "useCerbos", info.isUseCerbos(),
                "description", info.getDescription()
            ));
            
        } catch (Exception e) {
            log.error("Authorization health check failed", e);
            
            return ResponseEntity.status(500).body(java.util.Map.of(
                "status", "DOWN",
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Simple request DTO for backward compatibility
     */
    @lombok.Data
    public static class SimpleAuthorizationRequest {
        private String userId;
        private String resourceType;
        private String resourceId;
        private String action;
        private java.util.Map<String, Object> resourceAttributes;
    }
}