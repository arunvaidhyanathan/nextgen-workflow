# NextGen Workflow Docker Compose Setup

## Directory Structure

```
nextgen-workflow/
├── docker-compose-complete.yml     # Main Docker Compose file
├── postgres/
│   └── init.sql                   # PostgreSQL initialization script
├── cerbos/
│   ├── config/
│   │   └── config.yaml            # Cerbos server configuration
│   └── policies/
│       ├── derived_roles/
│       │   └── one-cms.yaml       # Derived roles definitions
│       └── resource_policies/     # Resource-specific policies
│           ├── case.yaml
│           ├── workflow.yaml
│           ├── task.yaml
│           └── queue.yaml
└── logs/                          # Shared logs directory (created automatically)
```

## Prerequisites

1. **Build all services first:**
   ```bash
   # From the root directory
   mvn clean package -DskipTests
   ```

2. **Ensure JAR files exist in target directories:**
   - `service-registry/target/service-registry-1.0.0.jar`
   - `api-gateway/target/api-gateway-1.0.0.jar`
   - `entitlement-service/target/entitlement-service-1.0.0.jar`
   - `flowable-wrapper-v2/target/flowable-wrapper-v2-1.0.0.jar`
   - `onecms-service/target/onecms-service-1.0.0.jar`

## Usage Commands

### Start All Services
```bash
# Start complete infrastructure
docker-compose -f docker-compose-complete.yml up -d

# View logs for all services
docker-compose -f docker-compose-complete.yml logs -f

# View logs for specific service
docker-compose -f docker-compose-complete.yml logs -f entitlement-service
```

### Include Optional Tools
```bash
# Start with PgAdmin included
docker-compose -f docker-compose-complete.yml --profile tools up -d
```

### Stop Services
```bash
# Stop all services
docker-compose -f docker-compose-complete.yml down

# Stop and remove volumes (clean slate)
docker-compose -f docker-compose-complete.yml down -v
```

## Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| Eureka Dashboard | http://localhost:8761 | Service Registry UI |
| API Gateway | http://localhost:8080 | Main entry point for all APIs |
| Entitlement Service | http://localhost:8081 | Direct access (dev only) |
| Flowable Service | http://localhost:8082 | Direct access (dev only) |
| OneCMS Service | http://localhost:8083 | Direct access (dev only) |
| Cerbos gRPC | localhost:3592 | Policy engine gRPC endpoint |
| Cerbos HTTP | http://localhost:3593 | Policy engine HTTP endpoint |
| PostgreSQL | localhost:5432 | Database |
| PgAdmin | http://localhost:5050 | Database UI (when using tools profile) |

## Test Credentials

### Database
- **Admin**: postgres / password
- **Service Users**: 
  - entitlement_user / password
  - flowable_user / password
  - onecms_user / password

### Application Users
- alice.intake / password123 - Intake Analyst
- edward.inv / password123 - Investigator
- sarah.legal / password123 - Legal Counsel
- mike.admin / password123 - System Administrator
- jane.manager / password123 - Investigation Manager
- tom.hr / password123 - HR Specialist

### PgAdmin (if enabled)
- admin@nextgen.local / admin

## Startup Sequence

Services start in this order based on health check dependencies:
1. PostgreSQL + Cerbos (parallel)
2. Service Registry (Eureka)
3. Entitlement Service
4. Flowable Service
5. OneCMS Service
6. API Gateway

## Configuration Files

### PostgreSQL Init Script (`postgres/init.sql`)
- Creates schemas: `entitlements`, `flowable`, `onecms`
- Creates database users with appropriate permissions
- Seeds test data for development

### Cerbos Configuration (`cerbos/config/config.yaml`)
- Configures Cerbos server ports
- Sets up file-based policy storage
- Configures policy watching for hot reload

### Cerbos Policies
- **Derived Roles** (`cerbos/policies/derived_roles/one-cms.yaml`): Dynamic role assignments
- **Resource Policies** (`cerbos/policies/resource_policies/*.yaml`): Resource-specific access rules

## Troubleshooting

### Service Won't Start
1. Check if JAR files exist in target directories
2. Check service logs: `docker-compose -f docker-compose-complete.yml logs [service-name]`
3. Ensure no port conflicts with running services

### Database Connection Issues
1. Ensure PostgreSQL is healthy: `docker-compose -f docker-compose-complete.yml ps postgres`
2. Check database logs: `docker-compose -f docker-compose-complete.yml logs postgres`
3. Verify schemas were created by checking init script execution

### Service Discovery Issues
1. Check Eureka dashboard at http://localhost:8761
2. Services should register within 30-60 seconds
3. Check service logs for registration errors

### Cerbos Policy Issues
1. Check Cerbos logs: `docker-compose -f docker-compose-complete.yml logs cerbos`
2. Verify policies are valid YAML
3. Check policy directory permissions

## Development Tips

1. **Hot Reload Policies**: Cerbos watches for policy file changes
2. **View Logs**: Use `docker-compose logs -f [service]` to tail logs
3. **Database Access**: Use PgAdmin or connect directly to PostgreSQL on port 5432
4. **API Testing**: All APIs accessible through Gateway at http://localhost:8080/api/*

## Production Considerations

1. Update passwords in `postgres/init.sql`
2. Use environment-specific configuration files
3. Enable TLS for Cerbos communication
4. Use proper volume mounts for production data
5. Configure proper resource limits
6. Set up monitoring and alerting