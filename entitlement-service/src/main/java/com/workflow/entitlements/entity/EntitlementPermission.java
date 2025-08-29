package com.workflow.entitlements.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Permission entity for database engine RBAC.
 * Maps to entitlements.entitlement_permissions table.
 */
@Entity
@Table(name = "entitlement_permissions", schema = "entitlements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"resource_type", "action"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementPermission {
    
    @Id
    @Column(name = "permission_id")
    private UUID permissionId;
    
    @Column(name = "resource_type", length = 255, nullable = false)
    private String resourceType;
    
    @Column(name = "action", length = 255, nullable = false)
    private String action;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EntitlementRolePermission> rolePermissions;
    
    /**
     * Convenience method to create permission key for caching/lookup
     */
    public String getPermissionKey() {
        return resourceType + ":" + action;
    }
}