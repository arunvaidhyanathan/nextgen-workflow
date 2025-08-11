package com.flowable.wrapper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the Flowable Core Workflow Service.
 * This service provides core workflow orchestration capabilities,
 * delegating authorization decisions to the Entitlement Service.
 */
@SpringBootApplication
@EnableDiscoveryClient  
public class FlowableWrapperApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowableWrapperApplication.class, args);
    }
}