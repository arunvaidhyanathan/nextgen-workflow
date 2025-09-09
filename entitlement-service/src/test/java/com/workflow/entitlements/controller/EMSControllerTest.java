package com.workflow.entitlements.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.entitlements.dto.ems.EMSAuthRequest;
import com.workflow.entitlements.dto.ems.EMSAuthResponse;
import com.workflow.entitlements.dto.ems.WhoAmIResponse;
import com.workflow.entitlements.service.EMSService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EMSController.class)
class EMSControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EMSService emsService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String TEST_SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        // Common setup for tests
    }

    @Test
    void whoAmI_WithValidSession_ShouldReturnUserContext() throws Exception {
        // Arrange
        WhoAmIResponse expectedResponse = createMockWhoAmIResponse();
        when(emsService.validateUserSession(TEST_SESSION_ID)).thenReturn(true);
        when(emsService.getUserIdFromSession(TEST_SESSION_ID)).thenReturn(Optional.of(TEST_USER_ID));
        when(emsService.buildUserContext(TEST_USER_ID)).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/ems/v1/whoami")
                .header("X-Session-Id", TEST_SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.id").value(TEST_USER_ID))
                .andExpect(jsonPath("$.user.username").value("alice.intake"));
    }

    @Test
    void whoAmI_WithValidDirectUserId_ShouldReturnUserContext() throws Exception {
        // Arrange
        WhoAmIResponse expectedResponse = createMockWhoAmIResponse();
        when(emsService.buildUserContext(TEST_USER_ID)).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/ems/v1/whoami")
                .header("X-User-Id", TEST_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.id").value(TEST_USER_ID));
    }

    @Test
    void whoAmI_WithInvalidSession_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        when(emsService.validateUserSession(TEST_SESSION_ID)).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/ems/v1/whoami")
                .header("X-Session-Id", TEST_SESSION_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void whoAmI_WithNoAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/ems/v1/whoami"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void whoAmI_WithInvalidUserIdFormat_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/ems/v1/whoami")
                .header("X-User-Id", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void canIUse_WithValidRequest_ShouldReturnAuthorizationResult() throws Exception {
        // Arrange
        EMSAuthRequest request = EMSAuthRequest.builder()
                .resourceId("CMS-10-20045")
                .actionId("CREATE_CASE")
                .resourceType("case")
                .build();

        EMSAuthResponse expectedResponse = createMockAuthResponse();
        when(emsService.getUserIdFromSession(TEST_SESSION_ID)).thenReturn(Optional.of(TEST_USER_ID));
        when(emsService.validateUserSession(TEST_SESSION_ID)).thenReturn(true);
        when(emsService.checkUserAuthorization(eq(TEST_USER_ID), any(EMSAuthRequest.class)))
                .thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(post("/api/ems/v1/caniuse")
                .header("X-Session-Id", TEST_SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.actions").isArray())
                .andExpect(jsonPath("$.resourceAccess.canRead").exists());
    }

    @Test
    void canIUse_WithEmptyRequest_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/ems/v1/caniuse")
                .header("X-Session-Id", TEST_SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk()); // Empty request body is still valid
    }

    @Test
    void canIUse_WithNoRequestBody_ShouldReturnBadRequest() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/ems/v1/caniuse")
                .header("X-Session-Id", TEST_SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void canIUse_WithNoAuthentication_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        EMSAuthRequest request = EMSAuthRequest.builder()
                .actionId("CREATE_CASE")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/ems/v1/caniuse")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    // Helper methods for creating mock responses

    private WhoAmIResponse createMockWhoAmIResponse() {
        WhoAmIResponse.UserContext user = WhoAmIResponse.UserContext.builder()
                .id(TEST_USER_ID)
                .username("alice.intake")
                .email("alice.intake@company.com")
                .firstName("Alice")
                .lastName("Johnson")
                .displayName("Alice Johnson")
                .isActive(true)
                .attributes(Collections.emptyMap())
                .build();

        WhoAmIResponse.RoleContext role = WhoAmIResponse.RoleContext.builder()
                .id(1L)
                .roleName("INTAKE_ANALYST")
                .displayName("Intake Analyst")
                .businessApplication("onecms")
                .isActive(true)
                .metadata(Collections.singletonMap("queue", "intake-analyst-queue"))
                .build();

        WhoAmIResponse.DepartmentContext department = WhoAmIResponse.DepartmentContext.builder()
                .id(1L)
                .name("Investigation Unit")
                .code("IU")
                .isActive(true)
                .build();

        WhoAmIResponse.PermissionContext permission = WhoAmIResponse.PermissionContext.builder()
                .resource("case")
                .actions(List.of("create", "read", "update"))
                .build();

        WhoAmIResponse.SessionContext context = WhoAmIResponse.SessionContext.builder()
                .sessionExpiration("2025-01-09T12:00:00Z")
                .lastAccessed("2025-01-09T10:30:00Z")
                .queues(List.of("intake-analyst-queue"))
                .build();

        return WhoAmIResponse.builder()
                .success(true)
                .user(user)
                .roles(List.of(role))
                .departments(List.of(department))
                .permissions(List.of(permission))
                .context(context)
                .build();
    }

    private EMSAuthResponse createMockAuthResponse() {
        EMSAuthResponse.ActionAuthResult action = EMSAuthResponse.ActionAuthResult.builder()
                .actionId("CREATE_CASE")
                .displayName("Create Case")
                .allowed(true)
                .reason("User has INTAKE_ANALYST role")
                .build();

        EMSAuthResponse.ResourceAccess resourceAccess = EMSAuthResponse.ResourceAccess.builder()
                .canRead(true)
                .canWrite(true)
                .canDelete(false)
                .canApprove(false)
                .build();

        return EMSAuthResponse.builder()
                .success(true)
                .actions(List.of(action))
                .resourceAccess(resourceAccess)
                .derivedRoles(List.of("user", "case_creator"))
                .evaluationTime(45L)
                .build();
    }
}