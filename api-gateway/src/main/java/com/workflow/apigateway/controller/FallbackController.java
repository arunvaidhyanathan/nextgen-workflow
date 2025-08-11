package com.workflow.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/entitlement")
    @PostMapping("/entitlement")
    @PutMapping("/entitlement")
    @DeleteMapping("/entitlement")
    public ResponseEntity<Map<String, Object>> entitlementFallback() {
        return createFallbackResponse("Entitlement Service");
    }

    @GetMapping("/workflow")
    @PostMapping("/workflow")
    @PutMapping("/workflow")
    @DeleteMapping("/workflow")
    public ResponseEntity<Map<String, Object>> workflowFallback() {
        return createFallbackResponse("Workflow Service");
    }

    @GetMapping("/cms")
    @PostMapping("/cms")
    @PutMapping("/cms")
    @DeleteMapping("/cms")
    public ResponseEntity<Map<String, Object>> cmsFallback() {
        return createFallbackResponse("CMS Service");
    }

    private ResponseEntity<Map<String, Object>> createFallbackResponse(String serviceName) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Service Temporarily Unavailable");
        response.put("message", serviceName + " is currently unavailable. Please try again later.");
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }
}