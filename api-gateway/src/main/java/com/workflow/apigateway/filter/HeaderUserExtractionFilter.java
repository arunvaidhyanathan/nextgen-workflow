package com.workflow.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Gateway filter to extract user ID from session and add as X-User-Id header
 * for downstream services that need user identification.
 */
@Component
public class HeaderUserExtractionFilter extends AbstractGatewayFilterFactory<HeaderUserExtractionFilter.Config> {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String SESSION_ID_HEADER = "X-Session-Id";
    private final WebClient webClient = WebClient.builder().build();

    public HeaderUserExtractionFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            System.out.println("HeaderUserExtractionFilter invoked for path: " + request.getPath());
            
            // Extract session ID header
            String sessionId = request.getHeaders().getFirst(SESSION_ID_HEADER);
            System.out.println("Session ID header: " + sessionId);
            
            if (sessionId == null || sessionId.isEmpty()) {
                System.out.println("No session ID found, continuing without user ID");
                return chain.filter(exchange);
            }

            System.out.println("Found session ID, validating with entitlement service");
            
            // Validate session and extract user ID
            return validateSessionAndExtractUserId(sessionId)
                .flatMap(userId -> {
                    System.out.println("Extracted userId from session: " + userId);
                    if (userId != null && !userId.isEmpty()) {
                        // Add X-User-Id header to the request
                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header(USER_ID_HEADER, userId)
                                .build();
                        System.out.println("Added X-User-Id header: " + userId);
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    } else {
                        System.out.println("Empty userId, continuing without user ID header");
                        return chain.filter(exchange);
                    }
                })
                .onErrorResume(error -> {
                    // Log error and continue without user ID header
                    System.err.println("Failed to validate session: " + error.getMessage());
                    error.printStackTrace();
                    return chain.filter(exchange);
                });
        };
    }

    /**
     * Validate session with entitlement service and extract user ID
     */
    private Mono<String> validateSessionAndExtractUserId(String sessionId) {
        return webClient.get()
                .uri("http://localhost:8081/api/auth/validate-session")
                .header(SESSION_ID_HEADER, sessionId)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Boolean success = (Boolean) response.get("success");
                    if (Boolean.TRUE.equals(success)) {
                        Object userObj = response.get("user");
                        if (userObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> user = (Map<String, Object>) userObj;
                            return (String) user.get("id");
                        }
                    }
                    return ""; // Return empty string instead of null
                })
                .onErrorReturn(""); // Return empty string if validation fails
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}