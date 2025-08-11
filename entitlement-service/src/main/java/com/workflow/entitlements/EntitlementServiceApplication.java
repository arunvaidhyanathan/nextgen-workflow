package com.workflow.entitlements;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main application class for the Entitlement Service.
 * This service manages user entitlements, business applications, roles,
 * and provides authorization decisions through Cerbos integration.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EntitlementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EntitlementServiceApplication.class, args);
    }
}