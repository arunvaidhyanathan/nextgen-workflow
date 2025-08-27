# NextGen Workflow Application

A comprehensive microservices-based workflow management system designed for case management, investigations, and business process automation. Built with Spring Boot, React, and Flowable BPMN engine.

## Architecture Overview

This application follows a microservices architecture with:

- **API Gateway**: Central entry point with session-based authentication
- **Case Management Service**: Core business logic for case handling
- **Workflow Service**: Flowable BPMN-based process automation
- **User Management Service**: Authentication and authorization
- **React Frontend**: Modern UI built with TypeScript and Tailwind CSS

## Key Features

- **Session-Based Authentication**: Simple, secure authentication with X-Session-Id headers
- **Case Management**: Complete lifecycle management for investigation cases
- **Workflow Automation**: BPMN 2.0 compliant business process execution with OneCMS Unified Workflow
- **Role-Based Access Control**: Fine-grained permissions with Cerbos policy engine
- **Multi-Department Support**: Ethics Office (EO), Corporate Security (CSIS), Employee Relations (ER), Legal, and Investigation workflows
- **RESTful APIs**: Comprehensive OpenAPI 3.0 specification
- **Real-Time Notifications**: Task assignments and status updates
- **Queue-Based Task Management**: Automated task routing to department-specific queues

## Technology Stack

### Backend
- **Java 21** with Spring Boot 3.3.4
- **PostgreSQL 16.x** databases per service
- **Flowable BPMN Engine 7.2.0** for workflow automation
- **Cerbos 0.14.0** for policy-based authorization
- **Docker** containerization

### Frontend
- **React 18** with TypeScript
- **Vite** build tool
- **Tailwind CSS** + shadcn/ui components
- **TanStack Query** for data fetching
- **React Router** for navigation

## Project Structure

```
├── api-gateway/                  # Central API Gateway with authentication
├── onecms-service/              # Core case management microservice
├── flowable-wrapper-v2/         # Flowable BPMN workflow engine
├── entitlement-service/         # User authentication and management
├── service-registry/            # Eureka service discovery
├── CMS-UI-App/                  # React frontend application
├── database/                    # Centralized Liquibase migrations
├── cerbos/                      # Cerbos policy configuration
├── scripts/                     # Database initialization scripts
├── docker-compose-infrastructure.yml  # Infrastructure services
├── nextgen-workflow.md          # Comprehensive architecture documentation
├── nextgen-workflow-openapi-session-auth.yaml  # OpenAPI specification
└── NextGen-Workflow-API-Collection.json        # Postman testing collection
```

## Quick Start

### Prerequisites

- **Java 21+**
- **Node.js 18+**
- **PostgreSQL 16.x**
- **Docker & Docker Compose**
- **Cerbos 0.14.0** (via Docker)

### 1. Clone Repository

```bash
git clone <repository-url>
cd NextGen-Workflow-Application
```

### 2. Start Infrastructure Components

```bash
# Start PostgreSQL and Cerbos
docker-compose -f docker-compose-infrastructure.yml up -d

# Wait for services to be ready
docker-compose -f docker-compose-infrastructure.yml ps
```

### 3. Start Backend Services

```bash
# Start Service Registry first
cd service-registry && ./mvnw spring-boot:run

# Then start other services (in separate terminals):
cd entitlement-service && ./mvnw spring-boot:run
cd flowable-wrapper-v2 && ./mvnw spring-boot:run
cd onecms-service && ./mvnw spring-boot:run
cd api-gateway && ./mvnw spring-boot:run
```

### 4. Start Frontend

```bash
cd CMS-UI-App
npm install
npm run dev
```

### 5. Access Application

- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080/api
- **Service Registry (Eureka)**: http://localhost:8761
- **Cerbos Admin**: http://localhost:3593
- **PgAdmin** (if started): http://localhost:5050

### Service Ports
- **Service Registry**: 8761
- **API Gateway**: 8080
- **Entitlement Service**: 8081
- **Flowable Workflow Engine**: 8082
- **OneCMS Service**: 8083
- **PostgreSQL**: 5432
- **Cerbos gRPC**: 3592
- **Cerbos HTTP**: 3593

## 📚 Documentation

