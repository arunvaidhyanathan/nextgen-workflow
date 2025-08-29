package com.citi.onecms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_narratives", schema = "onecms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseNarrative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "narrative_id", nullable = false, unique = true, length = 50)
    private String narrativeId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "investigation_function", length = 50)
    private String investigationFunction;

    @Column(name = "narrative_type", length = 100)
    private String narrativeType;

    @Column(name = "narrative_title", length = 255)
    private String narrativeTitle;

    @Column(name = "narrative_text", nullable = false, columnDefinition = "TEXT")
    private String narrativeText;

    @Column(name = "is_recalled")
    private Boolean isRecalled = false;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Many-to-one relationship with Case
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Case caseEntity;

    // Note: created_by now references entitlement-service users via String ID

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}