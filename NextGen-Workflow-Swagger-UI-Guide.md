# NextGen Workflow Swagger UI Testing Guide

## Overview

This guide provides comprehensive step-by-step instructions for testing the NextGen Workflow microservices using Swagger UI. The system consists of two main services:

1. **Flowable Wrapper Service** (Port 8082) - Workflow orchestration and task management
2. **OneCMS Service** (Port 8083) - Case management with workflow integration

## Prerequisites

1. **Start all services** in this order:
   ```bash
   # 1. Start PostgreSQL database
   docker-compose up postgres

   # 2. Start Service Registry
   cd service-registry && mvn spring-boot:run

   # 3. Start backend services
   cd entitlement-service && mvn spring-boot:run
   cd flowable-wrapper-v2 && mvn spring-boot:run  
   cd onecms-service && mvn spring-boot:run

   # 4. Start API Gateway
   cd api-gateway && mvn spring-boot:run
   ```

2. **Access Swagger UI**:
   - **Flowable Wrapper**: http://localhost:8082/swagger-ui.html
   - **OneCMS Service**: http://localhost:8083/swagger-ui.html

3. **Authentication Headers** (Required for all requests):
   - `X-User-Id`: `alice.intake` (or any valid user ID)

## Service URLs

| Service | Direct URL | Via API Gateway |
|---------|------------|-----------------|
| Flowable Wrapper | http://localhost:8082 | http://localhost:8080/api/workflow |
| OneCMS Service | http://localhost:8083 | http://localhost:8080/api/cms |
| Eureka Dashboard | http://localhost:8761 | - |

## Two-Phase Case Creation Workflow

This guide demonstrates a **two-phase approach** for case creation that matches real-world case management processes:

### **Desired Workflow**:
1. **Step 1**: Create case draft using `/createcase-draft` → Returns `case_id`
2. **Step 2**: Enhance case using `/enhance-case` with the `case_id` + allegations/entities/narratives

### **Current Implementation**: ✅ **ALREADY SUPPORTS THIS WORKFLOW**

The current endpoints perfectly match your requirements:

| Endpoint | Purpose | Request DTO | Response |
|----------|---------|-------------|----------|
| `POST /api/cms/v1/createcase-draft` | Initial case creation | `CreateCaseDraftRequest` | Returns `case_id` (e.g., `CMS-2025-000001`) |
| `POST /api/cms/v1/enhance-case` | Add details to existing case | `EnhancedCreateCaseRequest` | Returns enhanced case with all details |

### **Benefits of This Approach**:
- **🎯 Progressive Case Building**: Start simple, add complexity later
- **📋 Draft Management**: Proper draft lifecycle with workflow integration  
- **🔄 Workflow Integration**: Both endpoints integrate with Flowable BPMN processes
- **📊 Audit Trail**: Complete tracking from draft to final case
- **🚦 Authorization Control**: Different permissions for draft vs enhancement phases

---

## Part 1: Workflow Registration and Deployment

### Step 1: Register Workflow Metadata

**Endpoint**: `POST /api/{businessAppName}/workflow-metadata/register`  
**URL**: http://localhost:8082/swagger-ui.html

1. Navigate to **Workflow Metadata Controller** section
2. Click on `POST /api/{businessAppName}/workflow-metadata/register`
3. Click "Try it out"
4. Set parameters:
   - **businessAppName**: `onecms`
5. **Request Body**:
```json
{
  "processDefinitionKey": "oneCmsCaseWorkflow",
  "processName": "OneCMS Case Management Workflow",
  "businessAppName": "onecms",
  "description": "Comprehensive case management workflow with EO intake, department routing, and investigation",
  "candidateGroupMappings": {
    "GROUP_EO_INTAKE_ANALYST": "eo-intake-analyst-queue",
    "GROUP_EO_HEAD": "eo-head-queue",
    "GROUP_EO_OFFICER": "eo-officer-queue",
    "GROUP_CSIS_INTAKE_MANAGER": "csis-intake-manager-queue",
    "GROUP_CSIS_INTAKE_ANALYST": "csis-intake-analyst-queue",
    "GROUP_ER_INTAKE_ANALYST": "er-intake-analyst-queue",
    "GROUP_LEGAL_INTAKE_ANALYST": "legal-intake-analyst-queue",
    "GROUP_INVESTIGATION_MANAGER": "investigation-manager-queue",
    "GROUP_INVESTIGATOR": "investigator-queue"
  },
  "metadata": {
    "version": "1.0",
    "author": "NextGen Workflow Team",
    "lanes": ["EO Intake", "EO Officer", "Department Analysts", "Investigation Team"]
  }
}
```

