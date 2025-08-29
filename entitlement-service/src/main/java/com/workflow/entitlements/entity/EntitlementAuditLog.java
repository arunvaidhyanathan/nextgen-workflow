package com.workflow.entitlements.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Audit log entity for tracking authorization decisions and system events.
 * Maps to entitlements.entitlement_audit_logs table.
 * Used by both RBAC and ABAC engines for comprehensive audit trail.
 */
@Entity
@Table(name = "entitlement_audit_logs", schema = "entitlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementAuditLog {
    
    @Id
    @Column(name = "audit_id")
    private UUID auditId;
    
    @Column(name = "event_timestamp")
    @Builder.Default
    private Instant eventTimestamp = Instant.now();
    
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "resource_type", length = 255)
    private String resourceType;
    
    @Column(name = "resource_id", length = 255)
    private String resourceId;
    
    @Column(name = "action", length = 255)
    private String action;
    
    /**
     * Authorization decision: ALLOW or DENY
     */
    @Column(name = "decision", length = 20)
    private String decision;
    
    @Column(name = "decision_reason", columnDefinition = "TEXT")
    private String decisionReason;
    
    /**
     * Which authorization engine was used: DATABASE or CERBOS
     */
    @Column(name = "engine_type", length = 50)
    private String engineType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> requestMetadata = new HashMap<>();
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> responseMetadata = new HashMap<>();
    
    @Column(name = "session_id", length = 255)
    private String sessionId;
    
    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;
    
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    /**
     * Common event types for consistency
     */
    public static class EventType {
        public static final String AUTHORIZATION_CHECK = "AUTHORIZATION_CHECK";
        public static final String USER_LOGIN = "USER_LOGIN";
        public static final String USER_LOGOUT = "USER_LOGOUT";
        public static final String ROLE_ASSIGNMENT = "ROLE_ASSIGNMENT";
        public static final String PERMISSION_GRANT = "PERMISSION_GRANT";
        public static final String POLICY_UPDATE = "POLICY_UPDATE";
        public static final String SESSION_VALIDATION = "SESSION_VALIDATION";
    }
    
    /**
     * Common decision types
     */
    public static class Decision {
        public static final String ALLOW = "ALLOW";
        public static final String DENY = "DENY";
    }
    
    /**
     * Engine types
     */
    public static class EngineType {
        public static final String DATABASE = "DATABASE";
        public static final String CERBOS = "CERBOS";
    }
    
    /**
     * Builder helper for authorization check audit log
     */
    public static EntitlementAuditLog authorizationCheck(
            UUID userId, String resourceType, String resourceId, 
            String action, String decision, String reason, String engineType) {
        return EntitlementAuditLog.builder()
                .auditId(UUID.randomUUID())
                .eventType(EventType.AUTHORIZATION_CHECK)
                .userId(userId)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .action(action)
                .decision(decision)
                .decisionReason(reason)
                .engineType(engineType)
                .build();
    }
}