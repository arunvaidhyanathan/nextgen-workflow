package com.flowable.wrapper.controller;

import com.flowable.wrapper.dto.request.DeployWorkflowRequest;
import com.flowable.wrapper.dto.request.RegisterWorkflowMetadataRequest;
import com.flowable.wrapper.dto.response.WorkflowMetadataResponse;
import com.flowable.wrapper.exception.WorkflowException;
import com.flowable.wrapper.service.WorkflowMetadataService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/{businessAppName}/workflow-metadata")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Workflow Metadata", description = "APIs for managing workflow metadata and deployments")
public class WorkflowMetadataController {
    
    private final WorkflowMetadataService workflowMetadataService;
    private final EntitlementServiceClient entitlementServiceClient;
    
    @PostMapping("/register")
    @Operation(summary = "Register workflow metadata", 
              description = "Register a new workflow with its task-to-queue mappings")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Workflow metadata registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or workflow already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WorkflowMetadataResponse> registerWorkflowMetadata(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Valid @RequestBody RegisterWorkflowMetadataRequest request,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Registering workflow metadata: {} in business app: {} by user: {}", 
                request.getProcessDefinitionKey(), businessAppName, userId);
        
        // Authorization check via Entitlement Service
        boolean isAuthorized = entitlementServiceClient.checkAuthorization(
                userId, null, "workflow", request.getProcessDefinitionKey(), 
                Map.of("businessAppName", businessAppName), "register").isAllowed();
        
        if (!isAuthorized) {
            log.warn("User {} not authorized to register workflow {} in business app {}", 
                    userId, request.getProcessDefinitionKey(), businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        WorkflowMetadataResponse response = workflowMetadataService.registerWorkflowMetadata(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/deploy")
    @Operation(summary = "Deploy BPMN workflow", 
              description = "Deploy a BPMN workflow to the Flowable engine")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflow deployed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or deployment failed"),
        @ApiResponse(responseCode = "404", description = "Workflow metadata not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WorkflowMetadataResponse> deployWorkflow(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Valid @RequestBody DeployWorkflowRequest request,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Deploying workflow: {} in business app: {} by user: {}", 
                request.getProcessDefinitionKey(), businessAppName, userId);
        
        // Authorization check via Entitlement Service
        boolean isAuthorized = entitlementServiceClient.checkAuthorization(
                userId, null, "workflow", request.getProcessDefinitionKey(), 
                Map.of("businessAppName", businessAppName), "deploy").isAllowed();
        
        if (!isAuthorized) {
            log.warn("User {} not authorized to deploy workflow {} in business app {}", 
                    userId, request.getProcessDefinitionKey(), businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        WorkflowMetadataResponse response = workflowMetadataService.deployWorkflow(request);
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{processDefinitionKey}")
    @Operation(summary = "Get workflow metadata", 
              description = "Retrieve workflow metadata by process definition key")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflow metadata found"),
        @ApiResponse(responseCode = "404", description = "Workflow metadata not found")
    })
    public ResponseEntity<WorkflowMetadataResponse> getWorkflowMetadata(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @Parameter(description = "Process definition key", required = true)
            @PathVariable String processDefinitionKey,
            HttpServletRequest httpRequest) {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Getting workflow metadata for process: {} in business app: {} by user: {}", 
                processDefinitionKey, businessAppName, userId);
        
        // Authorization check via Entitlement Service
        boolean isAuthorized = entitlementServiceClient.checkAuthorization(
                userId, null, "workflow", processDefinitionKey, 
                Map.of("businessAppName", businessAppName), "view").isAllowed();
        
        if (!isAuthorized) {
            log.warn("User {} not authorized to view workflow {} in business app {}", 
                    userId, processDefinitionKey, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        WorkflowMetadataResponse response = workflowMetadataService.getWorkflowMetadata(processDefinitionKey);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/deploy-from-file")
    @Operation(summary = "Deploy BPMN workflow from file", 
              description = "Deploy a BPMN workflow from a file in the mounted definitions directory")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflow deployed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or deployment failed"),
        @ApiResponse(responseCode = "404", description = "Workflow metadata or file not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<WorkflowMetadataResponse> deployWorkflowFromFile(
            @Parameter(description = "Business application name", required = true)
            @PathVariable String businessAppName,
            @RequestParam String processDefinitionKey,
            @RequestParam String filename,
            HttpServletRequest httpRequest) throws WorkflowException {
        
        String userId = httpRequest.getHeader("X-User-Id");
        log.info("Deploying workflow from file: {} for process: {} in business app: {} by user: {}", 
                filename, processDefinitionKey, businessAppName, userId);
        
        // Authorization check via Entitlement Service
        boolean isAuthorized = entitlementServiceClient.checkAuthorization(
                userId, null, "workflow", processDefinitionKey, 
                Map.of("businessAppName", businessAppName), "deploy").isAllowed();
        
        if (!isAuthorized) {
            log.warn("User {} not authorized to deploy workflow {} in business app {}", 
                    userId, processDefinitionKey, businessAppName);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        WorkflowMetadataResponse response = workflowMetadataService.deployWorkflowFromFile(processDefinitionKey, filename);
        
        return ResponseEntity.ok(response);
    }
}