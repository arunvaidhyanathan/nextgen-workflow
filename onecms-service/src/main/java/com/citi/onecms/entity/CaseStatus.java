package com.citi.onecms.entity;

/**
 * Enumeration representing the different statuses a case can have
 */
public enum CaseStatus {
    
    /**
     * Case is in draft state and not yet submitted
     */
    DRAFT("Draft", "Case is in draft state and not yet submitted for processing", "secondary"),
    
    /**
     * Case has been created but not yet assigned or worked on
     */
    OPEN("Open", "Case has been created and is awaiting assignment", "primary"),
    
    /**
     * Case has been submitted for processing and is awaiting workflow assignment
     */
    SUBMITTED("Submitted", "Case has been submitted and is ready for processing", "info"),
    
    /**
     * Case is currently being worked on
     */
    IN_PROGRESS("In Progress", "Case is currently being investigated or processed", "warning"),
    
    /**
     * Case investigation has been completed but not yet closed
     */
    RESOLVED("Resolved", "Case investigation is complete and awaiting final review", "info"),
    
    /**
     * Case has been completed and closed
     */
    CLOSED("Closed", "Case has been completed and officially closed", "success"),
    
    /**
     * Case is on hold or suspended temporarily
     */
    ON_HOLD("On Hold", "Case processing has been temporarily suspended", "secondary"),
    
    /**
     * Case has been cancelled or withdrawn
     */
    CANCELLED("Cancelled", "Case has been cancelled or withdrawn", "danger"),
    
    /**
     * Case is under review by management or ethics office
     */
    UNDER_REVIEW("Under Review", "Case is being reviewed by supervisory authority", "info"),
    
    /**
     * Case has been escalated to higher authority
     */
    ESCALATED("Escalated", "Case has been escalated to higher management level", "warning"),
    
    /**
     * Case requires additional information before proceeding
     */
    PENDING_INFO("Pending Information", "Case is awaiting additional information or documentation", "secondary"),
    
    /**
     * Case is awaiting approval to proceed
     */
    PENDING_APPROVAL("Pending Approval", "Case is awaiting management approval to proceed", "warning"),
    
    /**
     * Case has been rejected or dismissed
     */
    REJECTED("Rejected", "Case has been rejected or dismissed", "danger"),
    
    /**
     * Case is being investigated by external party
     */
    EXTERNAL_INVESTIGATION("External Investigation", "Case has been referred to external investigators", "info"),
    
    /**
     * Case investigation is nearing completion
     */
    NEAR_COMPLETION("Near Completion", "Case investigation is in final stages", "success"),
    
    /**
     * Case requires management attention
     */
    REQUIRES_ATTENTION("Requires Attention", "Case requires immediate management attention", "danger");

    private final String displayName;
    private final String description;
    private final String cssClass;

    /**
     * Constructor for CaseStatus enum
     * 
     * @param displayName User-friendly display name
     * @param description Detailed description of the status
     * @param cssClass CSS class for UI styling (Bootstrap-style)
     */
    CaseStatus(String displayName, String description, String cssClass) {
        this.displayName = displayName;
        this.description = description;
        this.cssClass = cssClass;
    }

    /**
     * Get the user-friendly display name
     * 
     * @return Display name for UI
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get the detailed description
     * 
     * @return Detailed description of the status
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the CSS class for styling
     * 
     * @return CSS class name for UI styling
     */
    public String getCssClass() {
        return cssClass;
    }

    /**
     * Check if the status represents an active case
     * 
     * @return true if the case is active (not closed, cancelled, or rejected)
     */
    public boolean isActive() {
        return this != CLOSED && this != CANCELLED && this != REJECTED;
    }

    /**
     * Check if the status represents a completed case
     * 
     * @return true if the case is completed (closed, cancelled, or rejected)
     */
    public boolean isCompleted() {
        return this == CLOSED || this == CANCELLED || this == REJECTED;
    }

    /**
     * Check if the status indicates the case is in progress
     * 
     * @return true if the case is actively being worked on
     */
    public boolean isInProgress() {
        return this == IN_PROGRESS || this == SUBMITTED || this == UNDER_REVIEW || this == EXTERNAL_INVESTIGATION || this == NEAR_COMPLETION;
    }

    /**
     * Check if the status indicates the case is pending something
     * 
     * @return true if the case is waiting for something
     */
    public boolean isPending() {
        return this == PENDING_INFO || this == PENDING_APPROVAL || this == ON_HOLD;
    }

