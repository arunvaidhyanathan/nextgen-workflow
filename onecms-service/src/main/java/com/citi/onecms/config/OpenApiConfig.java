package com.citi.onecms.config;

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
 * OpenAPI 3 configuration for OneCMS Service API documentation
 */
@Configuration
public class OpenApiConfig {

    @Value("${cms.deployment.base-url:http://localhost}")
    private String baseUrl;

    @Value("${server.port:8083}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI oneCmsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("OneCMS Service API")
                        .description("NextGen Workflow OneCMS Service - Case Management System microservice with complete workflow integration, real BPMN process instances, and circuit breaker resilience patterns.")
                        .version("1.1.0")
                        .contact(new Contact()
                                .name("NextGen Workflow Team")
                                .email("nextgen-workflow@example.com")
                                .url("https://github.com/nextgen-workflow/onecms-service"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server()
                                .url(baseUrl + ":" + serverPort + contextPath)
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
                                .description("User ID header for authentication (required for all endpoints)"))
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