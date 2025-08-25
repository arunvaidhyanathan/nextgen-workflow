# NextGen Workflow - Multi-Tenancy Strategy

## Overview

Multi-tenancy implementation to support multiple organizations or departments within a single NextGen Workflow deployment while ensuring complete data isolation and security.

## Multi-Tenancy Architecture

### 1. **Shared Schema with Tenant Isolation**
- Single database with tenant ID column in all tables
- Row-level security for data isolation
- Shared application infrastructure
- Cost-effective for multiple tenants

### 2. **Tenant Identification Strategy**
```
Tenant Resolution Hierarchy:
1. HTTP Header: X-Tenant-Id (primary)
2. JWT Token: tenant claim (secondary)
3. User context: user.tenantId (fallback)
4. Subdomain: {tenant}.nextgen-workflow.com (future)
```

## Database Design

### Tenant-Aware Tables
All core tables will include tenant isolation:

```sql
-- Core business tables
ALTER TABLE cases ADD COLUMN tenant_id VARCHAR(50) NOT NULL;
ALTER TABLE allegations ADD COLUMN tenant_id VARCHAR(50) NOT NULL;
ALTER TABLE case_entities ADD COLUMN tenant_id VARCHAR(50) NOT NULL;

-- Workflow tables  
ALTER TABLE workflow_metadata ADD COLUMN tenant_id VARCHAR(50) NOT NULL;
ALTER TABLE queue_tasks ADD COLUMN tenant_id VARCHAR(50) NOT NULL;

-- User management
ALTER TABLE users ADD COLUMN tenant_id VARCHAR(50) NOT NULL;
ALTER TABLE user_roles ADD COLUMN tenant_id VARCHAR(50) NOT NULL;
```

### Tenant Configuration Table
```sql
CREATE TABLE tenants (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    subdomain VARCHAR(100) UNIQUE,
    status ENUM('ACTIVE', 'SUSPENDED', 'DELETED') DEFAULT 'ACTIVE',
    max_users INTEGER DEFAULT 100,
    max_cases INTEGER DEFAULT 1000,
    storage_quota_gb INTEGER DEFAULT 10,
    feature_flags JSON,
    configuration JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## Implementation Components

### 1. **Tenant Context Management**

```java
@Component
public class TenantContext {
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
```

### 2. **Tenant Resolution Filter**

```java
@Component
public class TenantResolutionFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        
        String tenantId = resolveTenantId(request);
        TenantContext.setCurrentTenant(tenantId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
```

### 3. **JPA Tenant-Aware Queries**

```java
@Entity
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Case {
    @Column(name = "tenant_id")
    private String tenantId;
    
    // Auto-populate tenant on save
    @PrePersist
    public void prePersist() {
        if (tenantId == null) {
            tenantId = TenantContext.getCurrentTenant();
        }
    }
}
```

## Service Layer Changes

### 1. **Repository Pattern**
```java
public interface CaseRepository extends JpaRepository<Case, Long> {
    
    // Tenant-aware queries
    @Query("SELECT c FROM Case c WHERE c.tenantId = :#{T(com.workflow.tenant.TenantContext).getCurrentTenant()}")
    List<Case> findAllForCurrentTenant();
    
    @Query("SELECT c FROM Case c WHERE c.id = :id AND c.tenantId = :#{T(com.workflow.tenant.TenantContext).getCurrentTenant()}")
    Optional<Case> findByIdForCurrentTenant(Long id);
}
```

### 2. **Service Layer**
```java
@Service
public class CaseService {
    
    @Transactional
    public Case createCase(CreateCaseRequest request) {
        // Tenant context is automatically applied
        Case case = new Case();
        case.setTitle(request.getTitle());
        // tenantId auto-populated in @PrePersist
        
        return caseRepository.save(case);
    }
}
```

## Security Enhancements

### 1. **Cerbos Multi-Tenant Policies**
```yaml
# policies/tenants/tenant-isolation.yaml
apiVersion: api.cerbos.dev/v1
resourcePolicy:
  version: "1_0"
  resource: "case"
  rules:
    - actions: ['*']
      effect: ALLOW
      roles: ['*']
      condition:
        match:
          expr: 'R.tenant_id == P.tenant_id'
```

### 2. **Cross-Tenant Access Prevention**
```java
@Component
public class TenantSecurityAspect {
    
    @Around("@annotation(TenantSecured)")
    public Object checkTenantAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String currentTenant = TenantContext.getCurrentTenant();
        
        if (currentTenant == null) {
            throw new UnauthorizedException("No tenant context");
        }
        
        return joinPoint.proceed();
    }
}
```

## Configuration Management

### 1. **Tenant-Specific Configuration**
```java
@Component
public class TenantConfigurationService {
    
    public TenantConfiguration getConfiguration(String tenantId) {
        return tenantRepository.findById(tenantId)
            .map(tenant -> tenant.getConfiguration())
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }
    
    public WorkflowConfiguration getWorkflowConfig(String tenantId) {
        TenantConfiguration config = getConfiguration(tenantId);
        return config.getWorkflowConfiguration();
    }
}
```

### 2. **Feature Flags per Tenant**
```java
@Component
public class TenantFeatureService {
    
    public boolean isFeatureEnabled(String tenantId, String feature) {
        return tenantConfigurationService.getConfiguration(tenantId)
            .getFeatureFlags()
            .getOrDefault(feature, false);
    }
}
```

## Workflow Engine Multi-Tenancy

### 1. **Flowable Tenant Support**
```java
@Configuration
public class FlowableTenantConfiguration {
    
    @Bean
    public TenantInfoHolder tenantInfoHolder() {
        return () -> TenantContext.getCurrentTenant();
    }
    
