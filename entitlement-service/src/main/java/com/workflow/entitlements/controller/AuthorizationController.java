package com.workflow.entitlements.controller;

import com.workflow.entitlements.cerbos.CerbosAuthorizationService;
import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entitlements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authorization", description = "Authorization and entitlement checking APIs")
public class AuthorizationController {
    
    private final CerbosAuthorizationService cerbosAuthorizationService;
    
    @PostMapping("/check")
    @Operation(summary = "Check authorization", description = "Perform authorization check using Cerbos policies")
    public ResponseEntity<AuthorizationCheckResponse> checkAuthorization(
            @RequestBody @Validated AuthorizationCheckRequest request) {
        
        log.info("Authorization check request: principal={}, resource={}, action={}", 
                request.getPrincipal().getId(), request.getResource().getKind(), request.getAction());
        
        try {
            AuthorizationCheckResponse response = cerbosAuthorizationService.check(request);
            
            // Return 200 OK for both allowed and denied to maintain consistent API behavior
            // The client should check the 'allowed' field in the response
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Authorization check failed: principal={}, resource={}, action={}, error={}", 
                    request.getPrincipal().getId(), request.getResource().getKind(), request.getAction(), e.getMessage(), e);
            
            return ResponseEntity.ok(AuthorizationCheckResponse.error("Authorization check failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Entitlement Service is running");
    }
}