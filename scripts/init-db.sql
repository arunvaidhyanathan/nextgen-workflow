-- Initialize NextGen Workflow Database
-- This script creates only the necessary users and schemas
-- All table creation will be handled by Liquibase migrations

-- Create database users for each microservice
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'entitlement_user') THEN
        CREATE USER entitlement_user WITH PASSWORD 'password';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'flowable_user') THEN
        CREATE USER flowable_user WITH PASSWORD 'password';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = 'onecms_user') THEN
        CREATE USER onecms_user WITH PASSWORD 'password';
    END IF;
END
$$;

-- Create schemas for each microservice
CREATE SCHEMA IF NOT EXISTS entitlements;
CREATE SCHEMA IF NOT EXISTS flowable;
CREATE SCHEMA IF NOT EXISTS onecms;

-- Grant schema ownership and permissions
GRANT ALL ON SCHEMA entitlements TO entitlement_user;
GRANT ALL ON SCHEMA flowable TO flowable_user;
GRANT ALL ON SCHEMA onecms TO onecms_user;

-- Grant database-level permissions
GRANT CREATE ON DATABASE nextgen_workflow TO entitlement_user;
GRANT CREATE ON DATABASE nextgen_workflow TO flowable_user;
GRANT CREATE ON DATABASE nextgen_workflow TO onecms_user;

-- Set default privileges for entitlements schema
ALTER DEFAULT PRIVILEGES IN SCHEMA entitlements GRANT ALL ON TABLES TO entitlement_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA entitlements GRANT ALL ON SEQUENCES TO entitlement_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA entitlements GRANT ALL ON FUNCTIONS TO entitlement_user;

-- Set default privileges for flowable schema
ALTER DEFAULT PRIVILEGES IN SCHEMA flowable GRANT ALL ON TABLES TO flowable_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA flowable GRANT ALL ON SEQUENCES TO flowable_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA flowable GRANT ALL ON FUNCTIONS TO flowable_user;

-- Set default privileges for onecms schema
ALTER DEFAULT PRIVILEGES IN SCHEMA onecms GRANT ALL ON TABLES TO onecms_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA onecms GRANT ALL ON SEQUENCES TO onecms_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA onecms GRANT ALL ON FUNCTIONS TO onecms_user;

-- Allow services to read from each other's schemas (for cross-service queries)
GRANT USAGE ON SCHEMA entitlements TO flowable_user, onecms_user;
GRANT USAGE ON SCHEMA flowable TO entitlement_user, onecms_user;
GRANT USAGE ON SCHEMA onecms TO entitlement_user, flowable_user;

-- Grant SELECT permissions for cross-service reads
GRANT SELECT ON ALL TABLES IN SCHEMA entitlements TO flowable_user, onecms_user;
GRANT SELECT ON ALL TABLES IN SCHEMA flowable TO entitlement_user, onecms_user;
GRANT SELECT ON ALL TABLES IN SCHEMA onecms TO entitlement_user, flowable_user;

-- Create Liquibase tracking tables in each schema
-- These will track migration history for each service
CREATE TABLE IF NOT EXISTS entitlements.databasechangelog (
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    dateexecuted TIMESTAMP NOT NULL,
    orderexecuted INTEGER NOT NULL,
    exectype VARCHAR(10) NOT NULL,
    md5sum VARCHAR(35),
    description VARCHAR(255),
    comments VARCHAR(255),
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    deployment_id VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS entitlements.databasechangeloglock (
    id INTEGER NOT NULL,
    locked BOOLEAN NOT NULL,
    lockgranted TIMESTAMP,
    lockedby VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS flowable.databasechangelog (
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    dateexecuted TIMESTAMP NOT NULL,
    orderexecuted INTEGER NOT NULL,
    exectype VARCHAR(10) NOT NULL,
    md5sum VARCHAR(35),
    description VARCHAR(255),
    comments VARCHAR(255),
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    deployment_id VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS flowable.databasechangeloglock (
    id INTEGER NOT NULL,
    locked BOOLEAN NOT NULL,
    lockgranted TIMESTAMP,
    lockedby VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS onecms.databasechangelog (
    id VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    dateexecuted TIMESTAMP NOT NULL,
    orderexecuted INTEGER NOT NULL,
    exectype VARCHAR(10) NOT NULL,
    md5sum VARCHAR(35),
    description VARCHAR(255),
    comments VARCHAR(255),
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    deployment_id VARCHAR(10)
);

CREATE TABLE IF NOT EXISTS onecms.databasechangeloglock (
    id INTEGER NOT NULL,
    locked BOOLEAN NOT NULL,
    lockgranted TIMESTAMP,
    lockedby VARCHAR(255),
    PRIMARY KEY (id)
);

-- Grant permissions on Liquibase tables
GRANT ALL ON TABLE entitlements.databasechangelog TO entitlement_user;
GRANT ALL ON TABLE entitlements.databasechangeloglock TO entitlement_user;
GRANT ALL ON TABLE flowable.databasechangelog TO flowable_user;
GRANT ALL ON TABLE flowable.databasechangeloglock TO flowable_user;
GRANT ALL ON TABLE onecms.databasechangelog TO onecms_user;
GRANT ALL ON TABLE onecms.databasechangeloglock TO onecms_user;