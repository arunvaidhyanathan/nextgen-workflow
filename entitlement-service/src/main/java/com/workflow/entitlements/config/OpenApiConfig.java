package com.workflow.entitlements.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for NextGen Workflow Entitlement Service
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NextGen Workflow - Entitlement Service API")
                        .version("1.0.0")
                        .description("""
                            **NextGen Workflow Entitlement Service**
                            
                            This service provides centralized authorization and user management capabilities for the NextGen Workflow ecosystem.
                            
                            ## Key Features
                            - **Hybrid Authorization**: Supports both Cerbos ABAC and database-driven RBAC
                            - **User Management**: Complete CRUD operations for users, roles, and permissions
                            - **Business Application Integration**: Multi-tenant application support
                            - **Session-Based Authentication**: Secure session management with X-User-Id headers
                            - **Policy-Based Authorization**: Fine-grained access control using Cerbos policies
                            
                            ## Authentication
                            All endpoints require a valid `X-User-Id` header containing the authenticated user's UUID.
                            This header is typically set by the API Gateway after session validation.
                            
                            ## Authorization Engines
                            - **Database Engine**: Traditional role-based access control using database queries
                            - **Cerbos Engine**: Attribute-based access control using Cerbos policy engine
                            - **Hybrid Mode**: Combines both engines for maximum flexibility
                            """)
                        .contact(new Contact()
                                .name("NextGen Workflow Team")
                                .email("support@nextgenworkflow.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://nextgenworkflow.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Development Server"),
                        new Server()
                                .url("https://api.nextgenworkflow.com")
                                .description("Production Server")))
                .tags(List.of(
                        new Tag()
                                .name("authorization-controller")
                                .description("**Core Authorization Endpoints** - Primary authorization checks and policy evaluation"),
                        new Tag()
                                .name("user-controller")
                                .description("**User Management** - CRUD operations for user accounts and profiles"),
                        new Tag()
                                .name("business-application-controller")
                                .description("**Application Management** - Business application registration and configuration"),
                        new Tag()
                                .name("business-app-role-controller")
                                .description("**Role Management** - Application-specific role definitions and management"),
                        new Tag()
                                .name("user-business-app-role-controller")
                                .description("**User Role Assignments** - Assign and manage user roles within applications"),
                        new Tag()
                                .name("health-controller")
                                .description("**Health Checks** - Service health monitoring and diagnostics")
                ));
    }
}