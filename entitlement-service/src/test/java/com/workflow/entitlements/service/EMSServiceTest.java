package com.workflow.entitlements.service;

import com.workflow.entitlements.controller.AuthController;
import com.workflow.entitlements.dto.ems.EMSAuthRequest;
import com.workflow.entitlements.dto.ems.EMSAuthResponse;
import com.workflow.entitlements.dto.ems.WhoAmIResponse;
import com.workflow.entitlements.dto.request.AuthorizationCheckRequest;
import com.workflow.entitlements.dto.response.AuthorizationCheckResponse;
import com.workflow.entitlements.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EMSServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserBusinessAppRoleService userBusinessAppRoleService;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private HybridAuthorizationService hybridAuthorizationService;

    @Mock
    private AuthController authController;

    @InjectMocks
    private EMSService emsService;

    private static final String TEST_USER_ID = "550e8400-e29b-41d4-a716-446655440001";
    private static final String TEST_SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private UUID testUserUuid;

    @BeforeEach
    void setUp() {
        testUserUuid = UUID.fromString(TEST_USER_ID);
    }

    @Test
    void buildUserContext_WithValidUser_ShouldReturnCompleteContext() {
        // Arrange
        User mockUser = createMockUser();
        List<UserBusinessAppRole> mockRoles = createMockUserRoles();
        List<UserDepartment> mockDepartments = createMockUserDepartments();

        when(userService.findById(testUserUuid)).thenReturn(Optional.of(mockUser));
        when(userBusinessAppRoleService.getActiveUserRolesByUserId(testUserUuid)).thenReturn(mockRoles);
        // Mock user department repository directly since we use it in the service
        // when(departmentService.getActiveDepartmentsByUserId(testUserUuid)).thenReturn(mockDepartments);

        // Act
        WhoAmIResponse result = emsService.buildUserContext(TEST_USER_ID);

        // Assert
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getUser().getId()).isEqualTo(TEST_USER_ID);
        assertThat(result.getUser().getUsername()).isEqualTo("alice.intake");
        assertThat(result.getUser().getDisplayName()).isEqualTo("Alice Johnson");
        assertThat(result.getRoles()).hasSize(1);
        assertThat(result.getRoles().get(0).getRoleName()).isEqualTo("INTAKE_ANALYST");
        assertThat(result.getDepartments()).hasSize(1);
        assertThat(result.getDepartments().get(0).getCode()).isEqualTo("IU");
        assertThat(result.getPermissions()).isNotEmpty();
        assertThat(result.getContext().getQueues()).contains("intake-analyst-queue");
    }

    @Test
    void buildUserContext_WithInvalidUser_ShouldReturnFailure() {
        // Arrange
        when(userService.findById(testUserUuid)).thenReturn(Optional.empty());

        // Act
        WhoAmIResponse result = emsService.buildUserContext(TEST_USER_ID);

        // Assert
        assertThat(result.getSuccess()).isFalse();
    }

    @Test
    void buildUserContext_WithInvalidUserId_ShouldReturnFailure() {
        // Act
        WhoAmIResponse result = emsService.buildUserContext("invalid-uuid");

        // Assert
        assertThat(result.getSuccess()).isFalse();
    }

    @Test
    void checkUserAuthorization_WithValidRequest_ShouldReturnAuthResult() {
        // Arrange
        EMSAuthRequest request = EMSAuthRequest.builder()
                .resourceId("CMS-10-20045")
                .actionId("CREATE_CASE")
                .resourceType("case")
                .build();

        User mockUser = createMockUser();
        AuthorizationCheckResponse mockAuthResponse = AuthorizationCheckResponse.builder()
                .allowed(true)
                .message("Access granted")
                .build();

        when(userService.findById(testUserUuid)).thenReturn(Optional.of(mockUser));
        when(hybridAuthorizationService.checkAuthorization(any(AuthorizationCheckRequest.class)))
                .thenReturn(mockAuthResponse);
        when(userBusinessAppRoleService.getActiveUserRolesByUserId(testUserUuid))
                .thenReturn(createMockUserRoles());

        // Act
        EMSAuthResponse result = emsService.checkUserAuthorization(TEST_USER_ID, request);

        // Assert
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getActions()).hasSize(1);
        assertThat(result.getActions().get(0).getActionId()).isEqualTo("CREATE_CASE");
        assertThat(result.getActions().get(0).getAllowed()).isTrue();
        assertThat(result.getResourceAccess().getCanRead()).isTrue();
        assertThat(result.getEvaluationTime()).isGreaterThan(0);
    }

    @Test
    void checkUserAuthorization_WithInvalidUser_ShouldReturnFailure() {
        // Arrange
        EMSAuthRequest request = EMSAuthRequest.builder()
                .actionId("CREATE_CASE")
                .build();

        when(userService.findById(testUserUuid)).thenReturn(Optional.empty());

        // Act
        EMSAuthResponse result = emsService.checkUserAuthorization(TEST_USER_ID, request);

        // Assert
        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getActions()).isEmpty();
        assertThat(result.getResourceAccess().getCanRead()).isFalse();
    }

    @Test
    void checkUserAuthorization_WithNoSpecificAction_ShouldCheckCommonActions() {
        // Arrange
        EMSAuthRequest request = EMSAuthRequest.builder()
                .resourceType("case")
                .build();

        User mockUser = createMockUser();
        AuthorizationCheckResponse mockAuthResponse = AuthorizationCheckResponse.builder()
                .allowed(true)
                .message("Access granted")
                .build();

        when(userService.findById(testUserUuid)).thenReturn(Optional.of(mockUser));
        when(hybridAuthorizationService.checkAuthorization(any(AuthorizationCheckRequest.class)))
                .thenReturn(mockAuthResponse);
        when(userBusinessAppRoleService.getActiveUserRolesByUserId(testUserUuid))
                .thenReturn(createMockUserRoles());

        // Act
        EMSAuthResponse result = emsService.checkUserAuthorization(TEST_USER_ID, request);

        // Assert
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getActions()).hasSizeGreaterThan(1); // Should check common actions
    }

    @Test
    void validateUserSession_WithValidSession_ShouldReturnTrue() {
        // Arrange
        Map<String, Object> responseBody = Map.of("success", true);
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(authController.validateSession(TEST_SESSION_ID)).thenReturn(mockResponse);

        // Act
        boolean result = emsService.validateUserSession(TEST_SESSION_ID);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void validateUserSession_WithInvalidSession_ShouldReturnFalse() {
        // Arrange
        Map<String, Object> responseBody = Map.of("success", false);
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
        when(authController.validateSession(TEST_SESSION_ID)).thenReturn(mockResponse);

        // Act
        boolean result = emsService.validateUserSession(TEST_SESSION_ID);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void getUserIdFromSession_WithValidSession_ShouldReturnUserId() {
        // Arrange
        Map<String, Object> userInfo = Map.of("id", TEST_USER_ID);
        Map<String, Object> responseBody = Map.of("success", true, "user", userInfo);
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(authController.validateSession(TEST_SESSION_ID)).thenReturn(mockResponse);

        // Act
        Optional<String> result = emsService.getUserIdFromSession(TEST_SESSION_ID);

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(TEST_USER_ID);
    }

    @Test
    void getUserIdFromSession_WithInvalidSession_ShouldReturnEmpty() {
        // Arrange
        Map<String, Object> responseBody = Map.of("success", false);
        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(responseBody, HttpStatus.UNAUTHORIZED);
        when(authController.validateSession(TEST_SESSION_ID)).thenReturn(mockResponse);

        // Act
        Optional<String> result = emsService.getUserIdFromSession(TEST_SESSION_ID);

        // Assert
        assertThat(result).isEmpty();
    }

    // Helper methods for creating mock objects

    private User createMockUser() {
        User user = new User();
        user.setUserId(testUserUuid);
        user.setUsername("alice.intake");
        user.setEmail("alice.intake@company.com");
        user.setFirstName("Alice");
        user.setLastName("Johnson");
        user.setIsActive(true);
        user.setGlobalAttributes(new HashMap<>());
        return user;
    }

    private List<UserBusinessAppRole> createMockUserRoles() {
        BusinessApplication app = new BusinessApplication();
        app.setId(1L);
        app.setBusinessAppName("onecms");

        BusinessAppRole role = new BusinessAppRole();
        role.setId(1L);
        role.setRoleName("INTAKE_ANALYST");
        role.setRoleDisplayName("Intake Analyst");
        role.setBusinessApplication(app);
        role.setMetadata(Map.of("queue", "intake-analyst-queue"));

        UserBusinessAppRole userRole = new UserBusinessAppRole();
        userRole.setId(1L);
        userRole.setUserId(testUserUuid);
        userRole.setBusinessAppRole(role);
        userRole.setIsActive(true);

        return List.of(userRole);
    }

    private List<UserDepartment> createMockUserDepartments() {
        Department department = new Department();
        department.setId(1L);
        department.setDepartmentName("Investigation Unit");
        department.setDepartmentCode("IU");
        department.setIsActive(true);

        UserDepartment userDepartment = new UserDepartment();
        userDepartment.setId(1L);
        userDepartment.setUserId(testUserUuid);
        userDepartment.setDepartment(department);
        userDepartment.setIsActive(true);

        return List.of(userDepartment);
    }
}