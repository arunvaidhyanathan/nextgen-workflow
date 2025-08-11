package com.citi.onecms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorizationCheckRequest {
    
    private Principal principal;
    private Resource resource;
    private String action;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Principal {
        private String id;
        private List<String> roles;
        private Map<String, Object> attributes;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resource {
        private String kind;
        private String id;
        private Map<String, Object> attributes;
    }
}