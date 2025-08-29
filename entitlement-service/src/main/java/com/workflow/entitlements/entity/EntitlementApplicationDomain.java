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
 * Application domain entity for database engine multi-tenancy.
 * Maps to entitlements.entitlement_application_domains table.
 */
@Entity
@Table(name = "entitlement_application_domains", schema = "entitlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntitlementApplicationDomain {
    
    @Id
    @Column(name = "domain_id")
    private UUID domainId;
    
    @Column(name = "domain_name", length = 255, nullable = false, unique = true)
    private String domainName;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "is_tiered")
    @Builder.Default
    private Boolean isTiered = false;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "domain_metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> domainMetadata = new HashMap<>();
    
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
    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EntitlementDomainRole> roles;
}