6. **Add Headers**:
   - `X-User-Id`: `alice.intake`

7. **Expected Response** (201 Created):
```json
{
  "id": 1,
  "processDefinitionKey": "oneCmsCaseWorkflow",
  "processName": "OneCMS Case Management Workflow",
  "businessAppName": "onecms",
  "active": true,
  "deployed": false,
  "candidateGroupMappings": {...},
  "createdAt": "2025-09-04T10:00:00Z"
}
```

### Step 2: Deploy BPMN from File System

**Endpoint**: `POST /api/{businessAppName}/workflow-metadata/deploy-from-file`

1. Navigate to **Workflow Metadata Controller** section  
2. Click on `POST /api/{businessAppName}/workflow-metadata/deploy-from-file`
3. Click "Try it out"
4. Set parameters:
   - **businessAppName**: `onecms`
   - **processDefinitionKey**: `oneCmsCaseWorkflow`
   - **filename**: `OneCMS_Case_Workflow.bpmn20.xml`

5. **Add Headers**:
   - `X-User-Id`: `alice.intake`

6. **Expected Response** (200 OK):
```json
{
  "id": 1,
  "processDefinitionKey": "oneCmsCaseWorkflow",
  "processName": "OneCMS Case Management Workflow",
  "deployed": true,
  "deploymentId": "deployment-12345",
  "version": 1
}
```

**File Path Configuration**: The BPMN file should be located at:
```
/Users/arunvaidhyanathan/Developer/nextgen-workflow/flowable-wrapper-v2/src/main/resources/bpmn/OneCMS_Case_Workflow.bpmn20.xml
```

---

## Part 2: Case Draft Creation and Testing

### **Authorization Setup (Required for All OneCMS Endpoints)**

Before testing any OneCMS endpoints, you must authenticate in Swagger UI:

1. Navigate to http://localhost:8083/api/swagger-ui.html
2. **Click the "Authorize" button (🔒)** at the top right
3. **In the Authorization modal**:
   - **X-User-Id**: Enter `alice.intake`
   - **X-Session-Id**: Leave empty or enter any value
   - Click **"Authorize"** for each
   - Click **"Close"**

**The headers will now be automatically included in all requests!**

### Step 3: Create Case Draft

**Endpoint**: `POST /api/cms/v1/createcase-draft`  
**URL**: http://localhost:8083/api/swagger-ui.html

1. Navigate to **Case Management Controller** section
2. Click on `POST /api/cms/v1/createcase-draft`
3. Click "Try it out"
4. **Request Body** (from `phase1_case_draft_test.json`):
```json
{
  "case_draft_title": "Employee Misconduct Investigation Draft",
  "case_draft_description": "Initial draft for comprehensive employee misconduct case",
  "priority": "HIGH",
  "case_type_id": 1,
  "department_id": 1,
  "date_received_by_escalation_channel": "2025-09-04",
  "complaint_received_method": "Citi Ethics Office",
  "process_definition_key": "oneCmsCaseWorkflow"
}
```

5. Click **"Execute"** (headers are automatically included after authorization)

6. **Expected Response** (201 Created):
```json
{
  "caseDraftId": 123,
  "caseId": "CMS-2025-000001",
  "caseNumber": "CMS-2025-000001",
  "title": "Employee Misconduct Investigation Draft",
  "status": "DRAFT",
  "workflowMetadata": {
    "processInstanceId": "proc_inst_456",
    "processDefinitionKey": "oneCmsCaseWorkflow",
    "workflowStatus": "STARTED",
    "currentTask": {
      "taskId": "task_789",
      "taskName": "Create a New Case",
      "queueName": "eo-intake-analyst-queue",
      "status": "OPEN",
      "candidateGroup": "GROUP_EO_INTAKE_ANALYST"
    }
  }
}
```

**🔥 Important**: Save the `caseId` value (`CMS-2025-000001`) for the next step.

---

## Part 3: Case Enhancement and Completion

### **Current Implementation Workflow**

The current endpoints perfectly match the two-phase approach:

| Endpoint | Purpose | Request DTO | Response |
|----------|---------|-------------|----------|
| `POST /api/cms/v1/createcase-draft` | Initial case creation | `CreateCaseDraftRequest` | Returns `case_id` (e.g., `CMS-2025-000001`) |
| `POST /api/cms/v1/enhance-case` | Add details to existing case | `EnhancedCreateCaseRequest` | Returns enhanced case with all details |

### Step 4: Enhance Case with Full Details

**Endpoint**: `POST /api/cms/v1/enhance-case`  
**URL**: http://localhost:8083/api/swagger-ui.html

