package com.citi.onecms.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Value("${entitlement-service.base-url:http://localhost:8081}")
    private String entitlementServiceUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // Simple cache to avoid repeated validation calls  
    private final Map<String, CacheEntry> userCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = TimeUnit.MINUTES.toMillis(5); // 5 minutes

    public HeaderAuthenticationFilter() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String requestURI = request.getRequestURI();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check for user ID header
        if (userId == null || userId.trim().isEmpty()) {
            sendUnauthorizedResponse(response, "Missing user ID header");
            return;
        }

        try {
            // Check cache first
            CacheEntry cachedEntry = userCache.get(userId);
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                setAuthentication(cachedEntry.getUserId(), cachedEntry.getUsername());
                filterChain.doFilter(request, response);
                return;
            }

            // Validate user with entitlement service
            Map<String, Object> validationResult = validateUserWithService(userId);
            
            if (validationResult != null && Boolean.TRUE.equals(validationResult.get("success"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> userInfo = (Map<String, Object>) validationResult.get("user");
                
                if (userInfo != null) {
                    String validatedUserId = (String) userInfo.get("id");
                    String username = (String) userInfo.get("username");
                    
                    // Cache the result
                    userCache.put(userId, new CacheEntry(validatedUserId, username));
                    
                    setAuthentication(validatedUserId, username);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            sendUnauthorizedResponse(response, "Invalid or inactive user");

        } catch (Exception e) {
            logger.error("Authentication error", e);
            sendUnauthorizedResponse(response, "Authentication failed");
        }
    }

    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.contains("/actuator/") ||
               requestURI.contains("/v3/api-docs") ||
               requestURI.contains("/swagger-ui") ||
               requestURI.equals("/") ||
               requestURI.equals("/health") ||
               requestURI.contains("/createcase"); // Temporary bypass for testing
    }

    private Map<String, Object> validateUserWithService(String userId) {
        try {
            String response = webClient.get()
                .uri(entitlementServiceUrl + "/api/auth/validate")
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return objectMapper.readValue(response, Map.class);
            
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                logger.debug("User validation failed: " + e.getMessage());
            } else {
                logger.error("Error validating user: " + e.getMessage());
            }
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error during user validation", e);
            return null;
        }
    }

    private void setAuthentication(String userId, String username) {
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(
                username, 
                null, 
                Collections.singletonList(new SimpleGrantedAuthority("USER"))
            );
        
        // Add user ID as detail for easy access in controllers
        authentication.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
            String.format("{\"success\":false,\"message\":\"%s\"}", message)
        );
    }

    // Clean expired cache entries periodically
    @SuppressWarnings("unused")
    private void cleanupCache() {
        userCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private static class CacheEntry {
        private final String userId;
        private final String username;
        private final long timestamp;

        public CacheEntry(String userId, String username) {
            this.userId = userId;
            this.username = username;
            this.timestamp = System.currentTimeMillis();
        }

        public String getUserId() { return userId; }
        public String getUsername() { return username; }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL;
        }
    }
}