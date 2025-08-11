package com.workflow.entitlements.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "business_app_roles", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"business_app_id", "role_name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessAppRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_app_id", nullable = false)
    private BusinessApplication businessApplication;
    
    @Column(name = "role_name", length = 100, nullable = false)
    private String roleName;
    
    @Column(name = "role_display_name", length = 255, nullable = false)
    private String roleDisplayName;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    @Type(io.hypersistence.utils.hibernate.type.json.JsonType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @OneToMany(mappedBy = "businessAppRole", fetch = FetchType.LAZY)
    private List<UserBusinessAppRole> userAssignments;
}