    /**
     * Check if the status requires urgent attention
     * 
     * @return true if the status indicates urgency
     */
    public boolean requiresAttention() {
        return this == ESCALATED || this == REQUIRES_ATTENTION;
    }

    /**
     * Get the priority level associated with this status
     * 
     * @return Priority level (1-5, where 5 is highest priority)
     */
    public int getPriorityLevel() {
        switch (this) {
            case REQUIRES_ATTENTION:
            case ESCALATED:
                return 5; // Highest priority
            case PENDING_APPROVAL:
            case UNDER_REVIEW:
                return 4; // High priority
            case IN_PROGRESS:
            case EXTERNAL_INVESTIGATION:
            case NEAR_COMPLETION:
                return 3; // Medium priority
            case OPEN:
            case SUBMITTED:
            case PENDING_INFO:
                return 2; // Low-medium priority
            case ON_HOLD:
            case RESOLVED:
                return 1; // Lowest priority
            case DRAFT:
            case CLOSED:
            case CANCELLED:
            case REJECTED:
            default:
                return 0; // No priority (completed/draft)
        }
    }

    /**
     * Get valid transition statuses from current status
     * 
     * @return Array of valid next statuses
     */
    public CaseStatus[] getValidTransitions() {
        switch (this) {
            case DRAFT:
                return new CaseStatus[]{OPEN, SUBMITTED, CANCELLED};
                
            case OPEN:
                return new CaseStatus[]{SUBMITTED, IN_PROGRESS, PENDING_INFO, PENDING_APPROVAL, ON_HOLD, CANCELLED, REJECTED};
            
            case SUBMITTED:
                return new CaseStatus[]{IN_PROGRESS, UNDER_REVIEW, PENDING_INFO, ON_HOLD, CANCELLED};
            
            case IN_PROGRESS:
                return new CaseStatus[]{RESOLVED, UNDER_REVIEW, ESCALATED, PENDING_INFO, ON_HOLD, EXTERNAL_INVESTIGATION, NEAR_COMPLETION};
            
            case RESOLVED:
                return new CaseStatus[]{CLOSED, UNDER_REVIEW, IN_PROGRESS};
            
            case UNDER_REVIEW:
                return new CaseStatus[]{IN_PROGRESS, RESOLVED, CLOSED, ESCALATED, PENDING_APPROVAL, REJECTED};
            
            case ESCALATED:
                return new CaseStatus[]{IN_PROGRESS, UNDER_REVIEW, REQUIRES_ATTENTION, PENDING_APPROVAL};
            
            case PENDING_INFO:
                return new CaseStatus[]{IN_PROGRESS, ON_HOLD, CANCELLED};
            
            case PENDING_APPROVAL:
                return new CaseStatus[]{IN_PROGRESS, REJECTED, ON_HOLD};
            
            case ON_HOLD:
                return new CaseStatus[]{IN_PROGRESS, CANCELLED, REJECTED};
            
            case EXTERNAL_INVESTIGATION:
                return new CaseStatus[]{IN_PROGRESS, RESOLVED, PENDING_INFO};
            
            case NEAR_COMPLETION:
                return new CaseStatus[]{RESOLVED, UNDER_REVIEW, IN_PROGRESS};
            
            case REQUIRES_ATTENTION:
                return new CaseStatus[]{IN_PROGRESS, ESCALATED, UNDER_REVIEW};
            
            case CLOSED:
            case CANCELLED:
            case REJECTED:
            default:
                return new CaseStatus[]{}; // No transitions from final states
        }
    }

    /**
     * Check if transition to another status is valid
     * 
     * @param targetStatus The status to transition to
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(CaseStatus targetStatus) {
        if (targetStatus == null) return false;
        
        CaseStatus[] validTransitions = getValidTransitions();
        for (CaseStatus validStatus : validTransitions) {
            if (validStatus == targetStatus) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get status by display name (case-insensitive)
     * 
     * @param displayName The display name to search for
     * @return CaseStatus matching the display name, or null if not found
     */
    public static CaseStatus getByDisplayName(String displayName) {
        if (displayName == null) return null;
        
        for (CaseStatus status : values()) {
            if (status.displayName.equalsIgnoreCase(displayName)) {
                return status;
            }
        }
        return null;
    }

