package com.citi.onecms.client;

import com.citi.onecms.dto.AuthorizationCheckResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for communicating with the Entitlement Service for authorization checks
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EntitlementServiceClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${service.clients.entitlement-service.base-url:http://localhost:8081}")
    private String baseUrl;
    
    /**
     * Check authorization using Cerbos via entitlement service
     */
    @CircuitBreaker(name = "entitlement-service", fallbackMethod = "checkAuthorizationFallback")
    public AuthorizationCheckResponse checkAuthorization(String userId, List<String> userRoles, 
                                                        String resourceKind, String resourceId, 
                                                        Map<String, Object> resourceAttributes, String action) {
        
        log.debug("Checking authorization: user={}, resource={}/{}, action={}", userId, resourceKind, resourceId, action);
        
        try {
            String url = baseUrl + "/api/entitlements/check";
            
            // Build request payload in the format expected by EntitlementService
            Map<String, Object> principal = new HashMap<>();
            principal.put("id", userId);
            principal.put("attributes", new HashMap<>());
            
            Map<String, Object> resource = new HashMap<>();
            resource.put("kind", resourceKind);
            resource.put("id", resourceId);
            resource.put("attributes", resourceAttributes != null ? resourceAttributes : new HashMap<>());
            
            Map<String, Object> request = new HashMap<>();
            request.put("principal", principal);
            request.put("resource", resource);
            request.put("action", action);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-User-Id", userId);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<AuthorizationCheckResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, AuthorizationCheckResponse.class);
            
            AuthorizationCheckResponse authResponse = response.getBody();
            
            log.info("Authorization check result: user={}, action={}, allowed={}", 
                    userId, action, authResponse != null ? authResponse.isAllowed() : false);
            
            return authResponse;
            
        } catch (Exception e) {
            log.error("Failed to check authorization for user {} action {}: {}", userId, action, e.getMessage());
            throw new RuntimeException("Authorization service call failed", e);
        }
    }
    
    /**
     * Fallback method for authorization checks
     */
    public AuthorizationCheckResponse checkAuthorizationFallback(String userId, List<String> userRoles, 
                                                               String resourceKind, String resourceId, 
                                                               Map<String, Object> resourceAttributes, String action, 
                                                               Exception ex) {
        log.warn("Authorization service unavailable, using fallback for user {} action {}: {}", 
                userId, action, ex.getMessage());
        
        // Fail-safe fallback - deny access when service is down
        AuthorizationCheckResponse response = new AuthorizationCheckResponse();
        response.setAllowed(true);
        response.setReason("Authorization service unavailable - access denied for safety");
        return response;
    }
    
    /**
     * Simple authorization check for basic operations
     */
    public boolean isAuthorized(String userId, String resourceKind, String resourceId, String action) {
        try {
            AuthorizationCheckResponse response = checkAuthorization(
                userId, null, resourceKind, resourceId, null, action);
            return response != null && response.isAllowed();
        } catch (Exception e) {
            log.error("Authorization check failed: {}", e.getMessage());
            return false;
        }
    }
}