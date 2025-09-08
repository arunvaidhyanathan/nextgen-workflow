package com.workflow.entitlements.config;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CerbosConfig {
    
    @Value("${cerbos.host:localhost}")
    private String cerbosHost;
    
    @Value("${cerbos.port:3593}")
    private int cerbosPort;
    
    @Value("${cerbos.tls.enabled:false}")
    private boolean tlsEnabled;
    
    @Value("${cerbos.connection.timeout:30s}")
    private String connectionTimeout;
    
    @Value("${cerbos.connection.keep-alive:true}")
    private boolean keepAlive;
    
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        value = "authorization.engine.use-cerbos", 
        havingValue = "true", 
        matchIfMissing = false)
    public CerbosBlockingClient cerbosBlockingClient() {
        try {
            String endpoint = cerbosHost + ":" + cerbosPort;
            log.info("Initializing Cerbos client: endpoint={}, tls={}, timeout={}, keepAlive={}", 
                    endpoint, tlsEnabled, connectionTimeout, keepAlive);
            
            CerbosClientBuilder builder = new CerbosClientBuilder(endpoint);
            
            if (!tlsEnabled) {
                builder = builder.withPlaintext().withInsecure();
            }
            
            CerbosBlockingClient client = builder.buildBlockingClient();
            
            log.info("Cerbos client initialized successfully");
            return client;
            
        } catch (Exception e) {
            log.error("Failed to initialize Cerbos client: host={}, port={}, tls={}", cerbosHost, cerbosPort, tlsEnabled, e);
            throw new RuntimeException("Failed to initialize Cerbos client", e);
        }
    }
}