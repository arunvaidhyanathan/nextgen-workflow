package com.workflow.entitlements.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Performance tests for the hybrid authorization system.
 * Tests evaluate response times, throughput, and resource utilization.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Hybrid Authorization System - Performance Tests")
public class HybridAuthorizationPerformanceTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private final String[] testUsers = {
        "alice.intake", "bob.investigator", "carol.legal", 
        "david.hr", "eve.manager", "frank.security", 
        "grace.ethics", "henry.admin", "iris.csis", "jack.analyst"
    };

    private final String[] testResources = {
        "case", "workflow", "evidence", "finding", "system", "user"
    };

    private final String[] testActions = {
        "create", "read", "update", "delete", "approve", "assign", "claim", "complete"
    };

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("PERF-001: Single Authorization Request Latency")
    public void testSingleAuthorizationLatency() throws Exception {
        String testPayload = objectMapper.writeValueAsString(Map.of(
            "username", "alice.intake",
            "resource", "case",
            "action", "create"
        ));

        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(post("/api/system-test/authorization/engine-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationResult").exists());
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        // Assert response time is under acceptable threshold (500ms)
        assertTrue(responseTime < 500, 
            "Single authorization request took " + responseTime + "ms, expected < 500ms");
        
        System.out.println("Single Authorization Latency: " + responseTime + "ms");
    }

    @Test
    @DisplayName("PERF-002: Bulk Authorization Requests Performance")
    public void testBulkAuthorizationPerformance() throws Exception {
        int numberOfRequests = 50;
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < numberOfRequests; i++) {
            String user = testUsers[i % testUsers.length];
            String resource = testResources[i % testResources.length];
            String action = testActions[i % testActions.length];
            
            String testPayload = objectMapper.writeValueAsString(Map.of(
                "username", user,
                "resource", resource,
                "action", action
            ));

            long startTime = System.currentTimeMillis();
            
            mockMvc.perform(post("/api/system-test/authorization/engine-test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(testPayload))
                    .andExpect(status().isOk());
            
            long responseTime = System.currentTimeMillis() - startTime;
            responseTimes.add(responseTime);
        }
        
        // Calculate statistics
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        long maxResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        
        long minResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);
        
        System.out.println("Bulk Authorization Performance (n=" + numberOfRequests + "):");
        System.out.println("  Average Response Time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("  Min Response Time: " + minResponseTime + "ms");
        System.out.println("  Max Response Time: " + maxResponseTime + "ms");
        
        // Performance assertions
        assertTrue(avgResponseTime < 200, 
            "Average response time " + String.format("%.2f", avgResponseTime) + "ms exceeds threshold of 200ms");
        assertTrue(maxResponseTime < 1000, 
            "Maximum response time " + maxResponseTime + "ms exceeds threshold of 1000ms");
    }

    @Test
    @DisplayName("PERF-003: Concurrent Authorization Requests")
    public void testConcurrentAuthorizationRequests() throws Exception {
        int concurrentThreads = 10;
        int requestsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        
        List<CompletableFuture<Long>> futures = new ArrayList<>();
        long overallStartTime = System.currentTimeMillis();
        
        for (int threadId = 0; threadId < concurrentThreads; threadId++) {
            final int tId = threadId;
            CompletableFuture<Long> future = CompletableFuture.supplyAsync(() -> {
                List<Long> threadResponseTimes = new ArrayList<>();
                
                for (int i = 0; i < requestsPerThread; i++) {
                    try {
                        String user = testUsers[(tId + i) % testUsers.length];
                        String resource = testResources[i % testResources.length];
                        String action = testActions[i % testActions.length];
                        
                        String testPayload = objectMapper.writeValueAsString(Map.of(
                            "username", user,
                            "resource", resource,
                            "action", action
                        ));

                        long startTime = System.currentTimeMillis();
                        
                        MvcResult result = mockMvc.perform(post("/api/system-test/authorization/engine-test")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(testPayload))
                                .andExpect(status().isOk())
                                .andReturn();
                        
                        long responseTime = System.currentTimeMillis() - startTime;
                        threadResponseTimes.add(responseTime);
                        
                    } catch (Exception e) {
                        System.err.println("Error in thread " + tId + ", request " + i + ": " + e.getMessage());
                        return -1L; // Error indicator
                    }
                }
                
                return threadResponseTimes.stream().mapToLong(Long::longValue).sum();
            }, executor);
            
            futures.add(future);
        }
        
        // Wait for all threads to complete
        List<Long> threadTotals = new ArrayList<>();
        for (CompletableFuture<Long> future : futures) {
            Long total = future.get(30, TimeUnit.SECONDS);
            if (total >= 0) {
                threadTotals.add(total);
            }
        }
        
        long overallDuration = System.currentTimeMillis() - overallStartTime;
        executor.shutdown();
        
        int totalRequests = concurrentThreads * requestsPerThread;
        double throughput = (double) totalRequests / (overallDuration / 1000.0);
        
        System.out.println("Concurrent Authorization Performance:");
        System.out.println("  Threads: " + concurrentThreads);
        System.out.println("  Requests per thread: " + requestsPerThread);
        System.out.println("  Total requests: " + totalRequests);
        System.out.println("  Total duration: " + overallDuration + "ms");
        System.out.println("  Throughput: " + String.format("%.2f", throughput) + " requests/second");
        System.out.println("  Successful threads: " + threadTotals.size() + "/" + concurrentThreads);
        
        // Performance assertions
        assertTrue(threadTotals.size() >= (concurrentThreads * 0.9), 
            "Less than 90% of concurrent threads completed successfully");
        assertTrue(throughput > 10, 
            "Throughput " + String.format("%.2f", throughput) + " req/s is below minimum threshold of 10 req/s");
    }

    @Test
    @DisplayName("PERF-004: System Status Endpoint Performance")
    public void testSystemStatusPerformance() throws Exception {
        int numberOfCalls = 20;
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < numberOfCalls; i++) {
            long startTime = System.currentTimeMillis();
            
            mockMvc.perform(get("/api/system-test/status")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.systemReady").exists());
            
            long responseTime = System.currentTimeMillis() - startTime;
            responseTimes.add(responseTime);
        }
        
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        System.out.println("System Status Performance (n=" + numberOfCalls + "):");
        System.out.println("  Average Response Time: " + String.format("%.2f", avgResponseTime) + "ms");
        
        // Assert system status calls are fast (< 100ms average)
        assertTrue(avgResponseTime < 100, 
            "System status average response time " + String.format("%.2f", avgResponseTime) + "ms exceeds 100ms threshold");
    }

    @Test
    @DisplayName("PERF-005: UUID User Operations Performance")
    public void testUuidUserOperationsPerformance() throws Exception {
        int numberOfCalls = 30;
        List<Long> responseTimes = new ArrayList<>();
        
        for (int i = 0; i < numberOfCalls; i++) {
            long startTime = System.currentTimeMillis();
            
            mockMvc.perform(get("/api/system-test/users/uuid-test")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.aliceFound").exists());
            
            long responseTime = System.currentTimeMillis() - startTime;
            responseTimes.add(responseTime);
        }
        
        double avgResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        
        long maxResponseTime = responseTimes.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
        
        System.out.println("UUID User Operations Performance (n=" + numberOfCalls + "):");
        System.out.println("  Average Response Time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("  Max Response Time: " + maxResponseTime + "ms");
        
        // Performance assertions for database operations
        assertTrue(avgResponseTime < 150, 
            "UUID user operations average response time " + String.format("%.2f", avgResponseTime) + "ms exceeds 150ms threshold");
        assertTrue(maxResponseTime < 500, 
            "UUID user operations max response time " + maxResponseTime + "ms exceeds 500ms threshold");
    }

    @Test
    @DisplayName("PERF-006: Memory Usage Under Load")
    public void testMemoryUsageUnderLoad() throws Exception {
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection to get baseline
        System.gc();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Execute load test
        int numberOfRequests = 100;
        for (int i = 0; i < numberOfRequests; i++) {
            String user = testUsers[i % testUsers.length];
            String resource = testResources[i % testResources.length];
            String action = testActions[i % testActions.length];
            
            String testPayload = objectMapper.writeValueAsString(Map.of(
                "username", user,
                "resource", resource,
                "action", action
            ));

            mockMvc.perform(post("/api/system-test/authorization/engine-test")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(testPayload))
                    .andExpect(status().isOk());
        }
        
        // Measure memory after load
        long memoryAfterLoad = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = memoryAfterLoad - baselineMemory;
        
        System.out.println("Memory Usage Under Load:");
        System.out.println("  Baseline Memory: " + (baselineMemory / (1024 * 1024)) + " MB");
        System.out.println("  Memory After Load: " + (memoryAfterLoad / (1024 * 1024)) + " MB");
        System.out.println("  Memory Increase: " + (memoryIncrease / (1024 * 1024)) + " MB");
        
        // Force garbage collection and check if memory returns to reasonable levels
        System.gc();
        Thread.sleep(1000); // Allow GC to complete
        long memoryAfterGC = runtime.totalMemory() - runtime.freeMemory();
        
        System.out.println("  Memory After GC: " + (memoryAfterGC / (1024 * 1024)) + " MB");
        
        // Assert memory increase is reasonable (< 50MB increase)
        assertTrue(memoryIncrease < (50 * 1024 * 1024), 
            "Memory increased by " + (memoryIncrease / (1024 * 1024)) + "MB, expected < 50MB");
    }
}