1. Navigate to **Case Management Controller** section
2. Click on `POST /api/cms/v1/enhance-case`
3. Click "Try it out"
4. **Request Body** (from `phase2_case_enhancement_test.json` - **Replace `case_id` with value from Step 3**):
```json
{
  "case_id": "CMS-2025-000001",
  "allegations": [
    {
      "allegationType": "HARASSMENT",
      "severity": "HIGH",
      "description": "Workplace harassment creating hostile work environment",
      "subject": "Harassment of female colleagues",
      "narrative": "Multiple reports of inappropriate comments and behavior towards female staff members during team meetings and private conversations.",
      "investigationFunction": "HR",
      "grcTaxonomyLevel1": "CONDUCT",
      "grcTaxonomyLevel2": "HARASSMENT",
      "grcTaxonomyLevel3": "WORKPLACE",
      "grcTaxonomyLevel4": "VERBAL"
    },
    {
      "allegationType": "Tracked",
      "severity": "MEDIUM",
      "description": "Potential expense report fraud and misuse of company funds",
      "subject": "Fraudulent expense claims",
      "narrative": "Suspicious expense reports with duplicate receipts and personal expenses claimed as business expenses over the past 6 months.",
      "investigationFunction": "LEGAL",
      "grcTaxonomyLevel1": "FINANCIAL",
      "grcTaxonomyLevel2": "FRAUD",
      "grcTaxonomyLevel3": "EXPENSES",
      "grcTaxonomyLevel4": "RECEIPTS"
    },
    {
      "allegationType": "Tracked",
      "severity": "CRITICAL",
      "description": "Data security violations and unauthorized access",
      "subject": "Security policy violations",
      "narrative": "Employee attempted to access confidential client data outside of assigned role and downloaded files to personal devices.",
      "investigationFunction": "CSIS",
      "grcTaxonomyLevel1": "SECURITY",
      "grcTaxonomyLevel2": "DATA_BREACH",
      "grcTaxonomyLevel3": "UNAUTHORIZED_ACCESS",
      "grcTaxonomyLevel4": "CLIENT_DATA"
    }
  ],
  "entities": [
    {
      "type": "Person",
      "relationshipType": "SUBJECT",
      "investigationFunction": "PRIMARY",
      "soeid": "EMP123456",
      "geid": "GE789012",
      "firstName": "John",
      "middleName": "Michael",
      "lastName": "Doe",
      "emailAddress": "john.doe@company.com",
      "phoneNumber": "+1-555-0123",
      "address": "123 Business St",
      "city": "New York",
      "state": "NY",
      "zip": "10001",
      "hireDate": "2020-01-15",
      "manager": "Sarah Johnson",
      "goc": "US",
      "hrResponsible": "Jennifer Wilson",
      "legalVehicle": "Citibank NA",
      "managedSegment": "Corporate Banking",
      "relationshipToCiti": "EMPLOYEE",
      "preferredContactMethod": "EMAIL",
      "anonymous": false
    },
    {
      "type": "Person",
      "relationshipType": "WITNESS",
      "investigationFunction": "SUPPORTING",
      "soeid": "EMP654321",
      "geid": "GE210987",
      "firstName": "Alice",
      "lastName": "Johnson",
      "emailAddress": "alice.johnson@company.com",
      "phoneNumber": "+1-555-0456",
      "relationshipToCiti": "EMPLOYEE",
      "preferredContactMethod": "PHONE",
      "anonymous": false
    },
    {
      "type": "Organization",
      "relationshipType": "EXTERNAL_PARTY",
      "investigationFunction": "AFFECTED_PARTY",
      "organizationName": "ABC Client Corporation",
      "emailAddress": "compliance@abcclient.com",
      "phoneNumber": "+1-555-0789",
      "address": "456 Client Avenue",
      "city": "Boston",
      "state": "MA",
      "zip": "02101",
      "preferredContactMethod": "EMAIL",
      "anonymous": false
    }
  ],
  "narratives": [
    {
      "type": "INITIAL_COMPLAINT",
      "title": "Initial Complaint Report",
      "narrative": "This case was initiated following multiple reports from staff members regarding inappropriate conduct by John Doe. The complainant, Jane Smith, filed a formal complaint after witnessing discriminatory behavior during a team meeting on August 15, 2025.",
      "investigationFunction": "INTAKE"
    },
    {
      "type": "FINANCIAL_ANALYSIS",
      "title": "Expense Report Analysis",
      "narrative": "Financial review of expense reports submitted by John Doe from February 2025 to August 2025 reveals patterns consistent with fraudulent activity. Notable discrepancies include duplicate receipts totaling $3,247 and personal meal expenses claimed as client entertainment.",
      "investigationFunction": "LEGAL"
    },
    {
      "type": "SECURITY_ASSESSMENT",
      "title": "IT Security Incident Report",
      "narrative": "IT security logs show that John Doe accessed the client database system outside normal business hours on July 22, 2025, and downloaded 47 files containing sensitive client information. These files were transferred to a USB device registered to his personal laptop.",
      "investigationFunction": "CSIS"
    },
    {
      "type": "WITNESS_STATEMENT",
      "title": "Witness Statement - Alice Johnson",
      "narrative": "Alice Johnson confirms observing John Doe making inappropriate comments about female colleagues' appearance during the August 15 team meeting. She also reports seeing him working late hours accessing systems he doesn't normally use in his role.",
      "investigationFunction": "HR"
    }
  ]
}
```

