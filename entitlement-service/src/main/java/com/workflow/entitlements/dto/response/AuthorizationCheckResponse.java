package com.workflow.entitlements.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCheckResponse {
    
    private boolean allowed;
    
    private String message;
    
    private String validationResult;
    
    public static AuthorizationCheckResponse allowed() {
        return AuthorizationCheckResponse.builder()
                .allowed(true)
                .message("Access granted")
                .build();
    }
    
    public static AuthorizationCheckResponse denied(String reason) {
        return AuthorizationCheckResponse.builder()
                .allowed(false)
                .message(reason != null ? reason : "Access denied")
                .build();
    }
    
    public static AuthorizationCheckResponse error(String error) {
        return AuthorizationCheckResponse.builder()
                .allowed(false)
                .message("Authorization check failed")
                .validationResult(error)
                .build();
    }
}