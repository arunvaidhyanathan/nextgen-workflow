# Technical Specification: LLM-Driven Case Creation and Summarization from Email

This document provides the complete technical specification for implementing the new email intake and LLM extraction pipeline within the OneCMS ecosystem. The implementation requires modifications to the existing OneCMS Spring Boot Microservice and the development of a new FastAPI LLM Gateway Service.

## 1. Architectural Overview and Data Flow

The solution involves an asynchronous intake process to prevent user-facing timeouts while handling heavy LLM loads.

### Process Flow

1. **Ingestion** (`/createcaseupload`): The OneCMS API receives the .eml file, saves the raw data to `case_creation_email` with status `PENDING`, and immediately responds `202 Accepted`.

2. **Orchestration** (Async): A background process (Orchestrator/Async Task) picks up the `PENDING` record and calls the FastAPI LLM Gateway.

3. **Extraction** (FastAPI): The FastAPI service calls the Google Gemini API using the defined JSON schema to extract structured data (Narrative, Entities, Allegations).

4. **Case Creation**: Upon successful extraction, the Orchestrator calls the core Case/Workflow Service to persist the data into the main OneCMS tables (`cases`, `case_entities`, `allegations`, `case_narratives`) and creates the initial snapshot in `case_summaries`.

5. **Retrieval** (`/casesummary`): A synchronous API allows users to fetch a specific historical snapshot of the case summary from the `case_summaries` table.

## 1.5. Database Schema: New Tables

The following two tables are added to the `onecms` schema to support the asynchronous intake and summary tracking.

### Table: case_creation_email (LLM Intake Queue)

This table tracks the status of the LLM extraction job for each uploaded email.

| Column Name | Data Type (PostgreSQL) | Nullable | Constraints & Defaults | Index |
|-------------|------------------------|----------|------------------------|-------|
| `call_id` | `BIGINT` | NO | Primary Key (via `create_case_email_seq` sequence) | |
| `status` | `VARCHAR(50)` | NO | None | `idx_case_creation_email_status` |
| `sender_email` | `VARCHAR(255)` | NO | None | `idx_case_creation_email_sender` |
| `sender_name` | `VARCHAR(255)` | YES | None | |
| `subject` | `TEXT` | YES | None | |
| `body_text` | `TEXT` | YES | None | |
| `raw_email_attachment` | `BYTEA` | NO | None | |
| `employee_id` | `VARCHAR(50)` | YES | None | |
| `case_id` | `BIGINT` | YES | Foreign Key to `onecms.cases.id` (ON DELETE SET NULL) | `idx_case_creation_email_case_id` |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | NO | Default: `now()` | |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | YES | None | |

### Table: case_summaries (Historical Summary Snapshots)

This table stores denormalized summary text at key transition points in the workflow.

| Column Name | Data Type (PostgreSQL) | Nullable | Constraints & Defaults | Index |
|-------------|------------------------|----------|------------------------|-------|
| `case_summaries_id` | `BIGINT` | NO | Primary Key (via `case_summaries_seq` sequence) | |
| `case_id` | `BIGINT` | NO | Foreign Key to `onecms.cases.id` (ON DELETE CASCADE), Unique | `idx_case_summaries_case_id` (Unique) |
| `status_id` | `VARCHAR(255)` | NO | Workflow step ID (e.g., `LLM_INITIAL`, `EO_INTAKE_ANALYST`) | `idx_case_summaries_status_id` |
| `summary_text` | `TEXT` | YES | None | |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | NO | Default: `now()` | |

## 2. OneCMS Spring Boot Service Specification (API Layer)

The OneCMS Microservice requires two new endpoints to manage the flow.

### 2.1. Endpoint 1: Email Upload and Asynchronous Case Creation

This endpoint handles file upload, initial database record creation, and asynchronous triggering of the LLM process.

**Target Table for Initial Data**: `case_creation_email`

