package com.citi.onecms.util;

/**
 * Application constants
 */
public final class Constants {

    // Prevent instantiation
    private Constants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    // Application Information
    public static final String APPLICATION_NAME = "Case Management System";
    public static final String APPLICATION_VERSION = "1.0.0";
    public static final String API_VERSION = "v1";

    // Security Constants
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String JWT_SECRET_DEFAULT = "mySecretKey12345678901234567890123456789012345678901234567890";
    public static final long JWT_EXPIRATION_DEFAULT = 86400000L; // 24 hours in milliseconds

    // Role Constants
    public static final String ROLE_PREFIX = "ROLE_";
    
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_DIRECTOR = "DIRECTOR";
    public static final String ROLE_IU_MANAGER = "IU_MANAGER";
    public static final String ROLE_INTAKE_ANALYST = "INTAKE_ANALYST";
    public static final String ROLE_HR_SPECIALIST = "HR_SPECIALIST";
    public static final String ROLE_LEGAL_COUNSEL = "LEGAL_COUNSEL";
    public static final String ROLE_SECURITY_ANALYST = "SECURITY_ANALYST";
    public static final String ROLE_INVESTIGATOR = "INVESTIGATOR";

    // Case Constants
    public static final String CASE_NUMBER_PREFIX = "CMS";
    public static final String CASE_NUMBER_FORMAT = "%s-%d-%06d"; // CMS-YEAR-SEQUENCE

    // Case Statuses
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_CLOSED = "CLOSED";

    // Case Priorities
    public static final String PRIORITY_LOW = "LOW";
    public static final String PRIORITY_MEDIUM = "MEDIUM";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_CRITICAL = "CRITICAL";

    // Work Item Statuses
    public static final String WORK_ITEM_PENDING = "PENDING";
    public static final String WORK_ITEM_IN_PROGRESS = "IN_PROGRESS";
    public static final String WORK_ITEM_COMPLETED = "COMPLETED";
    public static final String WORK_ITEM_ESCALATED = "ESCALATED";
    public static final String WORK_ITEM_CANCELLED = "CANCELLED";

    // Allegation Types
    public static final String ALLEGATION_HARASSMENT = "Harassment";
    public static final String ALLEGATION_SEXUAL_HARASSMENT = "Sexual Harassment";
    public static final String ALLEGATION_WORKPLACE_HARASSMENT = "Workplace Harassment";
    public static final String ALLEGATION_DISCRIMINATION = "Discrimination";
    public static final String ALLEGATION_AGE_DISCRIMINATION = "Age Discrimination";
    public static final String ALLEGATION_GENDER_DISCRIMINATION = "Gender Discrimination";
    public static final String ALLEGATION_RACIAL_DISCRIMINATION = "Racial Discrimination";
    public static final String ALLEGATION_POLICY_VIOLATION = "Policy Violation";
    public static final String ALLEGATION_CODE_OF_CONDUCT = "Code of Conduct";
    public static final String ALLEGATION_FRAUD = "Fraud";
    public static final String ALLEGATION_FINANCIAL_FRAUD = "Financial Fraud";
    public static final String ALLEGATION_EMBEZZLEMENT = "Embezzlement";
    public static final String ALLEGATION_COMPLIANCE_VIOLATION = "Compliance Violation";
    public static final String ALLEGATION_SECURITY_BREACH = "Security Breach";
    public static final String ALLEGATION_DATA_BREACH = "Data Breach";
    public static final String ALLEGATION_CRIMINAL_ACTIVITY = "Criminal Activity";
    public static final String ALLEGATION_THEFT = "Theft";

    // Classification Categories
    public static final String CLASSIFICATION_HR = "HR";
    public static final String CLASSIFICATION_LEGAL = "LEGAL";
    public static final String CLASSIFICATION_CSIS = "CSIS";

