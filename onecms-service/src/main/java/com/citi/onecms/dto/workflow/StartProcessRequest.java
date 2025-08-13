package com.citi.onecms.dto.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartProcessRequest {
    private String processDefinitionKey;
    private String businessKey;
    private Map<String, Object> variables;
    private String initiatorId;
}