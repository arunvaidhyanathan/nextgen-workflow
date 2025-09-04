package com.citi.onecms.entity;

import com.citi.onecms.entity.Priority;
import com.citi.onecms.enums.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_drafts", schema = "onecms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseDraft {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_draft_id")
    private Long caseDraftId;
    
    @NotNull
    @Column(name = "case_id", nullable = false)
    private Long caseId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Case caseEntity;
    
    @NotBlank
    @Size(max = 500)
    @Column(name = "case_draft_title", nullable = false)
    private String caseDraftTitle;
    
    @Column(name = "case_draft_description", columnDefinition = "TEXT")
    private String caseDraftDescription;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;
    
    @Column(name = "case_type_id")
    private Long caseTypeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_type_id", referencedColumnName = "id", insertable = false, updatable = false)
    private CaseType caseType;
    
    @Column(name = "department_id")
    private Long departmentId;
    
    // Escalation Channel Information
    @Column(name = "date_received_by_escalation_channel")
    private LocalDate dateReceivedByEscalationChannel;
    
    @NotBlank
    @Size(max = 100)
    @Column(name = "complaint_received_method", nullable = false)
    private String complaintReceivedMethod;
    
    // Workflow Integration
    @NotBlank
    @Size(max = 64)
    @Column(name = "process_instance_id", nullable = false)
    private String processInstanceId;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "process_definition_key", nullable = false)
    private String processDefinitionKey;
    
    @Size(max = 255)
    @Column(name = "business_key")
    private String businessKey;
    
    // Initial Task Information
    @Size(max = 64)
    @Column(name = "initial_task_id")
    private String initialTaskId;
    
    @Size(max = 255)
    @Column(name = "initial_task_name")
    private String initialTaskName;
    
    @Size(max = 100)
    @Column(name = "initial_task_queue")
    private String initialTaskQueue;
    
    @Size(max = 255)
    @Column(name = "candidate_group")
    private String candidateGroup;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false)
    @Builder.Default
    private TaskStatus taskStatus = TaskStatus.OPEN;
    
    // Audit Information
    @NotBlank
    @Size(max = 50)
    @Column(name = "created_by_user_id", nullable = false)
    private String createdByUserId;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Status and Flags
    @Enumerated(EnumType.STRING)
    @Column(name = "draft_status", nullable = false)
    @Builder.Default
    private DraftStatus draftStatus = DraftStatus.DRAFT;
    
    @Column(name = "is_submitted", nullable = false)
    @Builder.Default
    private Boolean isSubmitted = false;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Enum for draft status
    public enum DraftStatus {
        DRAFT, SUBMITTED, CANCELLED
    }
}