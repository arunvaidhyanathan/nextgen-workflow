package com.workflow.entitlements.controller;

import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.service.HybridAuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for authorization checks and policy evaluation
 */
@RestController
@RequestMapping("/api/entitlements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "authorization-controller", description = "Core Authorization Endpoints")
public class AuthorizationController {
    
    private final HybridAuthorizationService hybridAuthorizationService;
    
    /**
     * Primary authorization check endpoint - Advanced Cerbos-based authorization
     */
    @Operation(
            summary = "Check Authorization (Advanced)",
            description = """
                    **Primary authorization endpoint using Cerbos ABAC engine**
                    
                    This endpoint evaluates complex authorization policies using the Cerbos policy engine.
                    It supports attribute-based access control (ABAC) with rich principal and resource contexts.
                    
                    **Use Cases:**
                    - Complex multi-attribute authorization decisions
                    - Policy-driven access control
                    - Resource-specific attribute evaluation
                    - Derived role calculations
                    
                    **Authorization Flow:**
                    1. Validates request structure and required fields
                    2. Builds principal context from user attributes
                    3. Evaluates Cerbos policies against resource and action
                    4. Returns detailed authorization decision
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authorization decision completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthorizationCheckResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Access Granted",
                                            summary = "User has permission to perform action",
                                            value = """
                                                    {
                                                      "allowed": true,
                                                      "message": "Access granted: User has CREATE permission for case resources",
                                                      "validationResult": "POLICY_MATCHED"
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "Access Denied",
                                            summary = "User lacks permission to perform action",
                                            value = """
                                                    {
                                                      "allowed": false,
                                                      "message": "Access denied: User lacks UPDATE permission for case CMS-2025-000123",
                                                      "validationResult": "POLICY_DENIED"
                                                    }
                                                    """)
                            }))
    })
    @PostMapping("/check")
    public ResponseEntity<AuthorizationCheckResponse> checkAuthorization(
            @RequestBody(
                    description = "Authorization check request with principal, resource, and action",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthorizationCheckRequest.class),
                            examples = @ExampleObject(
                                    name = "Case Management Authorization",
                                    summary = "Check if user can update a specific case",
                                    value = """
                                            {
                                              "principal": {
                                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                                "attributes": {
                                                  "department": "INVESTIGATION",
                                                  "clearance": "confidential",
                                                  "roles": ["INVESTIGATOR", "CASE_ASSIGNEE"],
                                                  "queues": ["investigator-queue"]
                                                }
                                              },
                                              "resource": {
                                                "kind": "case",
                                                "id": "CMS-2025-000123",
                                                "attributes": {
                                                  "status": "ACTIVE",
                                                  "department": "INVESTIGATION",
                                                  "priority": "HIGH",
                                                  "assignedTo": "550e8400-e29b-41d4-a716-446655440000"
                                                }
                                              },
                                              "action": "update"
                                            }
                                            """)))
            AuthorizationCheckRequest request) {
        
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
    @Operation(
            summary = "Check Authorization (Simple)",
            description = """
                    **Simplified authorization endpoint for basic permission checks**
                    
                    This endpoint provides a simplified interface for common authorization scenarios.
                    It uses UUID-based user identification and supports basic resource permission checks.
                    
                    **Use Cases:**
                    - Quick permission validation
                    - Legacy system integration
                    - Simple resource access checks
                    - Basic CRUD operation authorization
                    
                    **Authorization Flow:**
                    1. Validates UUID format for userId
                    2. Looks up user context from database
                    3. Evaluates permissions using hybrid authorization engine
                    4. Returns simple allow/deny decision
                    
                    **Note:** This endpoint is maintained for backward compatibility. 
                    Use `/check` for advanced policy-based authorization.
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Authorization decision completed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthorizationCheckResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "Simple Access Granted",
                                            summary = "User can access resource",
                                            value = """
                                                    {
                                                      "allowed": true,
                                                      "message": "User has READ permission for resource",
                                                      "validationResult": null
                                                    }
                                                    """),
                                    @ExampleObject(
                                            name = "Invalid User ID",
                                            summary = "Invalid UUID format provided",
                                            value = """
                                                    {
                                                      "allowed": false,
                                                      "message": "Authorization check failed: Invalid UUID string: invalid-uuid",
                                                      "validationResult": null
                                                    }
                                                    """)
                            }))
    })
    @PostMapping("/check-simple")
    public ResponseEntity<AuthorizationCheckResponse> checkSimpleAuthorization(
            @RequestBody(
                    description = "Simple authorization request with basic parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SimpleAuthorizationRequest.class),
                            examples = @ExampleObject(
                                    name = "Simple Case Access Check",
                                    summary = "Check if user can read a case",
                                    value = """
                                            {
                                              "userId": "550e8400-e29b-41d4-a716-446655440000",
                                              "resourceType": "case",
                                              "resourceId": "CMS-2025-000123",
                                              "action": "read",
                                              "resourceAttributes": {
                                                "department": "INVESTIGATION",
                                                "status": "ACTIVE"
                                              }
                                            }
                                            """)))
            SimpleAuthorizationRequest simpleRequest) {
        
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
    @Operation(
            summary = "Authorization Service Health Check",
            description = """
                    **Comprehensive health check for the authorization service**
                    
                    This endpoint provides detailed information about the authorization service status,
                    including the active authorization engine, connectivity status, and service configuration.
                    
                    **Health Information Includes:**
                    - Service overall status (UP/DOWN)
                    - Active authorization engine type (CERBOS, DATABASE, HYBRID)
                    - Engine-specific connectivity status
                    - Configuration details
                    - Performance indicators
                    
                    **Use Cases:**
                    - Service monitoring and health checks
                    - Troubleshooting authorization issues
                    - Configuration validation
                    - Performance monitoring
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Health check completed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Healthy Service",
                                            summary = "Authorization service is healthy",
                                            value = """
                                                    {
                                                      "status": "UP",
                                                      "service": "authorization",
                                                      "engine": "HYBRID",
                                                      "useCerbos": true,
                                                      "description": "Hybrid authorization engine with Cerbos ABAC + Database RBAC"
                                                    }
                                                    """)
                            })),
            @ApiResponse(
                    responseCode = "500",
                    description = "Health check failed - service is unhealthy",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Unhealthy Service",
                                    summary = "Authorization service has issues",
                                    value = """
                                            {
                                              "status": "DOWN",
                                              "error": "Cerbos connection failed: Connection refused"
                                            }
                                            """)))
    })
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
    @Schema(
            description = "Simple authorization request for basic permission checks",
            example = """
                    {
                      "userId": "550e8400-e29b-41d4-a716-446655440000",
                      "resourceType": "case",
                      "resourceId": "CMS-2025-000123",
                      "action": "read",
                      "resourceAttributes": {
                        "department": "INVESTIGATION",
                        "status": "ACTIVE"
                      }
                    }
                    """)
    public static class SimpleAuthorizationRequest {
        
        @Schema(
                description = "User UUID for authorization check",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid",
                required = true)
        private String userId;
        
        @Schema(
                description = "Type of resource being accessed (e.g., case, workflow, task)",
                example = "case",
                required = true)
        private String resourceType;
        
        @Schema(
                description = "Unique identifier of the specific resource instance",
                example = "CMS-2025-000123",
                required = true)
        private String resourceId;
        
        @Schema(
                description = "Action being performed on the resource",
                example = "read",
                allowableValues = {"create", "read", "update", "delete", "claim", "complete", "delegate"},
                required = true)
        private String action;
        
        @Schema(
                description = "Optional resource attributes for context-aware authorization",
                example = """
                        {
                          "department": "INVESTIGATION",
                          "status": "ACTIVE",
                          "priority": "HIGH"
                        }
                        """)
        private java.util.Map<String, Object> resourceAttributes;
    }
}