    /**
     * Get status by name (case-insensitive, handles both enum name and display name)
     * 
     * @param name The name to search for
     * @return CaseStatus matching the name, or null if not found
     */
    public static CaseStatus getByName(String name) {
        if (name == null) return null;
        
        // Try enum name first
        try {
            return CaseStatus.valueOf(name.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Try display name
            return getByDisplayName(name);
        }
    }

    /**
     * Get all active statuses
     * 
     * @return Array of all active case statuses
     */
    public static CaseStatus[] getActiveStatuses() {
        return new CaseStatus[]{
            DRAFT, OPEN, SUBMITTED, IN_PROGRESS, UNDER_REVIEW, ESCALATED, PENDING_INFO, 
            PENDING_APPROVAL, ON_HOLD, EXTERNAL_INVESTIGATION, 
            NEAR_COMPLETION, REQUIRES_ATTENTION, RESOLVED
        };
    }

    /**
     * Get all completed statuses
     * 
     * @return Array of all completed case statuses
     */
    public static CaseStatus[] getCompletedStatuses() {
        return new CaseStatus[]{CLOSED, CANCELLED, REJECTED};
    }

    /**
     * Get all statuses that require action
     * 
     * @return Array of statuses requiring action
     */
    public static CaseStatus[] getActionRequiredStatuses() {
        return new CaseStatus[]{
            OPEN, PENDING_INFO, PENDING_APPROVAL, ESCALATED, REQUIRES_ATTENTION
        };
    }

    /**
     * Get status color for UI display
     * 
     * @return Hex color code for the status
     */
    public String getColor() {
        switch (this) {
            case DRAFT:
                return "#6c757d"; // Gray
            case OPEN:
                return "#007bff"; // Blue
            case SUBMITTED:
                return "#17a2b8"; // Teal
            case IN_PROGRESS:
            case NEAR_COMPLETION:
                return "#ffc107"; // Yellow/Amber
            case RESOLVED:
                return "#17a2b8"; // Teal
            case CLOSED:
                return "#28a745"; // Green
            case ON_HOLD:
            case PENDING_INFO:
            case PENDING_APPROVAL:
                return "#6c757d"; // Gray
            case CANCELLED:
            case REJECTED:
            case REQUIRES_ATTENTION:
                return "#dc3545"; // Red
            case UNDER_REVIEW:
            case EXTERNAL_INVESTIGATION:
                return "#17a2b8"; // Teal
            case ESCALATED:
                return "#fd7e14"; // Orange
            default:
                return "#6c757d"; // Default gray
        }
    }

    /**
     * Get status icon for UI display (Font Awesome class names)
     * 
     * @return Font Awesome icon class name
     */
    public String getIcon() {
        switch (this) {
            case DRAFT:
                return "fa-edit";
            case OPEN:
                return "fa-folder-open";
            case SUBMITTED:
                return "fa-paper-plane";
            case IN_PROGRESS:
                return "fa-spinner";
            case RESOLVED:
                return "fa-check-circle";
            case CLOSED:
                return "fa-times-circle";
            case ON_HOLD:
                return "fa-pause";
            case CANCELLED:
                return "fa-ban";
            case UNDER_REVIEW:
                return "fa-search";
            case ESCALATED:
                return "fa-arrow-up";
            case PENDING_INFO:
                return "fa-clock";
            case PENDING_APPROVAL:
                return "fa-hourglass-half";
            case REJECTED:
                return "fa-times";
            case EXTERNAL_INVESTIGATION:
                return "fa-external-link-alt";
            case NEAR_COMPLETION:
                return "fa-flag-checkered";
            case REQUIRES_ATTENTION:
                return "fa-exclamation-triangle";
            default:
                return "fa-question";
        }
    }

    /**
     * Convert to a map representation for API responses
     * 
     * @return Map containing all status information
     */
    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("name", this.name());
        map.put("displayName", this.displayName);
        map.put("description", this.description);
        map.put("cssClass", this.cssClass);
        map.put("color", this.getColor());
        map.put("icon", this.getIcon());
        map.put("isActive", this.isActive());
        map.put("isCompleted", this.isCompleted());
        map.put("isInProgress", this.isInProgress());
        map.put("isPending", this.isPending());
        map.put("requiresAttention", this.requiresAttention());
        map.put("priorityLevel", this.getPriorityLevel());
        return map;
    }

    /**
     * String representation for display
     * 
     * @return Display name of the status
     */
    @Override
    public String toString() {
        return displayName;
    }
}