| Component | Detail |
|-----------|--------|
| Endpoint | `/api/v1/emails/createcaseupload` |
| Method | `POST` |
| Request Body Type | `multipart/form-data` |
| Request Body | `file` (Type: file, Content: .eml or .msg file) |
| Logic | 1. Save raw file content, sender, subject, and body into `case_creation_email`. 2. Set status to `PENDING`. 3. Trigger the asynchronous Orchestrator task. |
| Successful Response | `202 Accepted`<br/>Body: `{ "callId": 12345, "status": "PROCESSING", "message": "Email intake started asynchronously." }` |
| Failure Response | `400 Bad Request` (Missing file) or `500 Internal Server Error` (DB/Storage error) |

### 2.2. Endpoint 2: Case Summary Retrieval (Historical Snapshot)

This synchronous endpoint allows a user to retrieve the summary associated with a specific workflow step.

**Target Table for Retrieval**: `case_summaries`

| Component | Detail |
|-----------|--------|
| Endpoint | `/api/v1/emails/casesummary` |
| Method | `GET` |
| Parameter | `caseID` (Type: Long, Query Parameter, Required)<br/>`stepName` (Type: String, Query Parameter, Required - e.g., `LLM_INITIAL`, `EO_INTAKE_ANALYST`) |
| Logic | 1. Authenticate user. 2. Query `case_summaries` where `case_id = :caseID` AND `status_id = :stepName`. 3. Enforce access control. |
| Successful Response | `200 OK`<br/>Body: `{ "caseSummaryId": 55, "caseId": 12345, "statusId": "EO_INTAKE_ANALYST", "summaryText": "The analyst review confirms the facts: Jane Doe is the subject...", "createdAt": "2025-10-25T14:30:00Z" }` |
| Failure Response | `404 Not Found` (No summary record exists for the given step) or `403 Forbidden` (Access denied) |

## 3. OpenAPI 3.0 Specification (OneCMS Microservice)

The following YAML block describes the two new endpoints for integration into the existing OneCMS OpenAPI specification:

```yaml
paths:
  /api/v1/emails/createcaseupload:
    post:
      tags:
        - Case Intake
      summary: Upload email file and asynchronously create case via LLM extraction.
      operationId: createCaseFromEmail
      requestBody:
        content:
          multipart/form-data:
            schema:
              type: object
              properties:
                file:
                  type: string
                  format: binary
                  description: The .eml or .msg file to be processed.
        required: true
      responses:
        '202':
          description: Accepted - Email file saved and LLM processing job triggered.
          content:
            application/json:
              schema:
                type: object
                properties:
                  callId:
                    type: integer
                    format: int64
                    description: Unique ID for tracking the asynchronous job in case_creation_email.
                  status:
                    type: string
                    example: PROCESSING
        '400':
          description: Bad Request (e.g., missing file).

  /api/v1/emails/casesummary:
    get:
      tags:
        - Case Summary
      summary: Retrieve a historical case summary snapshot for a specific workflow step.
      operationId: getCaseSummaryByStep
      parameters:
        - name: caseID
          in: query
          required: true
          schema:
            type: integer
            format: int64
          description: The unique identifier of the case.
        - name: stepName
          in: query
          required: true
          schema:
            type: string
            enum: [LLM_INITIAL, EO_INTAKE_ANALYST, EO_INTAKE_DIRECTOR]
          description: The workflow step whose summary snapshot is requested.
      responses:
        '200':
          description: OK - Returns the case summary record.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CaseSummaryRecord'
        '404':
          description: Not Found - No summary exists for the given step.

components:
  schemas:
    CaseSummaryRecord:
      type: object
      properties:
        caseSummaryId:
          type: integer
          format: int64
        caseId:
          type: integer
          format: int64
        statusId:
          type: string
        summaryText:
          type: string
        createdAt:
          type: string
          format: date-time
```

## 4. FastAPI LLM Gateway Service Specification

This service is solely responsible for prompt engineering and calling the external LLM API to get the required structured output.

### 4.1. Endpoint 3: LLM Extraction

This endpoint is called by the Spring Boot Orchestrator to perform the heavy lifting of structured data extraction.

