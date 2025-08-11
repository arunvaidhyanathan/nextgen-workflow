// src/main/java/com/workflow/cmsflowable/model/DeploymentResult.java
package com.citi.onecms.util;

public class DeploymentResult {
    private String resourceName;
    private String resourceType;
    private boolean success;
    private String deploymentId;
    private String definitionKey; // e.g., process definition key, decision definition key
    private String message;
    private String error; // This will hold the simple class name of the exception

    // 1. Full Constructor
    public DeploymentResult(String resourceName, String resourceType, boolean success, String deploymentId, String definitionKey, String message, String error) {
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.success = success;
        this.deploymentId = deploymentId;
        this.definitionKey = definitionKey;
        this.message = message;
        this.error = error;
    }

    // 2. Convenience Constructor for Failure (takes an Exception object)
    // This signature (String, String, Exception) is now distinct from (String, String, String, String)
    public DeploymentResult(String resourceName, String resourceType, Exception e) {
        this(resourceName, resourceType, false, null, null,
             "Failed to deploy: " + (e != null ? e.getMessage() : "Unknown error"), // Provide a meaningful message
             e != null ? e.getClass().getSimpleName() : "UnknownErrorType"); // Get the simple class name of the error
    }

    // 3. Convenience Constructor for Success (stays the same, its signature is now unique)
    public DeploymentResult(String resourceName, String resourceType, String deploymentId, String definitionKey) {
        this(resourceName, resourceType, true, deploymentId, definitionKey, "Deployed successfully", null);
    }

    // Getters
    public String getResourceName() {
        return resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getDefinitionKey() {
        return definitionKey;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    // Setters (if needed for deserialization, though typically not for results)
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public void setDefinitionKey(String definitionKey) {
        this.definitionKey = definitionKey;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setError(String error) {
        this.error = error;
    }
}