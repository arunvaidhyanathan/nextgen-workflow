# OneCMS Clean Case Management Workflow

## Overview

The OneCMS Clean Case Management Workflow (`oneCmsCleanCaseWorkflow`) is a comprehensive BPMN 2.0 process designed to handle case management from initial intake through investigation completion. The workflow implements a four-tier organizational structure with clear separation of responsibilities and sophisticated routing logic.

## Workflow Architecture

### Swimlane Structure

```
┌─────────────────────┐
│   EO Intake Lane    │ ← Initial case creation and information gathering
├─────────────────────┤
│   EO Officer Lane   │ ← Case assignment, routing decisions, and oversight
├─────────────────────┤
│ Department Analysts │ ← Specialized department review and processing
├─────────────────────┤
│ Investigation Team  │ ← Investigation execution and completion
└─────────────────────┘
```

### Key Stakeholders and Roles

| Swimlane | Role Groups | Responsibilities |
|----------|-------------|------------------|
| **EO Intake** | `GROUP_EO_INTAKE_ANALYST` | Case creation, information gathering |
| **EO Officer** | `GROUP_EO_HEAD`, `GROUP_EO_OFFICER` | Case assignment, routing, and decision-making |
| **Department Analysts** | `GROUP_CSIS_INTAKE_ANALYST`, `GROUP_ER_INTAKE_ANALYST`, `GROUP_LEGAL_INTAKE_ANALYST` | Specialized departmental review and processing |
| **Investigation Team** | `GROUP_INVESTIGATION_MANAGER`, `GROUP_INVESTIGATOR` | Investigation execution and management |

## Process Flow Diagram

```
[Start] → Create Case → Fill Information → Case Action Decision
                                                ├── Cancel → [End: Cancelled]
                                                └── Create ↓
                        
EO Head Assign → EO Officer Routing → Officer Decision Gateway
                                            ├── Send to CSIS → Dept Review
                                            ├── Send to ER → ER Routing ┬── Keep in ER → Dept Review
                                            │                            └── Route to CSIS → Dept Review
                                            ├── Send to Legal → Dept Review
                                            ├── Send to IU → Investigation Manager
                                            ├── Send Back → Fill Information
                                            ├── Reject → [End: Rejected]
                                            ├── Save & Close → [End: Closed]
                                            └── Non-tracked → [End: Non-tracked]

Department Review → Add Details → Department Action Gateway
                                        ├── Send Back to EO → Officer Routing
                                        ├── Assign Investigation → Investigation Manager
                                        └── Save & Close → [End: Completed]

Investigation Manager → Investigator Planning → Active Investigation → Investigation Complete?
                                                                               ├── Complete → [End: Completed]
                                                                               └── Continue → Active Investigation
```

## Detailed Process Flow

### Phase 1: Case Intake (EO Intake Lane)

#### 1.1 Start Event: EO Case Received
- **Trigger**: External case submission or internal referral
- **Variables Set**: `caseState = 'INTAKE'`
- **Next Step**: Create a New Case

#### 1.2 Create a New Case
- **Assignee**: `GROUP_EO_INTAKE_ANALYST`
- **Purpose**: Initialize case record with basic information
- **Next Step**: Fill Case Information

#### 1.3 Fill Case Information (Pop-up)
- **Assignee**: `GROUP_EO_INTAKE_ANALYST`
- **Purpose**: Comprehensive data entry including:
  - Case details and description
  - Involved entities (people/organizations)
  - Allegations and supporting evidence
  - Case narratives and documentation
- **Next Step**: Case Action Decision Gateway

#### 1.4 Case Action Decision Gateway
**Decision Variable**: `caseAction`

| Action | Condition | Next Step |
|--------|-----------|-----------|
| **CREATE** | `${caseAction == 'CREATE'}` | Assign Case (EO Head) |
| **CANCEL** | `${caseAction == 'CANCEL'}` | End: Case Cancelled |

### Phase 2: Case Assignment and Routing (EO Officer Lane)

#### 2.1 Assign Case
- **Assignee**: `GROUP_EO_HEAD`
- **Purpose**: EO Head assigns case to appropriate EO Officer
- **Next Step**: EO Officer Routing

#### 2.2 EO Officer Routing
- **Assignee**: `GROUP_EO_OFFICER`
- **Purpose**: Officer reviews case and determines routing strategy
- **Available Actions**:
  - Route to specialized departments (CSIS/ER/Legal/IU)
  - Send back to EO Intake for additional information
  - Reject as invalid case
  - Save and close without further action
  - Mark as non-tracked case
- **Next Step**: EO Officer Decision Gateway

#### 2.3 EO Officer Decision Gateway
**Decision Variable**: `eoOfficerAction`