    // Assignment Groups
    public static final String GROUP_HR_SPECIALIST = "HR_SPECIALIST";
    public static final String GROUP_HR_GENERALIST = "HR_GENERALIST";
    public static final String GROUP_LEGAL_COUNSEL = "LEGAL_COUNSEL";
    public static final String GROUP_COMPLIANCE_OFFICER = "COMPLIANCE_OFFICER";
    public static final String GROUP_CONTRACT_SPECIALIST = "CONTRACT_SPECIALIST";
    public static final String GROUP_SECURITY_SPECIALIST = "SECURITY_SPECIALIST";
    public static final String GROUP_CYBER_SECURITY_SPECIALIST = "CYBER_SECURITY_SPECIALIST";
    public static final String GROUP_INVESTIGATION_SPECIALIST = "INVESTIGATION_SPECIALIST";

    // Workflow Task Types
    public static final String TASK_TYPE_EO_INTAKE = "EO_INTAKE";
    public static final String TASK_TYPE_CLASSIFICATION = "CLASSIFICATION";
    public static final String TASK_TYPE_HR_ASSIGNMENT = "HR_ASSIGNMENT";
    public static final String TASK_TYPE_LEGAL_ASSIGNMENT = "LEGAL_ASSIGNMENT";
    public static final String TASK_TYPE_CSIS_ASSIGNMENT = "CSIS_ASSIGNMENT";
    public static final String TASK_TYPE_IU_ASSIGNMENT = "IU_ASSIGNMENT";
    public static final String TASK_TYPE_IU_PROCESSING = "IU_PROCESSING";
    public static final String TASK_TYPE_INVESTIGATION_PLAN = "INVESTIGATION_PLAN";
    public static final String TASK_TYPE_INVESTIGATION_REVIEW = "INVESTIGATION_REVIEW";
    public static final String TASK_TYPE_ACTIVE_INVESTIGATION = "ACTIVE_INVESTIGATION";
    public static final String TASK_TYPE_INVESTIGATION_FINALIZATION = "INVESTIGATION_FINALIZATION";
    public static final String TASK_TYPE_CASE_CLOSURE = "CASE_CLOSURE";

    // Workflow Process IDs
    public static final String PROCESS_CASE_MANAGEMENT = "case-management-process";
    public static final String PROCESS_ETHICS_OFFICE = "Process_Ethics_Office";
    public static final String PROCESS_SPECIALIZED_DEPARTMENTS = "Process_Specialized_Departments";
    public static final String PROCESS_IU_MANAGEMENT = "Process_IU_Management";
    public static final String PROCESS_IU_INVESTIGATION = "Process_IU_Investigation";
    public static final String PROCESS_ETHICS_CLOSURE = "Process_Ethics_Closure";

    // DMN Decision IDs
    public static final String DECISION_ALLEGATION_CLASSIFICATION = "allegation-classification";

    // Zeebe Job Types
    public static final String JOB_TYPE_INITIALIZE_CASE = "initialize-case";
    public static final String JOB_TYPE_VALIDATE_CASE_DATA = "validate-case-data";
    public static final String JOB_TYPE_PREPARE_CASE_ROUTING = "prepare-case-routing";
    public static final String JOB_TYPE_CREATE_WORK_ITEM = "create-work-item";
    public static final String JOB_TYPE_TASK_ASSIGNMENT_LISTENER = "task-assignment-listener";
    public static final String JOB_TYPE_TASK_REASSIGNMENT_LISTENER = "task-reassignment-listener";
    public static final String JOB_TYPE_TASK_ESCALATION_LISTENER = "task-escalation-listener";
    public static final String JOB_TYPE_CASE_STATUS_CHANGE_LISTENER = "case-status-change-listener";
    public static final String JOB_TYPE_CASE_MILESTONE_LISTENER = "case-milestone-listener";
    public static final String JOB_TYPE_CASE_DEADLINE_LISTENER = "case-deadline-listener";
    public static final String JOB_TYPE_SEND_CASE_ASSIGNMENT_NOTIFICATION = "send-case-assignment-notification";
    public static final String JOB_TYPE_SEND_CASE_STATUS_NOTIFICATION = "send-case-status-notification";
    public static final String JOB_TYPE_SEND_CASE_COMPLETION_NOTIFICATION = "send-case-completion-notification";
    public static final String JOB_TYPE_SEND_URGENT_NOTIFICATION = "send-urgent-notification";
    public static final String JOB_TYPE_AUDIT_CASE_CREATION = "audit-case-creation";
    public static final String JOB_TYPE_AUDIT_CASE_ASSIGNMENT = "audit-case-assignment";
    public static final String JOB_TYPE_AUDIT_TASK_COMPLETION = "audit-task-completion";
    public static final String JOB_TYPE_AUDIT_CASE_CLOSURE = "audit-case-closure";
    public static final String JOB_TYPE_AUDIT_COMPLIANCE_CHECK = "audit-compliance-check";

