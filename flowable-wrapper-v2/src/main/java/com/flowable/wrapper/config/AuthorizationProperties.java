package com.flowable.wrapper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "authorization")
public class AuthorizationProperties {
    
    /**
     * Enable/disable authorization checks.
     * When false, all authorization checks will be bypassed.
     */
    private boolean enabled = false;
}