| Action | Condition | Next Step | Description |
|--------|-----------|-----------|-------------|
| **SEND_TO_CSIS** | `${eoOfficerAction == 'SEND_TO_CSIS'}` | Department Review | Corporate Security routing |
| **SEND_TO_ER** | `${eoOfficerAction == 'SEND_TO_ER'}` | ER Routing Decision | Employee Relations with sub-routing |
| **SEND_TO_LEGAL** | `${eoOfficerAction == 'SEND_TO_LEGAL'}` | Department Review | Legal department routing |
| **SEND_TO_IU** | `${eoOfficerAction == 'SEND_TO_IU'}` | Investigation Manager | Direct to investigation |
| **SEND_BACK** | `${eoOfficerAction == 'SEND_BACK'}` | Fill Information | Return to intake for more info |
| **REJECT** | `${eoOfficerAction == 'REJECT'}` | End: Case Rejected | Invalid or duplicate case |
| **SAVE_CLOSE** | `${eoOfficerAction == 'SAVE_CLOSE'}` | End: Case Closed | Archive without action |
| **NON_TRACKED** | `${eoOfficerAction == 'NON_TRACKED'}` | End: Non-tracked | Informational/commentary |

### Phase 3: Department Processing (Department Analysts Lane)

#### 3.1 ER Sub-routing Pattern (Extensible Design)

##### 3.1.1 ER Routing Decision
- **Assignee**: `GROUP_ER_INTAKE_ANALYST`
- **Purpose**: ER analyst determines internal handling vs. security escalation
- **Next Step**: ER Routing Decision Gateway

##### 3.1.2 ER Routing Decision Gateway
**Decision Variable**: `erRoutingAction`

| Action | Condition | Next Step | Description |
|--------|-----------|-----------|-------------|
| **KEEP_ER** | `${erRoutingAction == 'KEEP_ER'}` | Department Review | Handle within Employee Relations |
| **ROUTE_TO_CSIS** | `${erRoutingAction == 'ROUTE_TO_CSIS'}` | Department Review | Escalate to CSIS for security aspects |

#### 3.2 Department Review
- **Dynamic Assignee**: `${departmentGroup}` (determined by execution listener)
- **Group Assignment Logic**:
  ```java
  departmentGroup = 
    eoOfficerAction == 'SEND_TO_CSIS' || erRoutingAction == 'ROUTE_TO_CSIS' ? 'GROUP_CSIS_INTAKE_ANALYST' :
    eoOfficerAction == 'SEND_TO_ER' || erRoutingAction == 'KEEP_ER' ? 'GROUP_ER_INTAKE_ANALYST' :
    eoOfficerAction == 'SEND_TO_LEGAL' ? 'GROUP_LEGAL_INTAKE_ANALYST' : 
    'GROUP_INVESTIGATION_MANAGER'
  ```
- **Purpose**: Specialized department analyst reviews case based on expertise area
- **Next Step**: Add Details

#### 3.3 Add Information, Narrative, Allegations, Entities
- **Assignee**: Same as Department Review (`${departmentGroup}`)
- **Purpose**: Department analyst enhances case with:
  - Additional evidence and documentation
  - Updated allegations based on department expertise
  - Relevant entities and relationships
  - Department-specific narrative updates
- **Next Step**: Department Action Gateway

#### 3.4 Department Action Gateway
**Decision Variable**: `deptAction`

| Action | Condition | Next Step | Description |
|--------|-----------|-----------|-------------|
| **SEND_BACK_TO_EO** | `${deptAction == 'SEND_BACK_TO_EO'}` | EO Officer Routing | Return to EO for re-routing |
| **ASSIGN_INVESTIGATION** | `${deptAction == 'ASSIGN_INVESTIGATION'}` | Assign to Investigation Manager | Proceed to formal investigation |
| **SAVE_CLOSE** | `${deptAction == 'SAVE_CLOSE'}` | End: Case Completed | Close case at department level |

### Phase 4: Investigation Execution (Investigation Team Lane)

#### 4.1 Assign to Investigation Manager
- **Assignee**: `${departmentGroup}` (department analyst)
- **Purpose**: Department prepares case handoff to investigation team
- **Next Step**: Investigation Manager Assignment

#### 4.2 Investigation Manager Assignment
- **Assignee**: `GROUP_INVESTIGATION_MANAGER`
- **Purpose**: Investigation manager reviews case and assigns to investigator
- **Next Step**: Investigator Review & Planning

#### 4.3 Investigator Review & Planning
- **Assignee**: `GROUP_INVESTIGATOR`
- **Purpose**: Assigned investigator:
  - Reviews complete case file
  - Develops investigation plan
  - Identifies required resources and timeline
