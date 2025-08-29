package com.workflow.entitlements.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Domain role entity for database engine RBAC.
 * Maps to entitlements.entitlement_domain_roles table.
 */
@Entity
@Table(name = "entitlement_domain_roles", schema = "entitlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementDomainRole {
    
    @Id
    @Column(name = "role_id")
    private UUID roleId;
    
    @Column(name = "domain_id", nullable = false)
    private UUID domainId;
    
    @Column(name = "role_name", length = 255, nullable = false)
    private String roleName;
    
    @Column(name = "display_name", length = 255)
    private String displayName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "role_level", length = 50)
    private String roleLevel;
    
    @Column(name = "maker_checker_type", length = 50)
    private String makerCheckerType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "role_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> roleMetadata = new HashMap<>();
    
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
    @JoinColumn(name = "domain_id", insertable = false, updatable = false)
    private EntitlementApplicationDomain domain;
    
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EntitlementRolePermission> rolePermissions;
    
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EntitlementUserDomainRole> userRoles;
}