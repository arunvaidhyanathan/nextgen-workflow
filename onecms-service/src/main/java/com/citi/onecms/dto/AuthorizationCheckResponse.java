package com.citi.onecms.dto;

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
    private String reason;
    
    public static AuthorizationCheckResponse allowed() {
        return AuthorizationCheckResponse.builder()
                .allowed(true)
                .reason("Access granted")
                .build();
    }
    
    public static AuthorizationCheckResponse denied(String reason) {
        return AuthorizationCheckResponse.builder()
                .allowed(false)
                .reason(reason)
                .build();
    }
    
    public static AuthorizationCheckResponse error(String reason) {
        return AuthorizationCheckResponse.builder()
                .allowed(false)
                .reason("Authorization error: " + reason)
                .build();
    }
}