- **[Architecture Guide](nextgen-workflow.md)**: Comprehensive system architecture and design decisions
- **[API Documentation](nextgen-workflow-openapi-session-auth.yaml)**: OpenAPI 3.0 specification
- **[Frontend Guide](CMS-UI-App/CLAUDE.md)**: React application structure and development guide

## Authentication

The application uses session-based authentication:

1. **Login**: POST `/auth/login` → Get `sessionId`
2. **Authenticate**: Include `X-Session-Id` header in all requests
3. **Session Management**: 30-minute inactivity timeout

### Sample Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice.intake", "password": "password123"}'
```

## Testing

### Backend Testing
```bash
# Run tests for all services
./mvnw test

# Service-specific testing
cd case-management-service && ./mvnw test
```

### Frontend Testing
```bash
cd CMS-UI-App
npm run test
npm run lint
```

### API Testing
Import `NextGen-Workflow-API-Collection.json` into Postman for comprehensive API testing.

## Sample Data

The application includes pre-configured test users:

| Username | Password | Role | Department |
|----------|----------|------|------------|
| alice.intake | password123 | Intake Analyst | Intake |
| edward.inv | password123 | Investigator | Investigation |
| sarah.legal | password123 | Legal Counsel | Legal |
| mike.admin | password123 | Administrator | IT |

## Workflow Roles and Personas

The OneCMS Unified Workflow supports the following roles:

### Ethics Office (EO)
- **EO Head** (`GROUP_EO_HEAD`): Reviews and approves cases before departmental routing
- **EO Officer** (`GROUP_EO_OFFICER`): Performs case triage and routing decisions

### Corporate Security & Information Security (CSIS)
- **CSIS Intake Manager** (`GROUP_CSIS_INTAKE_MANAGER`): Reviews cases and assigns analysts
- **CSIS Intake Analyst** (`GROUP_CSIS_INTAKE_ANALYST`): Performs detailed case analysis

### Employee Relations (ER)
- **ER Intake Analyst** (`GROUP_ER_INTAKE_ANALYST`): Reviews ER cases and routes to HR or Investigation

### Legal Department
- **Legal Intake Analyst** (`GROUP_LEGAL_INTAKE_ANALYST`): Reviews legal matters and routes appropriately

### Investigation Unit
- **Investigation Manager** (`GROUP_INVESTIGATION_MANAGER`): Reviews cases and assigns investigators
- **Investigator** (`GROUP_INVESTIGATOR`): Executes investigation plans and tasks

## Deployment

### Development
```bash
docker-compose -f docker-compose.dev.yml up
```

### Production
```bash
docker-compose -f docker-compose.prod.yml up
```

## Monitoring & Observability

- **Health Checks**: `/actuator/health` for all services
- **Metrics**: Prometheus-compatible metrics at `/actuator/prometheus`
- **Logging**: Structured JSON logging with correlation IDs

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## Support

For questions and support:
- **Documentation**: See [nextgen-workflow.md](nextgen-workflow.md)
- **API Issues**: Check OpenAPI specification
- **Bug Reports**: Create GitHub issues
- **Feature Requests**: Submit enhancement proposals

---

**NextGen Workflow Application** - Streamlining case management and business process automation.

OneCMS: Unified Case Management Workflow - Technical & Business Overview
1. Executive Summary
This document provides a complete technical and business specification for the OneCMS Unified Case Management Workflow. This workflow is the core engine of the case management application, designed to handle the entire lifecycle of a case, from its initial creation to its final handoff for formal investigation.

The architecture is built on a set of core principles to ensure robustness, scalability, and clarity:

Unified but Specialized Intake: A centralized, multi-stage intake process managed by the Ethics Office (EO) ensures initial consistency for all externally reported cases.
Autonomous Departmental Initiation: Specialized departments, starting with CSIS, have the ability to initiate their own cases independently, following their unique internal procedures.
Intelligent, Rule-Based Routing: A central routing hub directs cases to the appropriate department (CSIS, ER, Legal) for specialized secondary intake.
Standardized Investigation Handoff: All paths, regardless of origin or departmental review, converge at a single, standardized point to hand off approved cases to the Investigation Unit. This ensures the formal investigation process is always initiated in a consistent manner.
This document serves as the definitive blueprint for developers, product owners, architects, and managers.

2. Parties & Personas
The workflow involves several key organizational units (Parties) and specific job functions (Personas/Roles) within them.

Party / Unit	Persona / Role	Key Responsibilities
Ethics Office (EO)	EO Intake Analyst	Initial case creation and data entry for externally reported cases.
EO Head	High-level validation and first-pass review of EO-initiated cases.
EO Officer	Central triage and routing authority for all EO-initiated cases.
CSIS	CSIS Intake Manager	Initiates cases, performs triage, assigns to analysts, and gives final approval for investigation.
CSIS Intake Analyst	Conducts detailed case vetting and makes recommendations for cases initiated by or routed to CSIS.
ER	ER Intake Analyst	Receives and reviews cases routed to Employee Relations, with the unique ability to assign directly to HR.
Legal	Legal Intake Analyst	Receives and reviews cases routed to the Legal department.
Investigation Unit	Investigation Manager	Receives approved cases from all intake paths and assigns them to a specific Investigator.
Investigator	Accepts case assignments, flags conflicts, and creates the formal investigation plan.
3. Pictorial Workflow Representation (PlantUML)
This diagram illustrates the complete, end-to-end process flow, with vertical partitions representing the different roles involved.

code
Plantuml
@startuml
title OneCMS End-to-End Unified Case Workflow

partition "EO Intake Analyst" #LightBlue
  start
  :Receive External Case Report;
  note right: Corresponds to **startCaseReceived** event.
  :Create & Fill Case Info;
  if (Decision?) is (Create) then
@enduml
``````plantuml
@startuml
partition "EO Head" #LightBlue
    :Assign for EO Head Review;
    :Review & Add Narrative;
    if (EO Head Decision?) is (Approve) then
@enduml
``````plantuml
@startuml
partition "EO Officer" #LightBlue
      :Triage & Route Case;
      if (Final EO Triage?) is (Send to IU) then
        :Set Target Unit (CSIS/ER/Legal);
@enduml
``````plantuml
@startuml
partition "Routing" #White
        if (Route to Department?) is (To CSIS) then
@endumlumll
``````plantuml
@startuml
partition "CSIS Intake Manager" #LightGray
          #palegreen:CSIS Intake (from EO);
          :CSIS: Initial Triage;
          if (Manager Decision?) is (Assign to Analyst) then
@enduml
``````plantuml
@startuml
partition "CSIS Intake Analyst" #LightGray
            :CSIS: Detailed Analysis;
            if (Analyst Decision?) is (Escalate) then
@enduml
``````plantuml
@startuml
partition "CSIS Intake Manager" #LightGray
              :CSIS: Final Approval;
              if (Approval Decision?) is (Approve) then
                :CSIS: Assign to Inv. Mgr;
@enduml
``````plantuml
@startuml
partition "Investigation Manager" #AliceBlue
                #lightblue:**Unified Investigation Handoff**;
                :Review & Prepare Assignment;
                if (Assign or Send Back?) is (Assign Investigator) then
@enduml
``````plantuml
@startuml
partition "Investigator" #AliceBlue
                  :Accept/Reject Assignment;
                  if (Acceptance Decision?) is (Active Investigation) then
                    :Create Investigation Plan;
                    stop
                  else (Conflict / Unassign)
                    ' This is the Investigator -> Manager feedback loop
                    --> [task_inv_manager_review]
                  endif
@enduml
``````plantuml
@startuml
partition "Investigation Manager" #AliceBlue
                else (Send Back)
                  ' This is the Manager self-correction loop
                  --> [task_inv_manager_review]
                endif
@enduml
``````plantuml
@startuml
partition "CSIS Intake Manager" #LightGray
              else (Reject)
                ' This is the Manager -> Analyst feedback loop
                --> [task_csis_analyst_review]
              endif
@enduml
``````plantuml
@startuml
partition "CSIS Intake Analyst" #LightGray
            else (Retain for Intelligence)
              :Retain for Intelligence;
              stop
            endif
@enduml
``````plantuml
@startuml
partition "CSIS Intake Manager" #LightGray
          else (Send Back to EO)
            --> [task_eo_officerReview]
          else (Send to Other IU)
            --> [gateway_routeToDepartment]
          else (Close)
            stop
          endif
@enduml
``````plantuml
@startuml
partition "Routing" #White
        else (To ER)
@enduml
``````plantuml
@startuml
partition "ER Intake Analyst" #White
          #lightyellow:ER Intake (from EO);
          :ER: Intake Review;
          if (ER Intake Decision?) is (Assign to Inv. Mgr) then
            :ER: Assign to Inv. Mgr;
            --> [task_inv_manager_review]
          else (Assign to HR)
            :ER: Assign to HR;
            stop
          else (Send Back to EO)
            --> [task_eo_officerReview]
          endif
@enduml
``````plantuml
@startuml
partition "Routing" #White
        else (To Legal)
@enduml
``````plantuml
@startuml
partition "Legal Intake Analyst" #White
          #lightpink:Legal Intake (from EO);
          :Legal: Intake Review;
          if (Legal Intake Decision?) is (Assign to Inv. Mgr) then
            :Legal: Assign to Inv. Mgr;
            --> [task_inv_manager_review]
          else (Send Back to EO)
            --> [task_eo_officerReview]
          endif
        endif
@enduml
``````plantuml
@startuml
partition "EO Officer" #LightBlue
      else (Save & Close)
        stop
      else (Send Back to Intake)
        --> [task_eo_createCase]
      endif
@enduml
``````plantuml
@startuml
partition "EO Head" #LightBlue
    else (Reject)
      --> [task_eo_headAssign]
    endif
@enduml``````plantuml
@startuml
partition "EO Intake Analyst" #LightBlue
  else (Cancel)
    stop
  endif
@enduml
``````plantuml
@startuml
' --- CSIS Self-Initiated Path ---
partition "CSIS Intake Manager" #LightGray
  start
  note right: Corresponds to **startCsisCase** event.
  if (Initiator Role?) is (Manager) then
    #palegreen:CSIS Self-Initiated (Manager-led);
    :Create Case (Manager);
    if (Manager Initial Triage?) is (Assign Analyst) then
@enduml
``````plantuml
@startuml
partition "CSIS Intake Analyst" #LightGray
      :Vet Case for Investigation;
      if (Analyst Recommendation?) is (Recommend) then
@enduml
``````plantuml
@startuml
partition "CSIS Intake Manager" #LightGray
        :Manager Final Approval;
        if (Final Decision?) is (Approve) then
          --> "Unified Investigation Handoff"
        else (Reject)
          --> [task_csis_analyst_vetting]
        endif
@enduml
``````plantuml
@startuml
partition "CSIS Intake Analyst" #LightGray
      else (Reject)
        --> [task_csis_analyst_vetting]
      endif
@enduml
``````plantuml
@startuml
partition "CSIS Intake Manager" #LightGray
    else (Fast Track to Investigation)
      --> "Unified Investigation Handoff"
    else (Retain for Intelligence)
      stop
    endif
  else (Analyst)
@enduml
``````plantuml
@startuml
partition "CSIS Intake Analyst" #LightGray
    #palegreen:CSIS Self-Initiated (Analyst-led);
    :Create Case (Analyst);
    if (Analyst Initial Triage?) is (Assign to Manager) then
@enduml
``````plantuml
@startuml
partition "CSIS Intake Manager" #LightGray
      :Review Analyst-Created Case;
      if (Manager Approval?) is (Approve) then
        --> "Unified Investigation Handoff"
      else (Reject)
        --> [task_csis_analyst_create]
      endif
@enduml
``````plantuml
@startuml
partition "CSIS Intake Analyst" #LightGray
    else (Retain for Intelligence)
      stop
    endif
  endif
@enduml
4. Detailed Step-by-Step Flow Description
This section provides a narrative explanation of the process depicted in the PlantUML diagram.

Phase 1: Case Origination
A case can enter the workflow through two distinct starting points:

Path A: EO-Initiated Intake (Externally Reported Cases)

Creation (EO Intake Analyst): An analyst receives an external report and creates a case, filling in all required initial information.
Validation (EO Head): The case is assigned to an EO Head who performs a high-level review. They can either Approve it for further triage or Reject it back to their own queue for re-evaluation.
Triage (EO Officer): An approved case goes to an EO Officer who makes the critical routing decision:
Send to IU: The case is valid and requires investigation. The Officer sets the targetUnit (CSIS, ER, or Legal) and sends it to the central routing hub.
Save & Close: The case is valid but requires no further action.
Send Back to Intake: The case is missing critical information and is sent back to the original creator.
Path B: CSIS Self-Initiated Intake

Start: A CSIS user initiates a case. The system checks their role.
Scenario 1 (Manager-led):
An Intake Manager creates the case and can either fast-track it directly to the Investigation Unit, close it for intelligence, or assign it to an Analyst for vetting.
The Analyst reviews the case and recommends it for investigation or rejects it back for more work.
The Manager gives a final Approve before sending it to the Investigation Unit.
Scenario 2 (Analyst-led):
An Intake Analyst creates the case and must submit it to a Manager for review.
The Manager reviews the analyst-created case and either Approves it for investigation or Rejects it back to the analyst for correction.
Phase 2: Departmental-Specific Intake
After the EO Officer routes a case via the central hub, it arrives at the designated department for their specialized intake.

If routed to CSIS: The case goes through the detailed three-phase Manager -> Analyst -> Manager review process. Key outcomes are sending back to the EO, re-routing to another IU, closing for intelligence, or approving for investigation.
If routed to ER: An ER Intake Analyst reviews the case. They can send it back to the EO, approve it for investigation, or use their unique option to Assign to HR, which closes the case from a workflow perspective.
If routed to Legal: A Legal Intake Analyst performs a streamlined review. They can either send it back to the EO or approve it for investigation.
Phase 3: Unified Investigation Handoff
This is the critical convergence point for the entire process. A case reaches this phase after it has been formally approved by CSIS, ER, or Legal.

Assignment Prep (Investigation Manager): An Investigation Manager receives the approved case. They perform a final review and prepare to assign it to a specific investigator. They can loop the case back to themselves if more information is needed before assignment.
Assignment Acceptance (Investigator): The assigned Investigator receives the case. This is a critical acceptance gate.
Accept: The Investigator accepts the assignment by selecting "Active Investigation".
Reject (Conflict/Unassign): If the Investigator has a conflict of interest or other issues, they reject the assignment, which sends the case back to the Investigation Manager to be reassigned to someone else.
Investigation Kick-off: Once an Investigator accepts the case, their first official task is to Create Investigation Plan. This marks the end of the intake and assignment process and the beginning of the formal investigation. The workflow, as defined here, concludes at this point.
5. Implementation Details for Developers
Process Variables
These variables are critical for controlling the flow through gateways. Forms associated with User Tasks must set these variables based on user actions.

Variable Name	Set By Task / Step	Expected Values	Purpose
creationAction	task_eo_createCase	CREATE, CANCEL	Controls initial EO case submission.
eoHeadAction	task_eo_headReview	APPROVE, REJECT	Controls EO Head's decision.
eoOfficerAction	task_eo_officerReview	SEND_TO_IU, SAVE_AND_CLOSE, SEND_BACK	Controls EO Officer's main triage decision.
targetUnit	task_eo_officerReview	CSIS, ER, LEGAL	Used with SEND_TO_IU to direct routing.
initiatorRole	(System) startCsisCase	MANAGER, ANALYST	Determines which CSIS-initiated scenario to run.
csisManagerAction1	task_csis_manager_review_1	ASSIGN, SEND_BACK, REROUTE, CLOSE	Controls CSIS Manager's first decision (EO path).
csisAnalystAction	task_csis_analyst_review	ESCALATE, RETAIN_INTEL	Controls CSIS Analyst's decision (EO path).
csisManagerAction2	task_csis_manager_review_2	APPROVE, REJECT	Controls CSIS Manager's final approval (EO path).
erIntakeAction	task_er_intake_review	ASSIGN_MANAGER, ASSIGN_HR, SEND_BACK	Controls ER Intake Analyst's decision.
legalIntakeAction	task_legal_intake_review	ASSIGN_MANAGER, SEND_BACK	Controls Legal Intake Analyst's decision.
invManagerAction	task_inv_manager_review	ASSIGN, SEND_BACK	Controls Investigation Manager's assignment decision.
investigatorAction	task_inv_investigator_review	ACCEPT, REJECT	Controls Investigator's case acceptance.
Role Management (RBAC)
User tasks are assigned to candidate groups. The application's security layer must map users to these groups.

GROUP_EO_INTAKE_ANALYST
GROUP_EO_HEAD
GROUP_EO_OFFICER
GROUP_CSIS_INTAKE_MANAGER
GROUP_CSIS_INTAKE_ANALYST
GROUP_ER_INTAKE_ANALYST
GROUP_LEGAL_INTAKE_ANALYST
GROUP_INVESTIGATION_MANAGER
GROUP_INVESTIGATOR