package com.workflow.entitlements.dto.ems;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authorization response from EMS caniuse endpoint")
public class EMSAuthResponse {

    @JsonProperty("success")
    @Schema(description = "Whether the request was processed successfully", example = "true")
    private Boolean success;

    @JsonProperty("actions")
    @Schema(description = "List of actions with their authorization status")
    private List<ActionAuthResult> actions;

    @JsonProperty("resourceAccess")
    @Schema(description = "Summary of resource access permissions")
    private ResourceAccess resourceAccess;

    @JsonProperty("derivedRoles")
    @Schema(description = "List of derived roles applied during evaluation")
    private List<String> derivedRoles;

    @JsonProperty("evaluationTime")
    @Schema(description = "Policy evaluation time in milliseconds", example = "25")
    private Long evaluationTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Action authorization result")
    public static class ActionAuthResult {
        
        @JsonProperty("actionId")
        @Schema(description = "Action identifier", example = "CREATE_CASE")
        private String actionId;

        @JsonProperty("displayName")
        @Schema(description = "Human-readable action name", example = "Create Case")
        private String displayName;

        @JsonProperty("allowed")
        @Schema(description = "Whether the action is allowed", example = "true")
        private Boolean allowed;

        @JsonProperty("reason")
        @Schema(description = "Reason for the authorization decision", example = "User has INTAKE_ANALYST role")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Resource access summary")
    public static class ResourceAccess {
        
        @JsonProperty("canRead")
        @Schema(description = "Can read the resource", example = "true")
        private Boolean canRead;

        @JsonProperty("canWrite")
        @Schema(description = "Can write/update the resource", example = "true")
        private Boolean canWrite;

        @JsonProperty("canDelete")
        @Schema(description = "Can delete the resource", example = "false")
        private Boolean canDelete;

        @JsonProperty("canApprove")
        @Schema(description = "Can approve actions on the resource", example = "false")
        private Boolean canApprove;
    }
}