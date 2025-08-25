# NextGen Workflow Database Setup

This directory contains the centralized Liquibase database migration scripts for all microservices in the NextGen Workflow application.

## Prerequisites

- PostgreSQL 16.x
- Liquibase CLI (optional, if running outside of microservices)
- Database schemas already created

## Database Structure

The application uses three separate schemas:

1. **entitlements** - User management and authorization data
2. **flowable** - Workflow engine and task management  
3. **onecms** - Case management and business data

## Schema Creation

Before running Liquibase migrations, create the required schemas:

```sql
-- Connect to the workflow database as superuser
\c workflow

-- Create schemas
CREATE SCHEMA IF NOT EXISTS entitlements;
CREATE SCHEMA IF NOT EXISTS flowable;
CREATE SCHEMA IF NOT EXISTS onecms;

-- Grant permissions to service users
GRANT ALL ON SCHEMA entitlements TO entitlement_user;
GRANT ALL ON SCHEMA flowable TO flowable_user;
GRANT ALL ON SCHEMA onecms TO onecms_user;
```

## Running Migrations

### Option 1: Via Microservices (Recommended)

Each microservice will automatically run its own migrations on startup:
- entitlement-service → entitlements schema
- flowable-wrapper-v2 → flowable schema
- onecms-service → onecms schema

### Option 2: Centralized Execution

To run all migrations manually:

```bash
# Using Liquibase CLI
liquibase --changeLogFile=centralized-changelog-master.xml \
          --url=jdbc:postgresql://localhost:5432/workflow \
          --username=workflow_user \
          --password=workflow_pass \
          --classpath=/path/to/postgresql-driver.jar \
          update
```

## Changelog Structure

```
database/
├── centralized-changelog-master.xml    # Master changelog
├── entitlements/
│   └── 001-create-entitlement-tables.xml
├── flowable/
│   └── 001-create-workflow-tables.xml
└── onecms/
    ├── 001-consolidated-onecms-schema.xml
    └── 002-add-case-transitions-audit-table.xml
```

## Database Objects Created

### Entitlements Schema
- **Tables**: users, departments, user_departments, business_applications, business_app_roles, user_business_app_roles
- **Purpose**: User authentication and authorization

### Flowable Schema  
- **Tables**: workflow_metadata, queue_tasks (plus Flowable engine tables)
- **Purpose**: Workflow orchestration and task management

### OneCMS Schema
- **Tables**: cases, allegations, case_entities, case_narratives, case_comments, case_transitions, work_items
- **Reference Tables**: departments, case_types, countries_clusters, data_sources, escalation_methods
- **Purpose**: Case management and business operations

## Important Notes

1. **No Schema Creation**: These scripts do NOT create schemas. Schemas must exist before running migrations.
2. **Flowable Tables**: Flowable engine tables (ACT_*) are created automatically by Flowable on first startup.
3. **Order Matters**: Run migrations in the order specified in the master changelog.
4. **Seed Data**: OneCMS includes seed data for reference tables.

## Rollback

To rollback changes:

```bash
liquibase --changeLogFile=centralized-changelog-master.xml \
          --url=jdbc:postgresql://localhost:5432/workflow \
          --username=workflow_user \
          --password=workflow_pass \
          rollbackCount 1
```