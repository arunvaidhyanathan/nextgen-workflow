package com.workflow.entitlements.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Role-Permission mapping entity for database engine RBAC.
 * Maps to entitlements.entitlement_role_permissions table.
 */
@Entity
@Table(name = "entitlement_role_permissions", schema = "entitlements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementRolePermission {
    
    @Id
    @Column(name = "role_permission_id")
    private UUID rolePermissionId;
    
    @Column(name = "role_id", nullable = false)
    private UUID roleId;
    
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", insertable = false, updatable = false)
    private EntitlementDomainRole role;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", insertable = false, updatable = false)
    private EntitlementPermission permission;
}