5. Click **"Execute"** (headers are automatically included after authorization)

6. **Expected Response** (200 OK):
```json
{
  "caseId": "CMS-2025-000001",
  "caseNumber": "CMS-2025-000001",
  "title": "Employee Misconduct Investigation Draft",
  "status": "OPEN",
  "severity": "HIGH",
  "totalAllegations": 3,
  "totalEntities": 3,
  "totalNarratives": 4,
  "workflowMetadata": {
    "processInstanceId": "proc_inst_456",
    "workflowStatus": "ACTIVE",
    "currentTaskCount": 1
  }
}
```

---

## Part 4: Additional Testing Endpoints

### Check Case Details
**Endpoint**: `GET /v1/cases/{caseNumber}`

1. Use the case number from previous steps: `CMS-2025-000001`
2. Add `X-User-Id` header: `alice.intake`

### View Case Allegations
**Endpoint**: `GET /v1/cases/{caseNumber}/allegations`

### View Case Entities  
**Endpoint**: `GET /v1/cases/{caseNumber}/entities`

### View Case Narratives
**Endpoint**: `GET /v1/cases/{caseNumber}/narratives`

### Check Workflow Status
**Endpoint**: `GET /v1/cases/{caseNumber}/workflow-status`

### View Workflow Journey
**Endpoint**: `GET /v1/cases/{caseNumber}/journey`

### Dashboard Cases
**Endpoint**: `GET /v1/cases/dashboard-cases`

### Dashboard Statistics
**Endpoint**: `GET /v1/cases/dashboard-stats`

### My Cases
**Endpoint**: `GET /v1/cases/my-cases`

---

## Database Schema Reference

### OneCMS Schema (`onecms`)

#### Core Business Tables

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `cases` | Main case records | `id`, `case_number`, `title`, `status`, `priority`, `severity` |
| `case_drafts` | Draft cases with workflow integration | `id`, `case_id`, `process_instance_id`, `task_id` |
| `allegations` | Case allegations/charges | `id`, `case_id`, `allegation_type`, `severity`, `grc_taxonomy_*` |
| `case_entities` | People/organizations in cases | `id`, `case_id`, `type`, `relationship_type`, `first_name`, `last_name` |
| `case_narratives` | Case narrative content | `id`, `case_id`, `type`, `title`, `narrative`, `investigation_function` |
| `case_comments` | Case comments and notes | `id`, `case_id`, `comment_text`, `created_by` |
| `case_transitions` | Case status change audit | `id`, `case_id`, `from_status`, `to_status`, `created_by` |

#### Reference Data Tables

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `case_types` | Available case types | `id`, `type_name`, `description`, `active` |
| `departments` | Department reference | `id`, `department_name`, `description`, `manager_email` |
| `countries_clusters` | Geographic data | `id`, `country_code`, `country_name`, `cluster_name` |
| `data_sources` | Data source classifications | `id`, `source_code`, `source_name` |
| `escalation_methods` | Escalation methods | `id`, `method_code`, `method_name` |

#### Key Relationships
- `cases` 1:N `allegations`
- `cases` 1:N `case_entities` 
- `cases` 1:N `case_narratives`
- `cases` 1:N `case_comments`
- `cases` 1:1 `case_drafts` (optional)

### Flowable Schema (`flowable`)

#### Core Workflow Tables

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `workflow_metadata` | Process definitions and queue mappings | `id`, `process_definition_key`, `candidate_group_mappings`, `deployed` |
| `queue_tasks` | Task queue abstraction | `task_id`, `queue_name`, `assignee`, `status`, `process_instance_id` |

#### Flowable Engine Tables (ACT_*)