| Component | Detail |
|-----------|--------|
| Endpoint | `/api/v1/extract_email_info` |
| Method | `POST` |
| Request Body Type | `application/json` |
| Request Schema | Input Pydantic Model: `LlmExtractionRequest` |
| Input Fields | `call_id` (Long), `email_body` (String) |
| LLM Agent Task | 1. Construct the detailed prompt, instructing the LLM to act as a Compliance Analyst. 2. Pass the `email_body` and the required JSON schema (see Section 5) to the Gemini API. |
| Successful Response | `200 OK`<br/>Body: JSON strictly adhering to the `LLMCaseExtraction` schema. |
| Failure Response | `500 Internal Server Error` (Forward LLM errors, timeouts, or JSON parsing failures). |

## 5. LLM Prompt Engineering & Data Modeling

The prompt must leverage the context of the OneCMS database schema to ensure the LLM output is immediately compatible with the persistence layer.

### 5.1. Target Schema (for Gemini API)

The LLM MUST generate a response that strictly adheres to the following Pydantic/JSON Schema (already defined in `llm_output_schema.json`):

```json
{
  "title": "LLMCaseExtraction",
  "type": "object",
  "properties": {
    "narrative_text": { "..." },
    "allegations": { "..." },
    "entities": { "..." }
  },
  "required": ["narrative_text", "allegations", "entities"]
}
```

### 5.2. Core LLM Agent Instructions

The LLM Agent implementation in the FastAPI service must follow these steps for the `POST /api/v1/extract_email_info` handler:

1. **System Instruction (Role)**: "You are a highly experienced and meticulous Ethics and Compliance Analyst at a major financial institution. Your task is to analyze raw email content and extract all relevant case information into a highly precise, predefined JSON format. Do not include any conversational text, preamble, or markdown formatting outside of the JSON object."

2. **User Prompt (Task)**: "Analyze the following email content. Identify all Entities (People, Organizations, Locations) and their role in the case (Complainant, Subject, Witness). Identify all Allegations and their type (Misconduct, Conflict of Interest, Harassment, Other). Finally, create a single, neutral, and factual narrative_text summary of the incident. The output must be only a valid JSON object matching the provided schema."

3. **Data Persistence Mapping Context**: The LLM's output directly populates the following OneCMS database tables:
   - `narrative_text` → `case_narratives.content` (Initial record with type 'LLM_SUMMARY')
   - `allegations` array → `allegations` table
   - `entities` array → `case_entities` table

## 6. OpenAPI 3.0 Specification (FastAPI LLM Gateway)

The following YAML block describes the LLM Gateway endpoint:

```yaml
paths:
  /api/v1/extract_email_info:
    post:
      tags:
        - LLM Extraction
      summary: Executes the LLM to extract structured case data from raw email text.
      operationId: extractEmailInfo
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                call_id:
                  type: integer
                  format: int64
                  description: Tracking ID from OneCMS to correlate the request.
                email_body:
                  type: string
                  description: The plain text content of the email.
              required:
                - call_id
                - email_body
      responses:
        '200':
          description: Successful extraction, returns structured case data.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LLMCaseExtraction'
        '500':
          description: Internal Server Error (LLM failure, API issue, or JSON parsing error).

components:
  schemas:
    # This schema MUST mirror the Pydantic model used by the FastAPI service
    LLMCaseExtraction:
      type: object
      properties:
        narrative_text:
          type: string
          description: The concise, single-paragraph summary of the case facts.
        allegations:
          type: array
          description: List of core rule violations or ethical concerns identified.
          items:
            type: object
            properties:
              type:
                type: string
                enum: [Misconduct, Conflict of Interest, Harassment, Other]
              description:
                type: string
            required:
              - type
              - description
        entities:
          type: array
          description: People or Organizations mentioned, relevant to the case.
          items:
            type: object
            properties:
              category:
                type: string
                enum: [Person, Organization, Location]
              name:
                type: string
              role:
                type: string
                description: The person's role in the case (Complainant, Subject, Witness).
            required:
              - category
              - name
              - role
      required:
        - narrative_text
        - allegations
        - entities
```