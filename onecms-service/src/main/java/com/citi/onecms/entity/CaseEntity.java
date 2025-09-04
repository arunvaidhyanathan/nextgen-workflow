package com.citi.onecms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_entities", schema = "onecms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_id", nullable = false, unique = true, length = 50)
    private String entityId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 20)
    private EntityType entityType;

    @Column(name = "entity_name", nullable = false, length = 255)
    private String entityName;

    @Column(name = "investigation_function", length = 50)
    private String investigationFunction;

    @Column(name = "relationship_type", length = 100)
    private String relationshipType;

    // Person fields
    @Column(name = "soeid", length = 50)
    private String soeid;

    @Column(name = "geid", length = 50)
    private String geid;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    // Organization fields
    @Column(name = "organization_name", length = 255)
    private String organizationName;

    @Column(name = "organization_type", length = 100)
    private String organizationType;
    
    // New fields from UI design
    @Column(name = "preferred_contact_method", columnDefinition = "TEXT")
    private String preferredContactMethod;
    
    @Column(name = "goc", length = 100)
    private String goc;
    
    @Column(name = "manager", length = 255)
    private String manager;
    
    @Column(name = "hire_date")
    private LocalDate hireDate;
    
    @Column(name = "hr_responsible", length = 255)
    private String hrResponsible;
    
    @Column(name = "legal_vehicle", length = 100)
    private String legalVehicle;
    
    @Column(name = "managed_segment", length = 100)
    private String managedSegment;
    
    @Column(name = "relationship_to_citi", length = 100)
    private String relationshipToCiti;
    
    @Column(name = "anonymous")
    private Boolean anonymous = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many-to-one relationship with Case
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Case caseEntity;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EntityType {
        PERSON, ORGANIZATION
    }

    // Utility methods
    public String getDisplayName() {
        if (entityType == EntityType.PERSON) {
            StringBuilder name = new StringBuilder();
            if (firstName != null) name.append(firstName);
            if (middleName != null) name.append(" ").append(middleName);
            if (lastName != null) name.append(" ").append(lastName);
            if (soeid != null) name.append(" (").append(soeid).append(")");
            return name.toString().trim();
        } else {
            return organizationName;
        }
    }
}