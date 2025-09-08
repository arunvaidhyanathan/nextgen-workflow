package com.workflow.entitlements.controller;

import com.workflow.entitlements.cerbos.CerbosPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for policy management and validation endpoints
 */
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
@Slf4j
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    value = "authorization.engine.use-cerbos", 
    havingValue = "true", 
    matchIfMissing = false)
public class PolicyController {
    
    private final CerbosPolicyService cerbosPolicyService;
    
    /**
     * Get policy status and inventory
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPolicyStatus() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Test Cerbos connection
            boolean connected = cerbosPolicyService.testCerbosConnection();
            response.put("cerbosConnected", connected);
            
            // Log policy inventory (check logs for details)
            cerbosPolicyService.logPolicyInventory();
            response.put("inventoryLogged", true);
            
            response.put("status", "OK");
            response.put("message", "Policy service is operational");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get policy status", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Policy service check failed: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Validate policy files
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validatePolicies() {
        try {
            cerbosPolicyService.validatePolicyFiles();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "VALID");
            response.put("message", "All policy files validated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Policy validation failed", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "INVALID");
            errorResponse.put("message", "Policy validation failed: " + e.getMessage());
            
            return ResponseEntity.status(400).body(errorResponse);
        }
    }
    
    /**
     * Get policy file content (for debugging)
     */
    @GetMapping("/content/{policyType}/{fileName}")
    public ResponseEntity<Map<String, Object>> getPolicyContent(
            @PathVariable String policyType,
            @PathVariable String fileName) {
        
        try {
            String relativePath = policyType + "/" + fileName;
            String content = cerbosPolicyService.getPolicyFileContent(relativePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("policyType", policyType);
            response.put("fileName", fileName);
            response.put("content", content);
            response.put("contentLength", content.length());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get policy content: {}/{}", policyType, fileName, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to retrieve policy content: " + e.getMessage());
            
            return ResponseEntity.status(404).body(errorResponse);
        }
    }
}