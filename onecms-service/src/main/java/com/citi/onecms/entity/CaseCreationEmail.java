package com.citi.onecms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_creation_email", schema = "onecms")
public class CaseCreationEmail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "case_creation_email_seq")
    @SequenceGenerator(name = "case_creation_email_seq", sequenceName = "create_case_email_seq", allocationSize = 1, schema = "onecms")
    @Column(name = "call_id")
    private Long callId;
    
    @NotBlank
    @Size(max = 50)
    @Column(name = "status", nullable = false)
    private String status;
    
    @NotBlank
    @Size(max = 255)
    @Column(name = "sender_email", nullable = false)
    private String senderEmail;
    
    @Size(max = 255)
    @Column(name = "sender_name")
    private String senderName;
    
    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;
    
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;
    
    @NotNull
    @Lob
    @Column(name = "raw_email_attachment", nullable = false)
    private byte[] rawEmailAttachment;
    
    @Size(max = 50)
    @Column(name = "employee_id")
    private String employeeId;
    
    @Column(name = "case_id")
    private Long caseId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Optional relationship to the Case entity
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Case caseEntity;
    
    // Constructors
    public CaseCreationEmail() {}
    
    public CaseCreationEmail(String status, String senderEmail, String senderName, 
                           String subject, String bodyText, byte[] rawEmailAttachment) {
        this.status = status;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.subject = subject;
        this.bodyText = bodyText;
        this.rawEmailAttachment = rawEmailAttachment;
        this.createdAt = LocalDateTime.now();
    }
    
    // Lifecycle Callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getCallId() {
        return callId;
    }
    
    public void setCallId(Long callId) {
        this.callId = callId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getSenderEmail() {
        return senderEmail;
    }
    
    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }
    
    public String getSenderName() {
        return senderName;
    }
    
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getBodyText() {
        return bodyText;
    }
    
    public void setBodyText(String bodyText) {
        this.bodyText = bodyText;
    }
    
    public byte[] getRawEmailAttachment() {
        return rawEmailAttachment;
    }
    
    public void setRawEmailAttachment(byte[] rawEmailAttachment) {
        this.rawEmailAttachment = rawEmailAttachment;
    }
    
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public Long getCaseId() {
        return caseId;
    }
    
    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Case getCaseEntity() {
        return caseEntity;
    }
    
    public void setCaseEntity(Case caseEntity) {
        this.caseEntity = caseEntity;
    }
    
    // Status constants for processing states
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
}