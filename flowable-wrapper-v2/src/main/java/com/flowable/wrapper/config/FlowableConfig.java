package com.flowable.wrapper.config;

import org.springframework.context.annotation.Configuration;

/**
 * Flowable configuration class.
 * 
 * Note: We rely on Flowable's auto-configuration from flowable-spring-boot-starter.
 * The custom TaskService bean naming conflict has been resolved by using @Qualifier.
 */
@Configuration
public class FlowableConfig {
    
    // Flowable auto-configuration will handle creating all the necessary beans
    // including ProcessEngine, RuntimeService, TaskService, etc.
    
}