    // SLA Constants (in hours)
    public static final int SLA_CRITICAL_HOURS = 4;
    public static final int SLA_HIGH_HOURS = 24;
    public static final int SLA_MEDIUM_HOURS = 72;
    public static final int SLA_LOW_HOURS = 168; // 1 week

    // Notification Types
    public static final String NOTIFICATION_ASSIGNMENT = "ASSIGNMENT";
    public static final String NOTIFICATION_STATUS_UPDATE = "STATUS_UPDATE";
    public static final String NOTIFICATION_COMPLETION = "COMPLETION";
    public static final String NOTIFICATION_URGENT = "URGENT";
    public static final String NOTIFICATION_REMINDER = "REMINDER";
    public static final String NOTIFICATION_ESCALATION = "ESCALATION";

    // Audit Actions
    public static final String AUDIT_CASE_CREATED = "CASE_CREATED";
    public static final String AUDIT_CASE_ASSIGNED = "CASE_ASSIGNED";
    public static final String AUDIT_TASK_COMPLETED = "TASK_COMPLETED";
    public static final String AUDIT_CASE_CLOSED = "CASE_CLOSED";
    public static final String AUDIT_STATUS_CHANGED = "STATUS_CHANGED";
    public static final String AUDIT_COMPLIANCE_CHECK = "COMPLIANCE_CHECK";
    public static final String AUDIT_CASE_METRICS = "CASE_METRICS";

    // Compliance Status
    public static final String COMPLIANCE_COMPLIANT = "COMPLIANT";
    public static final String COMPLIANCE_MINOR_ISSUES = "MINOR_ISSUES";
    public static final String COMPLIANCE_NON_COMPLIANT = "NON_COMPLIANT";
    public static final String COMPLIANCE_ERROR = "ERROR";

    // Escalation Levels
    public static final String ESCALATION_SUPERVISOR = "SUPERVISOR";
    public static final String ESCALATION_MANAGER = "MANAGER";
    public static final String ESCALATION_DIRECTOR = "DIRECTOR";

    // Investigation Findings
    public static final String FINDING_SUBSTANTIATED = "SUBSTANTIATED";
    public static final String FINDING_PARTIALLY_SUBSTANTIATED = "PARTIALLY_SUBSTANTIATED";
    public static final String FINDING_UNSUBSTANTIATED = "UNSUBSTANTIATED";
    public static final String FINDING_INCONCLUSIVE = "INCONCLUSIVE";

    // Final Case Status
    public static final String FINAL_STATUS_CLOSED_SUBSTANTIATED = "CLOSED_SUBSTANTIATED";
    public static final String FINAL_STATUS_CLOSED_UNSUBSTANTIATED = "CLOSED_UNSUBSTANTIATED";
    public static final String FINAL_STATUS_CLOSED_INCONCLUSIVE = "CLOSED_INCONCLUSIVE";
    public static final String FINAL_STATUS_CLOSED_ADMINISTRATIVE = "CLOSED_ADMINISTRATIVE";