    @Bean
    public ProcessEngine processEngine() {
        ProcessEngineConfiguration config = ProcessEngineConfiguration
            .createStandaloneProcessEngineConfiguration()
            .setDatabaseSchemaUpdate("true")
            .setTenantInfoHolder(tenantInfoHolder());
            
        return config.buildProcessEngine();
    }
}
```

### 2. **Tenant-Scoped Process Definitions**
```java
@Service
public class TenantWorkflowService {
    
    public void deployProcessForTenant(String tenantId, String bpmnXml) {
        repositoryService.createDeployment()
            .tenantId(tenantId)
            .addString("process.bpmn20.xml", bpmnXml)
            .deploy();
    }
    
    public List<ProcessInstance> getProcessInstancesForTenant(String tenantId) {
        return runtimeService.createProcessInstanceQuery()
            .processInstanceTenantId(tenantId)
            .list();
    }
}
```

## API Gateway Enhancements

### 1. **Tenant Routing**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: tenant-aware-route
          uri: lb://onecms-service
          predicates:
            - Path=/api/tenant/{tenantId}/**
          filters:
            - RewritePath=/api/tenant/(?<tenantId>.*)/(?<remaining>.*), /api/$\{remaining}
            - AddRequestHeader=X-Tenant-Id, {tenantId}
```

### 2. **Rate Limiting per Tenant**
```java
@Component
public class TenantRateLimitingFilter implements GatewayFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
        
        return rateLimitingService.checkLimit(tenantId)
            .flatMap(allowed -> {
                if (!allowed) {
                    return respondWithRateLimit(exchange);
                }
                return chain.filter(exchange);
            });
    }
}
```

## Monitoring and Observability

### 1. **Tenant-Specific Metrics**
```java
@Component
public class TenantMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    public void recordCaseCreation(String tenantId) {
        Counter.builder("cases.created")
            .tag("tenant", tenantId)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordWorkflowExecution(String tenantId, String processKey) {
        Timer.builder("workflow.execution")
            .tag("tenant", tenantId)
            .tag("process", processKey)
            .register(meterRegistry)
            .record(duration);
    }
}
```

### 2. **Tenant Dashboards**
- Separate Grafana dashboards per tenant
- Tenant-filtered metrics and logs
- Resource usage monitoring per tenant

## Data Migration Strategy

### Phase 1: Schema Updates
```sql
-- Add tenant_id columns with default value for existing data
ALTER TABLE cases ADD COLUMN tenant_id VARCHAR(50) DEFAULT 'default';
ALTER TABLE allegations ADD COLUMN tenant_id VARCHAR(50) DEFAULT 'default';

-- Update existing data to use default tenant
UPDATE cases SET tenant_id = 'default' WHERE tenant_id IS NULL;
UPDATE allegations SET tenant_id = 'default' WHERE tenant_id IS NULL;

-- Make tenant_id NOT NULL after data migration
ALTER TABLE cases MODIFY tenant_id VARCHAR(50) NOT NULL;
ALTER TABLE allegations MODIFY tenant_id VARCHAR(50) NOT NULL;
```

### Phase 2: Index Optimization
```sql
-- Add composite indexes for tenant-aware queries
CREATE INDEX idx_cases_tenant_status ON cases (tenant_id, status);
CREATE INDEX idx_workflow_metadata_tenant ON workflow_metadata (tenant_id, process_definition_key);
CREATE INDEX idx_queue_tasks_tenant ON queue_tasks (tenant_id, queue_name, status);
```

## Tenant Onboarding

### 1. **Automated Tenant Provisioning**
```java
@Service
public class TenantProvisioningService {
    
    @Transactional
    public Tenant createTenant(CreateTenantRequest request) {
        // 1. Create tenant record
        Tenant tenant = new Tenant();
        tenant.setId(request.getTenantId());
        tenant.setName(request.getName());
        tenant.setConfiguration(getDefaultConfiguration());
        
        // 2. Create default admin user
        User adminUser = createAdminUser(tenant.getId(), request.getAdminEmail());
        
        // 3. Deploy default workflows
        deployDefaultWorkflows(tenant.getId());
        
        // 4. Setup monitoring dashboards
        setupTenantDashboards(tenant.getId());
        
        return tenantRepository.save(tenant);
    }
}
```

### 2. **Tenant Resource Limits**
```java
@Component
public class TenantResourceManager {
    
    public void enforceResourceLimits(String tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new TenantNotFoundException(tenantId));
            
        // Check user limits
        long userCount = userRepository.countByTenantId(tenantId);
        if (userCount >= tenant.getMaxUsers()) {
            throw new ResourceLimitExceededException("User limit exceeded");
        }
        
        // Check case limits
        long caseCount = caseRepository.countByTenantId(tenantId);
        if (caseCount >= tenant.getMaxCases()) {
            throw new ResourceLimitExceededException("Case limit exceeded");
        }
    }
}
```

## Benefits

1. **Cost Efficiency**: Shared infrastructure reduces operational costs
2. **Data Isolation**: Complete separation between tenants
3. **Scalability**: Easy horizontal scaling per tenant
4. **Feature Customization**: Tenant-specific feature flags
5. **Compliance**: Tenant-specific compliance configurations
6. **Monitoring**: Granular per-tenant observability

## Implementation Timeline

- **Phase 1** (Week 1): Core tenant context and database changes
- **Phase 2** (Week 2): Service layer multi-tenancy integration  
- **Phase 3** (Week 3): Workflow engine tenant support
- **Phase 4** (Week 4): API Gateway and security enhancements
- **Phase 5** (Week 5): Monitoring and tenant management UI