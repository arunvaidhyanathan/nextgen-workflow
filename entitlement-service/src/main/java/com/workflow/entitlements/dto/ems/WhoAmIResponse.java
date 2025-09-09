package com.workflow.entitlements.dto.ems;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Complete user context response from whoami endpoint")
public class WhoAmIResponse {

    @JsonProperty("success")
    @Schema(description = "Whether the request was processed successfully", example = "true")
    private Boolean success;

    @JsonProperty("user")
    @Schema(description = "User information")
    private UserContext user;

    @JsonProperty("roles")
    @Schema(description = "User's assigned roles")
    private List<RoleContext> roles;

    @JsonProperty("departments")
    @Schema(description = "User's department assignments")
    private List<DepartmentContext> departments;

    @JsonProperty("permissions")
    @Schema(description = "User's permissions by resource type")
    private List<PermissionContext> permissions;

    @JsonProperty("context")
    @Schema(description = "Session and additional context information")
    private SessionContext context;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User information")
    public static class UserContext {
        
        @JsonProperty("id")
        @Schema(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440001")
        private String id;

        @JsonProperty("username")
        @Schema(description = "Username", example = "alice.intake")
        private String username;

        @JsonProperty("email")
        @Schema(description = "Email address", example = "alice.intake@company.com")
        private String email;

        @JsonProperty("firstName")
        @Schema(description = "First name", example = "Alice")
        private String firstName;

        @JsonProperty("lastName")
        @Schema(description = "Last name", example = "Johnson")
        private String lastName;

        @JsonProperty("displayName")
        @Schema(description = "Full display name", example = "Alice Johnson")
        private String displayName;

        @JsonProperty("isActive")
        @Schema(description = "Whether the user account is active", example = "true")
        private Boolean isActive;

        @JsonProperty("attributes")
        @Schema(description = "User's global attributes")
        private Map<String, Object> attributes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Role information")
    public static class RoleContext {
        
        @JsonProperty("id")
        @Schema(description = "Role ID", example = "1")
        private Long id;

        @JsonProperty("roleName")
        @Schema(description = "Role name", example = "INTAKE_ANALYST")
        private String roleName;

        @JsonProperty("displayName")
        @Schema(description = "Role display name", example = "Intake Analyst")
        private String displayName;

        @JsonProperty("businessApplication")
        @Schema(description = "Business application name", example = "onecms")
        private String businessApplication;

        @JsonProperty("isActive")
        @Schema(description = "Whether the role assignment is active", example = "true")
        private Boolean isActive;

        @JsonProperty("metadata")
        @Schema(description = "Role metadata including queue information")
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Department information")
    public static class DepartmentContext {
        
        @JsonProperty("id")
        @Schema(description = "Department ID", example = "1")
        private Long id;

        @JsonProperty("name")
        @Schema(description = "Department name", example = "Investigation Unit")
        private String name;

        @JsonProperty("code")
        @Schema(description = "Department code", example = "IU")
        private String code;

        @JsonProperty("isActive")
        @Schema(description = "Whether the department assignment is active", example = "true")
        private Boolean isActive;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Permission information by resource")
    public static class PermissionContext {
        
        @JsonProperty("resource")
        @Schema(description = "Resource type", example = "case")
        private String resource;

        @JsonProperty("actions")
        @Schema(description = "Available actions on the resource", example = "[\"create\", \"read\", \"update\"]")
        private List<String> actions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Session and context information")
    public static class SessionContext {
        
        @JsonProperty("sessionExpiration")
        @Schema(description = "Session expiration timestamp", example = "2025-01-09T12:00:00Z")
        private String sessionExpiration;

        @JsonProperty("lastAccessed")
        @Schema(description = "Last access timestamp", example = "2025-01-09T10:30:00Z")
        private String lastAccessed;

        @JsonProperty("queues")
        @Schema(description = "Available queues for the user", example = "[\"intake-analyst-queue\", \"eo-officer-queue\"]")
        private List<String> queues;
    }
}