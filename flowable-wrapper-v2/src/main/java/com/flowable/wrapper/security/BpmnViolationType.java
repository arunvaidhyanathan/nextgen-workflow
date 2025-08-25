package com.flowable.wrapper.security;

/**
 * Types of BPMN security violations with severity levels
 */
public enum BpmnViolationType {
    
    // Critical violations that block deployment
    INVALID_XML(true, "Invalid or malformed XML structure"),
    PARSING_ERROR(true, "XML parsing failed"),
    MALICIOUS_CONTENT(true, "Potentially malicious content detected"),
    UNAUTHORIZED_API(true, "Unauthorized API or system access"),
    UNAUTHORIZED_SCRIPT(true, "Unauthorized scripting language or content"),
    RESOURCE_LIMIT(true, "Resource limits exceeded"),
    
    // Non-blocking violations that allow deployment with warnings
    INVALID_STRUCTURE(false, "Invalid BPMN structure or best practice violation"),
    SECURITY_WARNING(false, "Security best practice recommendation"),
    PERFORMANCE_WARNING(false, "Performance impact warning");
    
    private final boolean blocking;
    private final String description;
    
    BpmnViolationType(boolean blocking, String description) {
        this.blocking = blocking;
        this.description = description;
    }
    
    /**
     * Whether this violation type blocks deployment
     */
    public boolean isBlocking() {
        return blocking;
    }
    
    /**
     * Get description of violation type
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if violation is critical (blocking)
     */
    public boolean isCritical() {
        return blocking;
    }
}