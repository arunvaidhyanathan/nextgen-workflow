package com.workflow.entitlements.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Table(name = "business_applications", schema = "entitlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessApplication {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "business_app_name", length = 100, nullable = false, unique = true)
    private String businessAppName;
    
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
    
    @OneToMany(mappedBy = "businessApplication", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<BusinessAppRole> roles;
}