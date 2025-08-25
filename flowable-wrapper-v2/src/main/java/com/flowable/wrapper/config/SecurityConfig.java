package com.flowable.wrapper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import java.util.Set;
import java.util.List;

/**
 * Security configuration properties for BPMN validation
 */
@Configuration
@ConfigurationProperties(prefix = "workflow.security")
@Data
public class SecurityConfig {
    
    /**
     * Maximum number of elements allowed in a BPMN process
     */
    private int maxProcessElements = 200;
    
    /**
     * Maximum length of script content in characters
     */
    private int maxScriptLength = 1000;
    
    /**
     * Maximum length of expressions in characters
     */
    private int maxExpressionLength = 500;
    
    /**
     * Allowed script formats
     */
    private Set<String> allowedScriptFormats = Set.of("groovy", "javascript", "juel");
    
    /**
     * Blocked Java packages that should not be accessible
     */
    private Set<String> blockedJavaPackages = Set.of(
        "java.lang.Runtime", 
        "java.lang.ProcessBuilder", 
        "java.io.File",
        "java.nio.file", 
        "java.lang.System", 
        "java.lang.reflect"
    );
    
    /**
     * Whitelisted Java packages that are allowed
     */
    private Set<String> whitelistedPackages = Set.of(
        "com.flowable.",
        "com.workflow.",
        "org.flowable."
    );
    
    /**
     * Enable/disable security validation
     */
    private boolean validationEnabled = true;
    
    /**
     * Enable/disable strict mode (treat warnings as errors)
     */
    private boolean strictMode = false;
    
    /**
     * Custom dangerous patterns to check for
     */
    private List<String> customDangerousPatterns = List.of();
    
    /**
     * Maximum deployment size in bytes
     */
    private long maxDeploymentSize = 5 * 1024 * 1024; // 5MB
}