    // API Endpoints
    public static final String API_BASE_PATH = "/api";
    public static final String API_AUTH_PATH = "/auth";
    public static final String API_CASES_PATH = "/cases";
    public static final String API_WORKFLOW_PATH = "/workflow";
    public static final String API_USERS_PATH = "/users";
    public static final String API_ADMIN_PATH = "/admin";

    // HTTP Headers
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HEADER_X_REAL_IP = "X-Real-IP";

    // Media Types
    public static final String MEDIA_TYPE_JSON = "application/json";
    public static final String MEDIA_TYPE_XML = "application/xml";
    public static final String MEDIA_TYPE_FORM_DATA = "application/x-www-form-urlencoded";
    public static final String MEDIA_TYPE_MULTIPART = "multipart/form-data";

    // Database Constants
    public static final String SCHEMA_NAME = "cms";
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // File Upload Constants
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] ALLOWED_FILE_EXTENSIONS = {".pdf", ".doc", ".docx", ".txt", ".jpg", ".jpeg", ".png"};

    // Date/Time Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    // Cache Names
    public static final String CACHE_USERS = "users";
    public static final String CACHE_CASES = "cases";
    public static final String CACHE_ROLES = "roles";
    public static final String CACHE_CASE_TYPES = "caseTypes";

    // Thread Pool Constants
    public static final int CORE_POOL_SIZE = 5;
    public static final int MAX_POOL_SIZE = 20;
    public static final int QUEUE_CAPACITY = 100;
    public static final String THREAD_NAME_PREFIX = "CMS-Async-";

    // Validation Constants
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_CASE_TITLE_LENGTH = 255;
    public static final int MAX_DESCRIPTION_LENGTH = 4000;

    // Error Messages
    public static final String ERROR_USER_NOT_FOUND = "User not found";
    public static final String ERROR_CASE_NOT_FOUND = "Case not found";
    public static final String ERROR_INVALID_CREDENTIALS = "Invalid username or password";
    public static final String ERROR_ACCESS_DENIED = "Access denied";
    public static final String ERROR_VALIDATION_FAILED = "Validation failed";
    public static final String ERROR_INTERNAL_SERVER = "Internal server error";
    public static final String ERROR_UNAUTHORIZED = "Unauthorized access";
    public static final String ERROR_RESOURCE_NOT_FOUND = "Resource not found";
    public static final String ERROR_BAD_REQUEST = "Bad request";

    // Success Messages
    public static final String SUCCESS_CASE_CREATED = "Case created successfully";
    public static final String SUCCESS_CASE_UPDATED = "Case updated successfully";
    public static final String SUCCESS_TASK_COMPLETED = "Task completed successfully";
    public static final String SUCCESS_USER_AUTHENTICATED = "User authenticated successfully";
    public static final String SUCCESS_LOGOUT = "User logged out successfully";

    // Environment Profiles
    public static final String PROFILE_DEV = "dev";
    public static final String PROFILE_TEST = "test";
    public static final String PROFILE_PROD = "prod";
    public static final String PROFILE_STAGING = "staging";

    // Configuration Properties
    public static final String PROPERTY_CAMUNDA_ZEEBE_ADDRESS = "camunda.client.zeebe.gateway-address";
    public static final String PROPERTY_CAMUNDA_ZEEBE_PLAINTEXT = "camunda.client.zeebe.security.plaintext";
    public static final String PROPERTY_JWT_SECRET = "jwt.secret";
    public static final String PROPERTY_JWT_EXPIRATION = "jwt.expiration";
    public static final String PROPERTY_DB_URL = "spring.datasource.url";
    public static final String PROPERTY_DB_USERNAME = "spring.datasource.username";
    public static final String PROPERTY_DB_PASSWORD = "spring.datasource.password";
}