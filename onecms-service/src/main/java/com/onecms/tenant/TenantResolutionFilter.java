package com.onecms.tenant;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter to resolve and set tenant context for incoming requests
 */
@Component
@Order(1) // Execute early in filter chain
@Slf4j
public class TenantResolutionFilter implements Filter {
    
    // Header names for tenant resolution (in priority order)
    private static final List<String> TENANT_HEADERS = Arrays.asList(
        "X-Tenant-Id",
        "X-Tenant",
        "Tenant-Id"
    );
    
    // Paths that don't require tenant context
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/actuator",
        "/api/actuator", 
        "/swagger-ui",
        "/api-docs",
        "/health"
    );
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestUri = httpRequest.getRequestURI();
        
        try {
            // Skip tenant resolution for excluded paths
            if (isExcludedPath(requestUri)) {
                log.trace("Skipping tenant resolution for excluded path: {}", requestUri);
                chain.doFilter(request, response);
                return;
            }
            
            // Resolve tenant ID from request
            String tenantId = resolveTenantId(httpRequest);
            
            // Set tenant context
            TenantContext.setCurrentTenant(tenantId);
            
            log.debug("Resolved tenant '{}' for request: {} {}", 
                tenantId, httpRequest.getMethod(), requestUri);
            
            // Add tenant to response headers for debugging
            httpResponse.setHeader("X-Resolved-Tenant", tenantId);
            
            // Continue filter chain
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("Error in tenant resolution filter for {}: {}", requestUri, e.getMessage(), e);
            
            // Clear any partial tenant context
            TenantContext.clear();
            
            // Return error response
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            httpResponse.getWriter().write("Tenant resolution failed: " + e.getMessage());
            return;
            
        } finally {
            // Always clear tenant context to prevent leakage
            TenantContext.clear();
        }
    }
    
    /**
     * Resolve tenant ID from HTTP request using multiple strategies
     */
    private String resolveTenantId(HttpServletRequest request) {
        
        // Strategy 1: Check HTTP headers (primary method)
        String tenantId = resolveTenantFromHeaders(request);
        if (StringUtils.hasText(tenantId)) {
            log.debug("Resolved tenant from headers: {}", tenantId);
            return tenantId;
        }
        
        // Strategy 2: Extract from URL path
        tenantId = resolveTenantFromPath(request);
        if (StringUtils.hasText(tenantId)) {
            log.debug("Resolved tenant from URL path: {}", tenantId);
            return tenantId;
        }
        
        // Strategy 3: Extract from subdomain (future implementation)
        tenantId = resolveTenantFromSubdomain(request);
        if (StringUtils.hasText(tenantId)) {
            log.debug("Resolved tenant from subdomain: {}", tenantId);
            return tenantId;
        }
        
        // Fallback: Use default tenant
        log.debug("No tenant resolution method succeeded, using default tenant");
        return TenantContext.getDefaultTenant();
    }
    
    /**
     * Resolve tenant from HTTP headers
     */
    private String resolveTenantFromHeaders(HttpServletRequest request) {
        for (String headerName : TENANT_HEADERS) {
            String tenantId = request.getHeader(headerName);
            if (StringUtils.hasText(tenantId)) {
                return validateTenantId(tenantId);
            }
        }
        return null;
    }
    
    /**
     * Resolve tenant from URL path patterns
     * Supports patterns like: /api/tenant/{tenantId}/...
     */
    private String resolveTenantFromPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        
        // Pattern: /api/tenant/{tenantId}/...
        if (requestUri.startsWith("/api/tenant/")) {
            String[] pathParts = requestUri.split("/");
            if (pathParts.length >= 4) {
                return validateTenantId(pathParts[3]);
            }
        }
        
        // Pattern: /api/v1/{tenantId}/...
        if (requestUri.matches("/api/v\\d+/[^/]+/.*")) {
            String[] pathParts = requestUri.split("/");
            if (pathParts.length >= 4) {
                return validateTenantId(pathParts[3]);
            }
        }
        
        return null;
    }
    
    /**
     * Resolve tenant from subdomain (for future implementation)
     * Example: acme.nextgen-workflow.com -> tenant: acme
     */
    private String resolveTenantFromSubdomain(HttpServletRequest request) {
        String serverName = request.getServerName();
        
        if (serverName != null && serverName.contains(".")) {
            String[] parts = serverName.split("\\.");
            if (parts.length >= 3) { // subdomain.domain.tld
                String subdomain = parts[0];
                if (!"www".equals(subdomain) && !"api".equals(subdomain)) {
                    return validateTenantId(subdomain);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Validate and sanitize tenant ID
     */
    private String validateTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        
        // Clean and validate tenant ID
        tenantId = tenantId.trim().toLowerCase();
        
        // Validate format: alphanumeric, hyphens, underscores only
        if (!tenantId.matches("^[a-z0-9_-]+$")) {
            log.warn("Invalid tenant ID format: {}", tenantId);
            throw new IllegalArgumentException("Invalid tenant ID format: " + tenantId);
        }
        
        // Check length limits
        if (tenantId.length() > 50) {
            log.warn("Tenant ID too long: {}", tenantId);
            throw new IllegalArgumentException("Tenant ID too long: " + tenantId);
        }
        
        return tenantId;
    }
    
    /**
     * Check if request path should skip tenant resolution
     */
    private boolean isExcludedPath(String requestUri) {
        return EXCLUDED_PATHS.stream()
            .anyMatch(excludedPath -> requestUri.startsWith(excludedPath));
    }
}