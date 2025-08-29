package com.workflow.entitlements.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Resource-level permission entity for database engine ABAC-style access control.
 * Maps to entitlements.resource_permissions table.
 * Provides direct user-to-resource permission grants.
 */
@Entity
@Table(name = "resource_permissions", schema = "entitlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourcePermission {
    
    @Id
    @Column(name = "resource_permission_id")
    private UUID resourcePermissionId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "resource_type", length = 255, nullable = false)
    private String resourceType;
    
    @Column(name = "resource_id", length = 255, nullable = false)
    private String resourceId;
    
    /**
     * Array of allowed actions for this resource.
     * PostgreSQL array type mapping.
     */
    @Column(name = "allowed_actions", columnDefinition = "text[]", nullable = false)
    private String[] allowedActions;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> conditions = new HashMap<>();
    
    @Column(name = "granted_at")
    @Builder.Default
    private Instant grantedAt = Instant.now();
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "granted_by")
    private UUID grantedBy;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by", insertable = false, updatable = false)
    private User grantedByUser;
    
    /**
     * Check if the permission is currently valid (active and not expired)
     */
    public boolean isCurrentlyValid() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
    
    /**
     * Check if the permission allows a specific action
     */
    public boolean allowsAction(String action) {
        if (allowedActions == null) {
            return false;
        }
        for (String allowedAction : allowedActions) {
            if (allowedAction.equals(action)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Create permission key for caching/lookup
     */
    public String getPermissionKey() {
        return userId + ":" + resourceType + ":" + resourceId;
    }
}