- **Next Step**: Active Investigation

#### 4.4 Active Investigation
- **Assignee**: `GROUP_INVESTIGATOR`
- **Purpose**: Execute investigation activities:
  - Conduct interviews and evidence gathering
  - Document findings and progress
  - Update case with investigation results
- **Next Step**: Investigation Complete Gateway

#### 4.5 Investigation Complete Gateway
**Decision Variable**: `investigationStatus`

| Action | Condition | Next Step | Description |
|--------|-----------|-----------|-------------|
| **COMPLETE** | `${investigationStatus == 'COMPLETE'}` | End: Case Completed | Investigation finished |
| **CONTINUE** | `${investigationStatus == 'CONTINUE'}` | Active Investigation | Additional work required |

## Process Variables and State Management

### Core Process Variables

| Variable | Scope | Values | Usage |
|----------|-------|--------|-------|
| `caseState` | Global | `INTAKE`, `CANCELLED`, `COMPLETED`, `REJECTED`, `CLOSED`, `NON_TRACKED` | Case lifecycle tracking |
| `caseAction` | Intake | `CREATE`, `CANCEL` | Initial case decision |
| `eoOfficerAction` | Routing | `SEND_TO_CSIS`, `SEND_TO_ER`, `SEND_TO_LEGAL`, `SEND_TO_IU`, `SEND_BACK`, `REJECT`, `SAVE_CLOSE`, `NON_TRACKED` | EO Officer routing decision |
| `erRoutingAction` | ER Sub-routing | `KEEP_ER`, `ROUTE_TO_CSIS` | ER internal routing |
| `deptAction` | Department | `SEND_BACK_TO_EO`, `ASSIGN_INVESTIGATION`, `SAVE_CLOSE` | Department analyst decision |
| `investigationStatus` | Investigation | `COMPLETE`, `CONTINUE` | Investigation progress |
| `departmentGroup` | Dynamic | Candidate group IDs | Runtime role assignment |

### Case State Transitions

```
INTAKE → CANCELLED (Cancel during intake)
INTAKE → REJECTED (Rejected by EO Officer)
INTAKE → CLOSED (Saved and closed by EO Officer)
INTAKE → NON_TRACKED (Marked as non-tracked)
INTAKE → COMPLETED (Completed through department or investigation)
```

## Technical Implementation Details

### Dynamic Role Assignment Pattern

The workflow uses execution listeners to dynamically assign candidate groups based on routing decisions:

```xml
<flowable:executionListener event="start" expression="${execution.setVariable('departmentGroup', 
  eoOfficerAction == 'SEND_TO_CSIS' || erRoutingAction == 'ROUTE_TO_CSIS' ? 'GROUP_CSIS_INTAKE_ANALYST' :
  eoOfficerAction == 'SEND_TO_ER' || erRoutingAction == 'KEEP_ER' ? 'GROUP_ER_INTAKE_ANALYST' :
  eoOfficerAction == 'SEND_TO_LEGAL' ? 'GROUP_LEGAL_INTAKE_ANALYST' : 
  'GROUP_INVESTIGATION_MANAGER')}" />
```

### Sub-routing Architecture (Extensible Pattern)

The ER sub-routing implementation serves as a template for future department-specific routing:

1. **Department-Specific Routing Task**: Specialized decision point
2. **Department Gateway**: Business logic for internal routing
3. **Dynamic Group Assignment**: Runtime candidate group determination
4. **Loop-back Support**: Return to previous steps when needed

## Business Rules and Routing Logic

### Department Routing Criteria

| Department | Typical Case Types | Routing Rationale |
|------------|-------------------|-------------------|
| **CSIS** | Security incidents, data breaches, information security | Technical security expertise |
| **ER** | Employee misconduct, HR violations, workplace issues | Human resources and employee relations |
| **Legal** | Regulatory compliance, legal violations, contractual issues | Legal analysis and compliance |
| **IU** | Direct investigation requests, complex multi-department cases | Immediate investigation requirement |

### ER Sub-routing Business Logic

**Keep in ER** scenarios:
- Pure HR policy violations
- Employee performance issues
- Workplace harassment (non-security)

**Route to CSIS** scenarios:
- Security-related employee misconduct
- Data access violations
- Potential insider threats

## End State Definitions

| End State | Business Meaning | Trigger Conditions |
|-----------|------------------|-------------------|
| **Case Completed** | Successfully processed and resolved | Investigation complete or department closure |
| **Case Cancelled** | Cancelled during intake phase | User cancellation during initial creation |
| **Case Rejected** | Invalid or duplicate case | EO Officer rejection |
| **Case Closed (Saved)** | Archived without further action | EO Officer or Department save & close |
| **Non-tracked Case** | Informational/commentary only | EO Officer non-tracked designation |

