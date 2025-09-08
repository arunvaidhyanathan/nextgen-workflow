package com.workflow.entitlements.config;

import com.workflow.entitlements.service.authorization.AuthorizationEngine;
import com.workflow.entitlements.service.authorization.DatabaseAuthorizationEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for authorization engine selection.
 * 
 * This configuration provides proper bean selection based on application properties.
 * It ensures only one AuthorizationEngine implementation is active at runtime.
 */
@Configuration
@Slf4j
public class AuthorizationConfig {
    
    /**
     * Primary authorization engine bean selector.
     * This bean will be injected into HybridAuthorizationService.
     * 
     * @param cerbosEngine Cerbos-based authorization engine
     * @param databaseEngine Database-based authorization engine
     * @param useCerbos Configuration flag for engine selection
     * @return The selected authorization engine
     */
    @Bean
    @Primary
    public AuthorizationEngine authorizationEngine(
            DatabaseAuthorizationEngine databaseEngine,
            @Value("${authorization.engine.use-cerbos:false}") boolean useCerbos) {
        
        if (useCerbos) {
            log.warn("Cerbos engine requested but not available - falling back to Database engine");
        }
        
        log.info("Initializing hybrid authorization system with Database engine");
        return databaseEngine;
    }
    
    /**
     * Configure engine switching at runtime (if needed for testing).
     * This provides a way to programmatically switch engines during runtime testing.
     */
    @Bean("authorizationEngineConfig")
    public AuthorizationEngineConfig authorizationEngineConfig(
            @Value("${authorization.engine.use-cerbos:false}") boolean useCerbos,
            @Value("${authorization.engine.allow-runtime-switching:false}") boolean allowRuntimeSwitching) {
        
        return AuthorizationEngineConfig.builder()
                .useCerbos(useCerbos)
                .allowRuntimeSwitching(allowRuntimeSwitching)
                .build();
    }
    
    /**
     * Configuration holder for authorization engine settings
     */
    public static class AuthorizationEngineConfig {
        private final boolean useCerbos;
        private final boolean allowRuntimeSwitching;
        
        private AuthorizationEngineConfig(boolean useCerbos, boolean allowRuntimeSwitching) {
            this.useCerbos = useCerbos;
            this.allowRuntimeSwitching = allowRuntimeSwitching;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public boolean isUseCerbos() { return useCerbos; }
        public boolean isAllowRuntimeSwitching() { return allowRuntimeSwitching; }
        
        public static class Builder {
            private boolean useCerbos = false;
            private boolean allowRuntimeSwitching = false;
            
            public Builder useCerbos(boolean useCerbos) {
                this.useCerbos = useCerbos;
                return this;
            }
            
            public Builder allowRuntimeSwitching(boolean allowRuntimeSwitching) {
                this.allowRuntimeSwitching = allowRuntimeSwitching;
                return this;
            }
            
            public AuthorizationEngineConfig build() {
                return new AuthorizationEngineConfig(useCerbos, allowRuntimeSwitching);
            }
        }
    }
}