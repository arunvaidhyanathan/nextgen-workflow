package com.workflow.entitlements.dto.ems;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authorization request for EMS caniuse endpoint")
public class EMSAuthRequest {

    @JsonProperty("resourceId")
    @Schema(description = "Resource identifier", example = "CMS-10-20045")
    private String resourceId;

    @JsonProperty("actionId")
    @Schema(description = "Action identifier", example = "CREATE_CASE")
    private String actionId;

    @JsonProperty("resourceType")
    @Schema(description = "Resource type", example = "case")
    private String resourceType;

    @JsonProperty("context")
    @Schema(description = "Additional context information")
    private Map<String, Object> context;
}