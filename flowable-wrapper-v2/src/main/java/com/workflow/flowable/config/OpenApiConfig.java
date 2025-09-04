package com.workflow.flowable.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 configuration for Flowable Core Workflow Service
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI flowableWorkflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flowable Core Workflow Service API")
                        .description("Comprehensive BPMN workflow orchestration and task management API for NextGen Workflow Platform")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Workflow Development Team")
                                .email("workflow-dev@company.com")
                                .url("https://company.com/workflow"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://company.com/licenses/proprietary")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development Server"),
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("API Gateway (Recommended)")
                ))
                .components(new Components()
                        .addSecuritySchemes("X-User-Id", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-User-Id")
                                .description("User ID header for authentication and authorization"))
                        .addSecuritySchemes("X-Session-Id", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Session-Id")
                                .description("Session ID header for authentication (when using API Gateway)")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("X-User-Id")
                        .addList("X-Session-Id"));
    }
}