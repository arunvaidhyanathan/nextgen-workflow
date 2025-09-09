package com.workflow.entitlements.controller;

import com.workflow.entitlements.dto.ems.EMSAuthRequest;
import com.workflow.entitlements.dto.ems.EMSAuthResponse;
import com.workflow.entitlements.dto.ems.WhoAmIResponse;
import com.workflow.entitlements.service.EMSService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * EMS Controller - Enterprise Management System API endpoints
 * Provides user context and authorization information for frontend applications
 */
@RestController
@RequestMapping("/api/ems/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EMS", description = "Enterprise Management System API for frontend integration")
public class EMSController {

    private final EMSService emsService;

    /**
     * Get comprehensive user context - WHO AM I endpoint
     */
    @GetMapping("/whoami")
    @Operation(
        summary = "Get current user context",
        description = """
            Retrieve comprehensive user information including roles, departments, permissions,
            and session context for frontend application use.
            
            **Authentication:** Requires either X-Session-Id or X-User-Id header
            
            **Use Cases:**
            - Initialize user interface with current user context
            - Display user information in navigation/profile
            - Determine available features based on roles
            - Show assigned departments and queues
            """)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User context retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WhoAmIResponse.class),
                examples = @ExampleObject(
                    name = "successful_response",
                    value = """
                        {
                          "success": true,
                          "user": {
                            "id": "550e8400-e29b-41d4-a716-446655440001",
                            "username": "alice.intake",
                            "email": "alice.intake@company.com",
                            "firstName": "Alice",
                            "lastName": "Johnson",
                            "displayName": "Alice Johnson",
                            "isActive": true,
                            "attributes": {
                              "department": "IU",
                              "clearance": "standard"
                            }
                          },
                          "roles": [
                            {
                              "id": 1,
                              "roleName": "INTAKE_ANALYST",
                              "displayName": "Intake Analyst",
                              "businessApplication": "onecms",
                              "isActive": true,
                              "metadata": {
                                "queue": "intake-analyst-queue"
                              }
                            }
                          ],
                          "departments": [
                            {
                              "id": 4,
                              "name": "Investigation Unit",
                              "code": "IU",
                              "isActive": true
                            }
                          ],
                          "permissions": [
                            {
                              "resource": "case",
                              "actions": ["create", "read", "update", "route"]
                            }
                          ],
                          "context": {
                            "sessionExpiration": "2025-01-09T12:00:00Z",
                            "lastAccessed": "2025-01-09T10:30:00Z",
                            "queues": ["intake-analyst-queue"]
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required - invalid or missing session/user ID",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "unauthorized_response",
                    value = """
                        {
                          "success": false,
                          "error": {
                            "code": "UNAUTHORIZED",
                            "message": "No valid authentication provided",
                            "timestamp": "2025-01-09T10:30:00Z",
                            "path": "/api/ems/v1/whoami"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "error_response",
                    value = """
                        {
                          "success": false,
                          "error": {
                            "code": "INTERNAL_ERROR",
                            "message": "An unexpected error occurred",
                            "timestamp": "2025-01-09T10:30:00Z",
                            "path": "/api/ems/v1/whoami"
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Object> whoAmI(
            @Parameter(description = "Session ID for authentication", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            
            @Parameter(description = "Direct user ID for API Gateway integration", example = "550e8400-e29b-41d4-a716-446655440001")
            @RequestHeader(value = "X-User-Id", required = false) String directUserId) {

        try {
            // Resolve user ID from session or direct header
            Optional<String> resolvedUserId = resolveUserId(sessionId, directUserId);
            
            if (resolvedUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("UNAUTHORIZED", "No valid authentication provided", "/api/ems/v1/whoami"));
            }

            // Build user context
            WhoAmIResponse response = emsService.buildUserContext(resolvedUserId.get());
            
            if (!response.getSuccess()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("UNAUTHORIZED", "User not found or inactive", "/api/ems/v1/whoami"));
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format in whoami request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("BAD_REQUEST", "Invalid user ID format", "/api/ems/v1/whoami"));
                
        } catch (Exception e) {
            log.error("Error processing whoami request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", "/api/ems/v1/whoami"));
        }
    }

    /**
     * Check user authorization for resources and actions - CAN I USE endpoint
     */
    @PostMapping("/caniuse")
    @Operation(
        summary = "Check user authorization",
        description = """
            Check if the current user can perform specific actions on resources.
            Returns detailed authorization decisions for frontend use.
            
            **Authentication:** Requires either X-Session-Id or X-User-Id header
            
            **Use Cases:**
            - Enable/disable UI buttons based on permissions
            - Show/hide features based on authorization
            - Validate actions before API calls
            - Display permission-based content
            """)
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authorization check completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EMSAuthResponse.class),
                examples = @ExampleObject(
                    name = "successful_auth_response",
                    value = """
                        {
                          "success": true,
                          "actions": [
                            {
                              "actionId": "CREATE_CASE",
                              "displayName": "Create Case",
                              "allowed": true,
                              "reason": "User has INTAKE_ANALYST role"
                            },
                            {
                              "actionId": "APPROVE_CASE", 
                              "displayName": "Approve Case",
                              "allowed": false,
                              "reason": "Insufficient privileges"
                            }
                          ],
                          "resourceAccess": {
                            "canRead": true,
                            "canWrite": true,
                            "canDelete": false,
                            "canApprove": false
                          },
                          "derivedRoles": ["user", "case_creator"],
                          "evaluationTime": 45
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - invalid request format",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "bad_request_response",
                    value = """
                        {
                          "success": false,
                          "error": {
                            "code": "BAD_REQUEST",
                            "message": "Invalid request format",
                            "timestamp": "2025-01-09T10:30:00Z",
                            "path": "/api/ems/v1/caniuse"
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Authentication required",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "unauthorized_response",
                    value = """
                        {
                          "success": false,
                          "error": {
                            "code": "UNAUTHORIZED",
                            "message": "No valid authentication provided",
                            "timestamp": "2025-01-09T10:30:00Z",
                            "path": "/api/ems/v1/caniuse"
                          }
                        }
                        """
                )
            )
        )
    })
    public ResponseEntity<Object> canIUse(
            @Parameter(description = "Session ID for authentication")
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            
            @Parameter(description = "Direct user ID for API Gateway integration")
            @RequestHeader(value = "X-User-Id", required = false) String directUserId,
            
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Authorization check request",
                required = true,
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = EMSAuthRequest.class),
                    examples = {
                        @ExampleObject(
                            name = "specific_action_check",
                            description = "Check specific action on a resource",
                            value = """
                                {
                                  "resourceId": "CMS-10-20045",
                                  "actionId": "CREATE_CASE",
                                  "resourceType": "case",
                                  "context": {
                                    "department": "HR",
                                    "priority": "HIGH"
                                  }
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "resource_check",
                            description = "Check all common actions for a resource type",
                            value = """
                                {
                                  "resourceType": "case",
                                  "context": {
                                    "department": "IU"
                                  }
                                }
                                """
                        ),
                        @ExampleObject(
                            name = "minimal_check",
                            description = "Minimal authorization check",
                            value = """
                                {
                                  "actionId": "READ_CASE"
                                }
                                """
                        )
                    }
                )
            )
            @RequestBody EMSAuthRequest request) {

        try {
            // Validate request
            if (request == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(buildErrorResponse("BAD_REQUEST", "Request body is required", "/api/ems/v1/caniuse"));
            }

            // Resolve user ID
            Optional<String> resolvedUserId = resolveUserId(sessionId, directUserId);
            
            if (resolvedUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(buildErrorResponse("UNAUTHORIZED", "No valid authentication provided", "/api/ems/v1/caniuse"));
            }

            // Perform authorization check
            EMSAuthResponse response = emsService.checkUserAuthorization(resolvedUserId.get(), request);
            
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request in caniuse: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildErrorResponse("BAD_REQUEST", "Invalid request format: " + e.getMessage(), "/api/ems/v1/caniuse"));
                
        } catch (Exception e) {
            log.error("Error processing caniuse request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", "/api/ems/v1/caniuse"));
        }
    }

    // Private helper methods

    /**
     * Resolve user ID from session or direct header
     */
    private Optional<String> resolveUserId(String sessionId, String directUserId) {
        // Try session-based authentication first
        if (sessionId != null && !sessionId.isEmpty()) {
            if (emsService.validateUserSession(sessionId)) {
                Optional<String> userIdFromSession = emsService.getUserIdFromSession(sessionId);
                if (userIdFromSession.isPresent()) {
                    return userIdFromSession;
                }
            }
        }
        
        // Fallback to direct user ID (API Gateway style)
        if (directUserId != null && !directUserId.isEmpty()) {
            try {
                // Validate UUID format
                UUID.fromString(directUserId);
                return Optional.of(directUserId);
            } catch (IllegalArgumentException e) {
                log.debug("Invalid UUID format for direct user ID: {}", directUserId);
            }
        }
        
        return Optional.empty();
    }

    /**
     * Build consistent error response format
     */
    private Map<String, Object> buildErrorResponse(String code, String message, String path) {
        return Map.of(
            "success", false,
            "error", Map.of(
                "code", code,
                "message", message,
                "timestamp", java.time.Instant.now().toString(),
                "path", path
            )
        );
    }
}