package com.flowable.wrapper.client;

import com.flowable.wrapper.dto.request.AuthorizationCheckRequest;
import com.flowable.wrapper.dto.response.AuthorizationCheckResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class EntitlementServiceClient {

    private final WebClient webClient;
    private final String baseUrl;
    private final Duration timeout;

    public EntitlementServiceClient(WebClient.Builder webClientBuilder,
                                  @Value("${entitlement-service.base-url}") String baseUrl,
                                  @Value("${entitlement-service.timeout:5000}") int timeoutMs) {
        this.baseUrl = baseUrl;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    @CircuitBreaker(name = "entitlement-service", fallbackMethod = "fallbackAuthorizationCheck")
    public AuthorizationCheckResponse checkAuthorization(String userId, List<String> userRoles,
                                                        String resourceKind, String resourceId,
                                                        Map<String, Object> resourceAttributes,
                                                        String action) {
        log.debug("Checking authorization for user: {}, action: {}, resource: {}/{}", 
                  userId, action, resourceKind, resourceId);
        
        AuthorizationCheckRequest request = AuthorizationCheckRequest.builder()
                .principal(AuthorizationCheckRequest.Principal.builder()
                        .id(userId)
                        .roles(userRoles)
                        .build())
                .resource(AuthorizationCheckRequest.Resource.builder()
                        .kind(resourceKind)
                        .id(resourceId)
                        .attributes(resourceAttributes)
                        .build())
                .action(action)
                .build();

        try {
            AuthorizationCheckResponse response = webClient.post()
                    .uri("/api/entitlements/check")
                    .body(Mono.just(request), AuthorizationCheckRequest.class)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                        log.warn("Client error when checking authorization: {}", clientResponse.statusCode());
                        return Mono.error(new RuntimeException("Authorization check failed with client error"));
                    })
                    .onStatus(status -> status.is5xxServerError(), serverResponse -> {
                        log.error("Server error when checking authorization: {}", serverResponse.statusCode());
                        return Mono.error(new RuntimeException("Authorization service unavailable"));
                    })
                    .bodyToMono(AuthorizationCheckResponse.class)
                    .timeout(timeout)
                    .block();

            log.debug("Authorization check result for user {}: {}", userId, response.isAllowed());
            return response;

        } catch (WebClientResponseException e) {
            log.error("Error calling entitlement service: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Authorization check failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during authorization check", e);
            throw new RuntimeException("Authorization check failed", e);
        }
    }

    // Circuit breaker fallback method
    public AuthorizationCheckResponse fallbackAuthorizationCheck(String userId, List<String> userRoles,
                                                               String resourceKind, String resourceId,
                                                               Map<String, Object> resourceAttributes,
                                                               String action, Exception ex) {
        log.error("Authorization service circuit breaker opened. Denying access for user: {} action: {} resource: {}/{}. Error: {}", 
                  userId, action, resourceKind, resourceId, ex.getMessage());
        
        // Security-first approach: Deny access when authorization service is unavailable
        return AuthorizationCheckResponse.denied("Authorization service unavailable");
    }
}