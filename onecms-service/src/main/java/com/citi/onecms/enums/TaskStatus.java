package com.citi.onecms.enums;

/**
 * Enumeration for task status values in the case management system.
 * Aligned with Flowable workflow system statuses.
 */
public enum TaskStatus {
    OPEN,       // Task is available for assignment (Flowable compatible)
    CLAIMED,    // Task has been claimed by a user (Flowable compatible)
    PENDING,    // Task is waiting for dependencies
    IN_PROGRESS,
    COMPLETED,  // Task is finished (Flowable compatible)
    CANCELLED,
    ESCALATED,
    SUSPENDED
}