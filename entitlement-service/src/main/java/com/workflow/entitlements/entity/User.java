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
import java.util.Map;
import java.util.UUID;

/**
 * Primary user identity entity for hybrid authorization system.
 * Maps to entitlements.entitlement_core_users table.
 * This is the single source of truth for user identity across both RBAC and ABAC engines.
 */
@Entity
@Table(name = "entitlement_core_users", schema = "entitlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "username", length = 255, nullable = false, unique = true)
    private String username;
    
    @Column(name = "email", length = 255, nullable = false, unique = true)
    private String email;
    
    @Column(name = "first_name", length = 255, nullable = false)
    private String firstName;
    
    @Column(name = "last_name", length = 255, nullable = false)
    private String lastName;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    /**
     * Global attributes stored as JSONB for flexible user metadata.
     * Used by both RBAC and ABAC engines for attribute-based decisions.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "global_attributes", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> globalAttributes = new HashMap<>();
    
    /**
     * Legacy compatibility method - returns userId as string.
     * @deprecated Use getUserId() instead for UUID type safety
     */
    @Deprecated
    public String getId() {
        return userId != null ? userId.toString() : null;
    }
    
    /**
     * Legacy compatibility method - sets userId from string.
     * @deprecated Use setUserId(UUID) instead for type safety
     */
    @Deprecated
    public void setId(String id) {
        this.userId = id != null ? UUID.fromString(id) : null;
    }
    
    /**
     * Legacy compatibility method for attributes field.
     * @deprecated Use getGlobalAttributes() instead
     */
    @Deprecated
    public Map<String, Object> getAttributes() {
        return globalAttributes;
    }
    
    /**
     * Legacy compatibility method for attributes field.
     * @deprecated Use setGlobalAttributes() instead
     */
    @Deprecated
    public void setAttributes(Map<String, Object> attributes) {
        this.globalAttributes = attributes;
    }
}