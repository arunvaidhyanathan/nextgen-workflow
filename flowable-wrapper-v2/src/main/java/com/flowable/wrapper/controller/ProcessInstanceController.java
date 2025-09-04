package com.flowable.wrapper.controller;

import com.flowable.wrapper.dto.request.StartProcessRequest;
import com.flowable.wrapper.dto.response.ProcessInstanceResponse;
import com.flowable.wrapper.exception.WorkflowException;
import com.flowable.wrapper.service.ProcessInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import com.flowable.wrapper.client.EntitlementServiceClient;
import com.flowable.wrapper.config.AuthorizationProperties;
import java.util.Map;

@RestController
@RequestMapping("/api/{businessAppName}/process-instances")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Process Instances", description = "APIs for managing process instances")
public class ProcessInstanceController {
    
    private final ProcessInstanceService processInstanceService;
    private final EntitlementServiceClient entitlementServiceClient;
    private final AuthorizationProperties authorizationProperties;
    
    @PostMapping("/start")
    @Operation(summary = "Start a new process instance", 
              description = "Start a new instance of a deployed workflow")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Process instance started successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Workflow not found or not deployed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProcessInstanceResponse> startProcess(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Valid @RequestBody StartProcessRequest request,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Starting process instance for workflow: {} in business app: {} by user: {}", 
                request.getProcessDefinitionKey(), businessAppName, userId);
        
        // Authorization check for starting workflow instance (if enabled)
        if (authorizationProperties.isEnabled()) {
            boolean isAuthorized = entitlementServiceClient.checkAuthorization(
                    userId, null, "process", request.getProcessDefinitionKey(), 
                    Map.of("businessAppName", businessAppName), "start").isAllowed();
            
            if (!isAuthorized) {
                log.warn("User {} not authorized to create process {} in business app {}", 
                        userId, request.getProcessDefinitionKey(), businessAppName);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            log.info("Authorization disabled - bypassing auth check for process start");
        }
        
        ProcessInstanceResponse response = processInstanceService.startProcess(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{processInstanceId}")
    @Operation(summary = "Get process instance details", 
              description = "Retrieve details of a specific process instance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Process instance found"),
        @ApiResponse(responseCode = "404", description = "Process instance not found")
    })
    public ResponseEntity<ProcessInstanceResponse> getProcessInstance(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Process instance ID", required = true)
            @PathVariable String processInstanceId,
            HttpServletRequest httpRequest) {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Getting process instance: {} in business app: {} by user: {}", 
                processInstanceId, businessAppName, userId);
        
        // Authorization check for reading workflow instance (if enabled)
        if (authorizationProperties.isEnabled()) {
            boolean isAuthorized = entitlementServiceClient.checkAuthorization(
                    userId, null, "process", processInstanceId, 
                    Map.of("businessAppName", businessAppName), "view").isAllowed();
            
            if (!isAuthorized) {
                log.warn("User {} not authorized to view process instance {} in business app {}", 
                        userId, processInstanceId, businessAppName);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            log.info("Authorization disabled - bypassing auth check for process instance view");
        }
        
        ProcessInstanceResponse response = processInstanceService.getProcessInstance(processInstanceId);
        
        return ResponseEntity.ok(response);
    }
}