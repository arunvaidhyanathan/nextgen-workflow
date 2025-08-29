package com.workflow.entitlements.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCheckRequest {
    
    @NotNull
    private Principal principal;
    
    @NotNull
    private Resource resource;
    
    @NotBlank
    private String action;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Principal {
        @NotNull
        private UUID id;
        
        private Map<String, Object> attributes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        @NotBlank
        private String kind;
        
        @NotBlank
        private String id;
        
        private Map<String, Object> attributes;
    }
}