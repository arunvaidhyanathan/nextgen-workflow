package com.workflow.entitlements.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import java.util.Map;

/**
 * Comprehensive end-to-end tests for the hybrid authorization system.
 * Tests validate both UUID-based operations and authorization engine functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Hybrid Authorization System - End-to-End Tests")
public class HybridSystemEndToEndTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @Order(1)
    @DisplayName("E2E-001: System Status and Health Check")
    public void testSystemStatusAndHealth() throws Exception {
        setUp();
        
        mockMvc.perform(get("/api/system-test/status")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hybridSchema", is("active")))
                .andExpect(jsonPath("$.entityCounts.totalUsers", greaterThan(0)))
                .andExpect(jsonPath("$.entityCounts.activeUsers", greaterThan(0)))
                .andExpect(jsonPath("$.entityCounts.totalRoleAssignments", greaterThan(0)))
                .andExpect(jsonPath("$.entityCounts.activeRoleAssignments", greaterThan(0)))
                .andExpect(jsonPath("$.systemReady", is(true)))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @Order(2)
    @DisplayName("E2E-002: UUID User Operations Test")
    public void testUuidUserOperations() throws Exception {
        setUp();
        
        mockMvc.perform(get("/api/system-test/users/uuid-test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActiveUsers", greaterThan(0)))
                .andExpect(jsonPath("$.aliceFound", is(true)))
                .andExpect(jsonPath("$.bobFound", is(true)))
                .andExpect(jsonPath("$.aliceUserId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.bobUserId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.sampleUser.username", is("alice.intake")))
                .andExpect(jsonPath("$.sampleUser.userId", notNullValue()));
    }

    @Test
    @Order(3)
    @DisplayName("E2E-003: UUID User-Role Assignment Test")
    public void testUuidUserRoleAssignments() throws Exception {
        setUp();
        
        mockMvc.perform(get("/api/system-test/user-roles/uuid-test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aliceUserId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.totalRoles", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.activeRoles", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.roleDetails").isArray())
                .andExpect(jsonPath("$.roleDetails[0].roleName", notNullValue()))
                .andExpect(jsonPath("$.roleDetails[0].applicationName", notNullValue()))
                .andExpect(jsonPath("$.roleDetails[0].isActive", is(true)));
    }

    @Test
    @Order(4)
    @DisplayName("E2E-004: Authorization Engine - Alice Intake Analyst Case Create")
    public void testAuthorizationEngineAliceIntake() throws Exception {
        setUp();
        
        String testPayload = objectMapper.writeValueAsString(Map.of(
            "username", "alice.intake",
            "resource", "case", 
            "action", "create"
        ));
        
        mockMvc.perform(post("/api/system-test/authorization/engine-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("alice.intake")))
                .andExpect(jsonPath("$.userId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.resource", is("case")))
                .andExpect(jsonPath("$.action", is("create")))
                .andExpect(jsonPath("$.authorizationResult.allowed", is(true)))
                .andExpect(jsonPath("$.authorizationResult.message", notNullValue()));
    }

    @Test
    @Order(5)
    @DisplayName("E2E-005: Authorization Engine - Bob Investigator Case Update")
    public void testAuthorizationEngineBobInvestigator() throws Exception {
        setUp();
        
        String testPayload = objectMapper.writeValueAsString(Map.of(
            "username", "bob.investigator",
            "resource", "case",
            "action", "update"
        ));
        
        mockMvc.perform(post("/api/system-test/authorization/engine-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("bob.investigator")))
                .andExpect(jsonPath("$.userId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.resource", is("case")))
                .andExpect(jsonPath("$.action", is("update")))
                .andExpect(jsonPath("$.authorizationResult.allowed", is(true)))
                .andExpect(jsonPath("$.authorizationResult.message", notNullValue()));
    }

    @Test
    @Order(6)
    @DisplayName("E2E-006: Authorization Engine - Carol Legal Case Review")
    public void testAuthorizationEngineCarolLegal() throws Exception {
        setUp();
        
        String testPayload = objectMapper.writeValueAsString(Map.of(
            "username", "carol.legal",
            "resource", "case",
            "action", "approve"
        ));
        
        mockMvc.perform(post("/api/system-test/authorization/engine-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("carol.legal")))
                .andExpect(jsonPath("$.userId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.resource", is("case")))
                .andExpect(jsonPath("$.action", is("approve")))
                .andExpect(jsonPath("$.authorizationResult.allowed", is(true)))
                .andExpect(jsonPath("$.authorizationResult.message", notNullValue()));
    }

    @Test
    @Order(7)
    @DisplayName("E2E-007: Authorization Engine - Henry Admin System Access")
    public void testAuthorizationEngineHenryAdmin() throws Exception {
        setUp();
        
        String testPayload = objectMapper.writeValueAsString(Map.of(
            "username", "henry.admin",
            "resource", "system",
            "action", "admin"
        ));
        
        mockMvc.perform(post("/api/system-test/authorization/engine-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("henry.admin")))
                .andExpect(jsonPath("$.userId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.resource", is("system")))
                .andExpect(jsonPath("$.action", is("admin")))
                .andExpect(jsonPath("$.authorizationResult.allowed", is(true)))
                .andExpect(jsonPath("$.authorizationResult.message", notNullValue()));
    }

    @Test
    @Order(8)
    @DisplayName("E2E-008: Authorization Engine - Unauthorized User Access Denial")  
    public void testAuthorizationEngineUnauthorizedAccess() throws Exception {
        setUp();
        
        String testPayload = objectMapper.writeValueAsString(Map.of(
            "username", "david.hr", // HR user trying system admin action
            "resource", "system",
            "action", "admin"
        ));
        
        mockMvc.perform(post("/api/system-test/authorization/engine-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("david.hr")))
                .andExpect(jsonPath("$.userId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.resource", is("system")))
                .andExpect(jsonPath("$.action", is("admin")))
                .andExpect(jsonPath("$.authorizationResult.allowed", is(false)))
                .andExpect(jsonPath("$.authorizationResult.message", containsString("permissions")));
    }

    @Test
    @Order(9)
    @DisplayName("E2E-009: Cross-Department Resource Access Test")
    public void testCrossDepartmentResourceAccess() throws Exception {
        setUp();
        
        String testPayload = objectMapper.writeValueAsString(Map.of(
            "username", "iris.csis", // CSIS user
            "resource", "case",
            "action", "read"
        ));
        
        mockMvc.perform(post("/api/system-test/authorization/engine-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("iris.csis")))
                .andExpect(jsonPath("$.userId", matchesPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")))
                .andExpect(jsonPath("$.resource", is("case")))
                .andExpect(jsonPath("$.action", is("read")))
                .andExpect(jsonPath("$.authorizationResult", notNullValue()));
    }

    @Test
    @Order(10)  
    @DisplayName("E2E-010: Invalid User Authorization Test")
    public void testInvalidUserAuthorization() throws Exception {
        setUp();
        
        String testPayload = objectMapper.writeValueAsString(Map.of(
            "username", "nonexistent.user",
            "resource", "case", 
            "action", "read"
        ));
        
        mockMvc.perform(post("/api/system-test/authorization/engine-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(testPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error", containsString("User not found")));
    }
}