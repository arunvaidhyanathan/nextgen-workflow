package com.citi.onecms.entity;

public enum Severity {
    LOW("Low"),
    MEDIUM("Medium"), 
    HIGH("High"),
    CRITICAL("Critical");
    
    private final String displayName;
    
    Severity(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}