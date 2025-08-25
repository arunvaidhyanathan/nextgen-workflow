package com.flowable.wrapper.security;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of BPMN security validation containing violations and warnings
 */
@Data
public class BpmnValidationResult {
    
    private List<BpmnViolation> violations = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private boolean valid = true;
    
    /**
     * Add a security violation
     */
    public void addViolation(BpmnViolationType type, String message) {
        violations.add(new BpmnViolation(type, message));
        if (type.isBlocking()) {
            valid = false;
        }
    }
    
    /**
     * Add a warning
     */
    public void addWarning(String message) {
        warnings.add(message);
    }
    
    /**
     * Check if there are any blocking violations
     */
    public boolean hasBlockingViolations() {
        return violations.stream().anyMatch(v -> v.getType().isBlocking());
    }
    
    /**
     * Get count of violations
     */
    public int getViolationCount() {
        return violations.size();
    }
    
    /**
     * Get count of warnings
     */
    public int getWarningCount() {
        return warnings.size();
    }
    
    /**
     * Check if validation passed (no blocking violations)
     */
    public boolean isValid() {
        return !hasBlockingViolations();
    }
    
    /**
     * Get summary of validation result
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Validation Result: %s violations, %s warnings", 
            getViolationCount(), getWarningCount()));
        
        if (!violations.isEmpty()) {
            sb.append("\nViolations:");
            violations.forEach(v -> sb.append("\n  - ").append(v.getType()).append(": ").append(v.getMessage()));
        }
        
        if (!warnings.isEmpty()) {
            sb.append("\nWarnings:");
            warnings.forEach(w -> sb.append("\n  - ").append(w));
        }
        
        return sb.toString();
    }
    
    /**
     * Individual BPMN violation
     */
    @Data
    public static class BpmnViolation {
        private final BpmnViolationType type;
        private final String message;
        
        public BpmnViolation(BpmnViolationType type, String message) {
            this.type = type;
            this.message = message;
        }
    }
}