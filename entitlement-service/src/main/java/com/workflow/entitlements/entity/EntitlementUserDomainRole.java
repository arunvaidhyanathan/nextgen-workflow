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
 * User-Domain Role assignment entity for database engine RBAC.
 * Maps to entitlements.entitlement_user_domain_roles table.
 */
@Entity
@Table(name = "entitlement_user_domain_roles", schema = "entitlements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementUserDomainRole {
    
    @Id
    @Column(name = "user_role_id")
    private UUID userRoleId;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
    
    @Column(name = "assigned_at")
    @Builder.Default
    private Instant assignedAt = Instant.now();
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "assigned_by")
    private UUID assignedBy;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assignment_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> assignmentMetadata = new HashMap<>();
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private EntitlementDomainRole role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", insertable = false, updatable = false)
    private User assignedByUser;
    
    /**
     * Check if the role assignment is currently valid (active and not expired)
     */
    public boolean isCurrentlyValid() {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(Instant.now());
    }
}