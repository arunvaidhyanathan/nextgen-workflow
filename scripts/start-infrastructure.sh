#!/bin/bash

# Start Infrastructure Services Script
# This script starts PostgreSQL and Cerbos services for NextGen Workflow

echo "Starting NextGen Workflow Infrastructure Services..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Error: Docker is not running. Please start Docker Desktop first."
    exit 1
fi

# Navigate to the project directory
cd "$(dirname "$0")/.." || exit

# Create necessary directories
echo "Creating necessary directories..."
mkdir -p cerbos/policies
mkdir -p database

# Copy entitlements schema to database directory if it doesn't exist
if [ ! -f database/entitlements-schema.sql ]; then
    echo "Setting up database schema..."
    # Schema is already created in database/entitlements-schema.sql
fi

# Start the infrastructure services
echo "Starting PostgreSQL and Cerbos containers..."
docker-compose -f docker-compose-infrastructure.yml up -d postgres cerbos

# Wait for services to be healthy
echo "Waiting for services to be ready..."

# Wait for PostgreSQL
until docker exec nextgen-postgres pg_isready -U postgres -d nextgen_workflow > /dev/null 2>&1; do
    echo "Waiting for PostgreSQL to be ready..."
    sleep 2
done
echo "PostgreSQL is ready!"

# Wait for Cerbos
until curl -s http://localhost:3593/_cerbos/health > /dev/null 2>&1; do
    echo "Waiting for Cerbos to be ready..."
    sleep 2
done
echo "Cerbos is ready!"

# Execute the init script
echo "Initializing database with users and schemas..."
docker exec -i nextgen-postgres psql -U postgres -d nextgen_workflow < scripts/init-db.sql

echo ""
echo "Infrastructure services started successfully!"
echo ""
echo "Services running:"
echo "- PostgreSQL: localhost:5432"
echo "  - Database: nextgen_workflow"
echo "  - Admin user: postgres/password"
echo "  - Service users: entitlement_user, flowable_user, onecms_user (all with password: 'password')"
echo "  - Schemas created: entitlements, flowable, onecms"
echo ""
echo "- Cerbos: "
echo "  - gRPC: localhost:3592"
echo "  - HTTP: localhost:3593"
echo "  - Admin: http://localhost:3593/"
echo ""
echo "Note: All database tables will be created by Liquibase migrations when services start"
echo ""
echo "To stop services: docker-compose -f docker-compose-infrastructure.yml down"
echo "To view logs: docker-compose -f docker-compose-infrastructure.yml logs -f"