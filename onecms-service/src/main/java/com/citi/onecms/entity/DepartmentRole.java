package com.citi.onecms.entity;

import java.util.Set;

public enum DepartmentRole {
    HR_SPECIALIST("HR_SPECIALIST", Set.of("hr-intake-queue", "hr-processing-queue")),
    HR_MANAGER("HR_MANAGER", Set.of("hr-intake-queue", "hr-processing-queue", "hr-approval-queue")),
    
    CSIS_ANALYST("CSIS_ANALYST", Set.of("csis-intake-queue", "csis-investigation-queue")),
    SECURITY_ANALYST("SECURITY_ANALYST", Set.of("csis-intake-queue", "csis-investigation-queue")),
    CSIS_DIRECTOR("CSIS_DIRECTOR", Set.of("csis-intake-queue", "csis-investigation-queue", "csis-approval-queue")),
    
    LEGAL_COUNSEL("LEGAL_COUNSEL", Set.of("legal-intake-queue", "legal-review-queue")),
    LEGAL_DIRECTOR("LEGAL_DIRECTOR", Set.of("legal-intake-queue", "legal-review-queue", "legal-approval-queue")),
    
    INVESTIGATOR("INVESTIGATOR", Set.of("investigation-queue", "field-investigation-queue")),
    INVESTIGATION_MANAGER("INVESTIGATION_MANAGER", Set.of("investigation-queue", "field-investigation-queue", "investigation-approval-queue")),
    
    ETHICS_DIRECTOR("ETHICS_DIRECTOR", Set.of("director-oversight-queue", "hr-approval-queue", "csis-approval-queue", "legal-approval-queue", "investigation-approval-queue"));
    
    private final String roleName;
    private final Set<String> accessibleQueues;
    
    DepartmentRole(String roleName, Set<String> accessibleQueues) {
        this.roleName = roleName;
        this.accessibleQueues = accessibleQueues;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public Set<String> getAccessibleQueues() {
        return accessibleQueues;
    }
    
    public static DepartmentRole fromRoleName(String roleName) {
        for (DepartmentRole role : values()) {
            if (role.roleName.equals(roleName)) {
                return role;
            }
        }
        return HR_SPECIALIST; // Default fallback
    }
    
    public boolean canAccessQueue(String queueName) {
        return accessibleQueues.contains(queueName);
    }
}