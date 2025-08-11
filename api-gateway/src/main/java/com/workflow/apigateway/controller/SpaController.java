package com.workflow.apigateway.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Controller to handle SPA (Single Page Application) routing.
 * Serves index.html for all non-API routes to support React Router.
 */
@RestController
public class SpaController {

    @GetMapping(value = {
        "/", 
        "/dashboard", 
        "/dashboard/**",
        "/case/**",
        "/login",
        "/cases/**"
    }, produces = MediaType.TEXT_HTML_VALUE)
    public Mono<Resource> spa(ServerWebExchange exchange) {
        // Serve React app's index.html for SPA routes
        return Mono.just(new ClassPathResource("static/index.html"));
    }
}