| Table Prefix | Purpose | Examples |
|--------------|---------|----------|
| `ACT_RE_*` | Repository (definitions) | `ACT_RE_PROCDEF`, `ACT_RE_DEPLOYMENT` |
| `ACT_RU_*` | Runtime (active instances) | `ACT_RU_EXECUTION`, `ACT_RU_TASK`, `ACT_RU_VARIABLE` |
| `ACT_HI_*` | History (completed instances) | `ACT_HI_PROCINST`, `ACT_HI_TASKINST`, `ACT_HI_ACTINST` |
| `ACT_ID_*` | Identity (users/groups) | `ACT_ID_USER`, `ACT_ID_GROUP`, `ACT_ID_MEMBERSHIP` |

#### Key Flowable Tables for Monitoring

| Table | Purpose | Monitor For |
|-------|---------|-------------|
| `ACT_RU_EXECUTION` | Active process instances | `PROC_INST_ID_`, `BUSINESS_KEY_`, `PROC_DEF_ID_` |
| `ACT_RU_TASK` | Active tasks | `ID_`, `NAME_`, `ASSIGNEE_`, `CANDIDATE_GROUPS_` |
| `ACT_RU_VARIABLE` | Process variables | `PROC_INST_ID_`, `NAME_`, `TEXT_`, `LONG_` |
| `ACT_HI_PROCINST` | Process instance history | `PROC_INST_ID_`, `BUSINESS_KEY_`, `START_TIME_`, `END_TIME_` |
| `ACT_HI_TASKINST` | Task history | `ID_`, `PROC_INST_ID_`, `NAME_`, `ASSIGNEE_`, `START_TIME_`, `END_TIME_` |

---

## Database Monitoring Queries

### Check Active Process Instances
```sql
SELECT 
    e.PROC_INST_ID_,
    e.BUSINESS_KEY_,
    pd.NAME_ as process_name,
    e.START_TIME_
FROM flowable.ACT_RU_EXECUTION e
JOIN flowable.ACT_RE_PROCDEF pd ON e.PROC_DEF_ID_ = pd.ID_
WHERE e.PARENT_ID_ IS NULL;
```

### Check Active Tasks
```sql  
SELECT 
    t.ID_ as task_id,
    t.NAME_ as task_name,
    t.ASSIGNEE_,
    t.CANDIDATE_GROUPS_,
    t.CREATE_TIME_,
    t.PROC_INST_ID_
FROM flowable.ACT_RU_TASK t
ORDER BY t.CREATE_TIME_ DESC;
```

### Check Queue Tasks Status
```sql
SELECT 
    queue_name,
    status,
    COUNT(*) as task_count
FROM flowable.queue_tasks
GROUP BY queue_name, status
ORDER BY queue_name;
```

### Check Case Workflow Integration
```sql
SELECT 
    c.case_number,
    c.status as case_status,
    cd.process_instance_id,
    cd.task_id,
    cd.queue_name,
    e.BUSINESS_KEY_
FROM onecms.cases c
LEFT JOIN onecms.case_drafts cd ON c.id = cd.case_id
LEFT JOIN flowable.ACT_RU_EXECUTION e ON cd.process_instance_id = e.PROC_INST_ID_
WHERE c.case_number = 'CMS-2025-000001';
```

---

## Troubleshooting

### Common Issues

1. **404 Not Found**: Ensure all services are running and registered with Eureka
2. **401/403 Authorization**: Check `X-User-Id` header is present and valid
3. **Workflow Not Found**: Ensure workflow metadata is registered before deployment
4. **File Not Found**: Verify BPMN file path in `workflow.definitions.path` configuration
5. **Database Connection**: Check PostgreSQL is running and schemas exist

### Verification Steps

1. **Check Service Registry**: http://localhost:8761
2. **Check Health Endpoints**:
   - Flowable: http://localhost:8082/actuator/health
   - OneCMS: http://localhost:8083/actuator/health
3. **Check Database Tables**:
   ```sql
   SELECT COUNT(*) FROM onecms.cases;
   SELECT COUNT(*) FROM flowable.workflow_metadata;
   SELECT COUNT(*) FROM flowable.ACT_RU_TASK;
   ```

---

## Summary

This guide covers the complete testing workflow:

1. ✅ **Register workflow metadata** with candidate group to queue mappings
2. ✅ **Deploy BPMN file** from the mounted file system
3. ✅ **Create case draft** which starts the workflow automatically
4. ✅ **Enhance case** with full details (allegations, entities, narratives)
5. ✅ **Monitor progress** through various endpoints and database queries

The integration creates a complete audit trail from case creation through workflow execution, with all data properly stored in the respective microservice databases.