## Integration Points

### Microservices Integration

The workflow integrates with the NextGen Workflow microservices architecture:

- **OneCMS Service**: Case data persistence and API endpoints
- **Entitlement Service**: Authorization and role-based access control
- **API Gateway**: Request routing and session management

### Authorization Model

Each task integrates with Cerbos policy engine for fine-grained authorization:

```
Task Authorization = f(User Roles, Department Assignment, Case Ownership, Queue Membership)
```

### Task Queue Mappings

| Business Role | Queue Assignment | Task Types |
|---------------|------------------|------------|
| `GROUP_EO_INTAKE_ANALYST` | `eo-intake-queue` | Case creation and information gathering |
| `GROUP_EO_HEAD` | `eo-head-queue` | Case assignment and oversight |
| `GROUP_EO_OFFICER` | `eo-officer-queue` | Routing and triage decisions |
| `GROUP_CSIS_INTAKE_ANALYST` | `csis-intake-analyst-queue` | CSIS department review |
| `GROUP_ER_INTAKE_ANALYST` | `er-intake-analyst-queue` | ER department review and routing |
| `GROUP_LEGAL_INTAKE_ANALYST` | `legal-intake-analyst-queue` | Legal department review |
| `GROUP_INVESTIGATION_MANAGER` | `investigation-manager-queue` | Investigation assignment |
| `GROUP_INVESTIGATOR` | `investigator-queue` | Investigation execution |

## Workflow Patterns and Best Practices

### 1. **Gateway Decision Pattern**
Each decision point uses exclusive gateways with explicit condition expressions:
```xml
<conditionExpression xsi:type="tFormalExpression">${variableName == 'VALUE'}</conditionExpression>
```

### 2. **Loop-back Pattern**
The workflow supports returning to previous steps:
- EO Officer → EO Intake (Send Back)
- Department → EO Officer (Send Back to EO)
- Investigation → Active Investigation (Continue)

### 3. **Sub-routing Pattern**
ER sub-routing demonstrates extensible department-specific routing:
```
Department Entry → Department-Specific Routing → Department Gateway → Continue Processing
```

### 4. **Dynamic Assignment Pattern**
Uses execution listeners for runtime role assignment based on routing decisions.

### 5. **State Management Pattern**
Comprehensive state tracking through execution listeners at end events.

## Error Handling and Edge Cases

### Validation Points
1. **Case Creation**: Validate required fields during intake
2. **Department Assignment**: Ensure valid department selection
3. **Investigation Assignment**: Verify investigator availability
4. **Authorization**: Check permissions at each task assignment

### Loop-back Scenarios
- **Insufficient Information**: EO Officer sends back to intake
- **Incorrect Routing**: Department sends back to EO Officer
- **Investigation Extension**: Investigator continues active investigation

### Terminal States
- Multiple explicit end events for clear audit trail
- State variables set for downstream system integration
- No implicit process termination

## Future Extensibility

### Adding New Departments
The sub-routing pattern can be extended for other departments:

1. **Add Department Lane Reference**
2. **Create Department-Specific Routing Task**
3. **Implement Department Gateway**
4. **Update Dynamic Group Assignment Logic**
5. **Add Sequence Flows and BPMN Diagram Elements**

### Adding New Decision Points
The gateway pattern supports additional decision criteria:
- Multi-condition routing
- Risk-based routing
- Priority-based assignment
- Workload balancing

## Technical Configuration

### Process Definition
- **Process ID**: `oneCmsCleanCaseWorkflow`
- **Process Name**: OneCMS Clean Case Management Workflow
- **Executable**: `true`
- **Target Namespace**: `http://www.onecms.com/workflow`

### Database Integration
- **Schema**: `flowable` (Flowable engine tables)
- **Custom Tables**: `workflow_metadata`, `queue_tasks`
- **External References**: Case data in `onecms` schema

### Performance Considerations
- **Task Query Optimization**: Index on candidate groups and assignment
- **Process Instance Tracking**: Efficient lookup by business key
- **Variable Storage**: Minimize variable scope and size
- **Concurrent Execution**: Support for parallel case processing

## Monitoring and Metrics

### Key Performance Indicators
- **Cycle Time**: Total time from intake to completion
- **Queue Time**: Time spent in each department queue
- **Routing Accuracy**: Percentage of cases requiring re-routing
- **Investigation Success Rate**: Cases completed vs. continued

### Audit Trail
- Process instance history for compliance
- Task assignment and completion tracking
- Decision point audit with timestamps
- State transition logging

This workflow design provides a robust, scalable foundation for case management while maintaining flexibility for future enhancements and department-specific requirements.