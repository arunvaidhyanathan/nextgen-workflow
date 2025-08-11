package com.citi.onecms.service;

import com.citi.onecms.dto.RegisterWorkflowMetadataRequest;
import com.citi.onecms.dto.WorkflowMetadataResponse;
import com.citi.onecms.dto.DeployWorkflowRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Stub implementation of WorkflowMetadataService for microservice architecture.
 * This service will communicate with the workflow-service via REST APIs.
 * TODO: Replace with proper REST client implementation.
 */
@Service
public class WorkflowMetadataService {
    
    public WorkflowMetadataResponse registerWorkflowMetadata(RegisterWorkflowMetadataRequest request) {
        // TODO: Implement REST call to workflow-service
        throw new UnsupportedOperationException("This method should make a REST call to workflow-service");
    }
    
    public List<WorkflowMetadataResponse> getAllWorkflowMetadata() {
        // TODO: Implement REST call to workflow-service
        throw new UnsupportedOperationException("This method should make a REST call to workflow-service");
    }
    
    public WorkflowMetadataResponse getWorkflowMetadata(String workflowId) {
        // TODO: Implement REST call to workflow-service
        throw new UnsupportedOperationException("This method should make a REST call to workflow-service");
    }
    
    public void deleteWorkflowMetadata(String workflowId) {
        // TODO: Implement REST call to workflow-service
        throw new UnsupportedOperationException("This method should make a REST call to workflow-service");
    }
    
    public WorkflowMetadataResponse updateWorkflowMetadata(String workflowId, RegisterWorkflowMetadataRequest request) {
        // TODO: Implement REST call to workflow-service
        throw new UnsupportedOperationException("This method should make a REST call to workflow-service");
    }
    
    public WorkflowMetadataResponse deployWorkflow(DeployWorkflowRequest request) {
        // TODO: Implement REST call to workflow-service
        throw new UnsupportedOperationException("This method should make a REST call to workflow-service");
    }
    
    public List<WorkflowMetadataResponse> getAllActiveWorkflows() {
        // TODO: Implement REST call to workflow-service
        throw new UnsupportedOperationException("This method should make a REST call to workflow-service");
    }
    
    public List<WorkflowMetadataResponse> getAllDeployedWorkflows() {
        // TODO: Implement REST call to workflow-service
        throw new UnsupportedOperationException("This method should make a REST call to workflow-service");
    }
}