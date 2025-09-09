# Entitlement Service - Role Structure Changes TODO

## Overview
This document outlines the required changes to implement the new RoleDetails interface structure for the entitlement service. This represents a major architectural change from string-based roles to structured role definitions.

## Target RoleDetails Interface Structure
```typescript
export interface RoleDetails {
    /**
     * @example: "E0_L1_IN" - EO Intake Analyst
     * @example: "ER_L2_IU" - ER Investigator  
     * @example: "CSIS_L3_IU" - CSIS Manager
     */
    displayName: string;
    
    /**
     * E0 - Ethics Office
     * ER - Employee Relations
     * CSIS - Cyber Security Incident Specialist
     */
    function: string;
    
    /**
     * L1 - Analyst
     * L2 - Officer/Investigator
     * L3 - Manager/Head/Director
     */
    level: string;
    
    /**
     * IN - Intake
     * IU - Investigation
     */
    phase: string;
}
```

## Impact Analysis Summary

**EFFORT LEVEL: HIGH (8-10 weeks)**
**TEAM SIZE: 2-3 developers**
**RISK LEVEL: HIGH - Major architectural change affecting core authorization**

## Current State Analysis

### Current Role Structure
- **String-based roles**: `INTAKE_ANALYST`, `EO_OFFICER`, `GROUP_EO_HEAD`, etc.
- **Fixed mappings**: Hardcoded in multiple locations
- **Queue assignments**: Direct role-to-queue mappings
- **Cerbos integration**: 64+ references to GROUP_ patterns

### Role Transformation Examples
```
Current → Target
INTAKE_ANALYST → E0_L1_IN (function=E0, level=L1, phase=IN)
EO_OFFICER → E0_L2_OF (function=E0, level=L2, phase=OF)  
CSIS_MANAGER → CSIS_L3_IU (function=CSIS, level=L3, phase=IU)
INVESTIGATOR → ER_L2_IU (function=ER, level=L2, phase=IU)
```

## Required Changes by Component

### 1. Database Schema Changes - Medium Effort (2-3 weeks)

#### Affected Tables:
- `entitlements.business_app_roles`
- `entitlements.entitlement_domain_roles`

#### Schema Modifications:
```sql
-- Add new columns to business_app_roles
ALTER TABLE entitlements.business_app_roles 
ADD COLUMN role_function VARCHAR(10),
ADD COLUMN role_level VARCHAR(10), 
ADD COLUMN role_phase VARCHAR(10),
ADD COLUMN legacy_role_name VARCHAR(100); -- For migration support

-- Add constraints
ALTER TABLE entitlements.business_app_roles
ADD CONSTRAINT chk_role_function CHECK (role_function IN ('E0', 'ER', 'CSIS')),
ADD CONSTRAINT chk_role_level CHECK (role_level IN ('L1', 'L2', 'L3')),
ADD CONSTRAINT chk_role_phase CHECK (role_phase IN ('IN', 'IU', 'OF', 'RV'));

-- Update display name generation
ALTER TABLE entitlements.business_app_roles
ADD COLUMN computed_display_name VARCHAR(100) 
GENERATED ALWAYS AS (role_function || '_' || role_level || '_' || role_phase) STORED;
```

#### Data Migration Strategy:
```sql
-- Migration mapping for existing roles
UPDATE entitlements.business_app_roles 
SET role_function = 'E0', role_level = 'L1', role_phase = 'IN',
    legacy_role_name = role_name
WHERE role_name = 'INTAKE_ANALYST';

UPDATE entitlements.business_app_roles 
SET role_function = 'E0', role_level = 'L2', role_phase = 'OF',
    legacy_role_name = role_name
WHERE role_name = 'EO_OFFICER';

-- Continue for all existing roles...
```

### 2. Liquibase Migration Scripts - High Effort (2-3 weeks)

#### Files to Update:
- `src/main/resources/db/changelog/002-sample-data.xml`
- `src/main/resources/db/changelog/003-workflow-enhancement-roles.xml` 
- `src/main/resources/db/changelog/004-hybrid-enhancements.xml`
- `src/main/resources/db/changelog/005-minimal-hybrid-enhancements.xml`

#### New Migration File Needed:
```xml
<!-- 006-role-structure-transformation.xml -->
<changeSet id="transform-role-structure" author="claude-code">
    <comment>Transform string-based roles to RoleDetails structure</comment>
    
    <!-- Schema modifications -->
    <!-- Data transformation -->
    <!-- Index updates -->
    <!-- Constraint additions -->
</changeSet>
```

### 3. Java Entity & Service Layer - Medium Effort (1-2 weeks)

#### Files to Modify:
- `src/main/java/com/workflow/entitlements/entity/BusinessAppRole.java`
- `src/main/java/com/workflow/entitlements/service/BusinessAppRoleService.java`
- All role-related repositories and controllers

#### BusinessAppRole.java Updates:
```java
@Entity
public class BusinessAppRole {
    // Existing fields...
    
    @Column(name = "role_function", length = 10)
    private String roleFunction;
    
    @Column(name = "role_level", length = 10)
    private String roleLevel;
    
    @Column(name = "role_phase", length = 10) 
    private String rolePhase;
    
    @Column(name = "legacy_role_name", length = 100)
    private String legacyRoleName;
    
    // Computed display name
    public String getComputedDisplayName() {
        return roleFunction + "_" + roleLevel + "_" + rolePhase;
    }
    
    // Implement RoleDetails interface
    public RoleDetails toRoleDetails() {
        return new RoleDetailsImpl(
            getComputedDisplayName(),
            roleFunction,
            roleLevel,
            rolePhase
        );
    }
}
```

#### New Interface Implementation:
```java
public interface RoleDetails {
    String getDisplayName();
    String getFunction();
    String getLevel();
    String getPhase();
}

@Data
@AllArgsConstructor
public class RoleDetailsImpl implements RoleDetails {
    private String displayName;
    private String function;
    private String level;
    private String phase;
}
```

### 4. Cerbos Policy Engine Updates - High Effort (3-4 weeks)

#### Critical Files Affected:
- `src/main/resources/cerbos/policies/derived_roles/one-cms.yaml`
- `src/main/resources/cerbos/policies/resources/case-nextgen.yaml`
- `src/main/resources/cerbos/policies/resources/workflow-nextgen.yaml`

#### Policy Transformation Examples:

**Current Policy:**
```yaml
- name: intake_phase_authorized
  parentRoles: ["user"]
  condition:
    match:
      expr: has(request.principal.attr.roles.GROUP_EO_INTAKE_ANALYST)
```

**New Policy Structure:**
```yaml
- name: intake_phase_authorized
  parentRoles: ["user"]
  condition:
    match:
      expr: |
        has(request.principal.attr.roleDetails) &&
        request.principal.attr.roleDetails.exists(r, 
          r.function == "E0" && r.level == "L1" && r.phase == "IN")
```

#### Principal Structure Changes:
```json
// Current principal attributes
{
  "roles": {
    "GROUP_EO_HEAD": true,
    "INTAKE_ANALYST": true
  }
}

// New principal attributes  
{
  "roleDetails": [
    {
      "displayName": "E0_L3_RV",
      "function": "E0",
      "level": "L3", 
      "phase": "RV"
    },
    {
      "displayName": "E0_L1_IN",
      "function": "E0",
      "level": "L1",
      "phase": "IN"
    }
  ]
}
```

### 5. Workflow Queue Mappings - High Effort (2-3 weeks)

#### Files Affected:
- `workflow-metadata-registration.sql` - 20+ queue-role mappings
- All BPMN workflow definitions
- `flowable-wrapper-v2/src/main/resources/bpmn/*.xml`

#### Queue Mapping Transformation:
```json
// Current mapping
{
  "GROUP_EO_HEAD": "eo-head-queue",
  "GROUP_EO_OFFICER": "eo-officer-queue",
  "GROUP_CSIS_INTAKE_MANAGER": "csis-intake-manager-queue"
}

// New mapping strategy
{
  "E0_L3_*": "eo-head-queue",        // Function + Level based
  "E0_L2_*": "eo-officer-queue",     // Wildcard phase support
  "CSIS_L3_*": "csis-intake-manager-queue"
}
```

#### BPMN Candidate Group Updates:
```xml
<!-- Current -->
<userTask id="eoHeadReview" name="EO Head Review">
  <candidateGroups>GROUP_EO_HEAD</candidateGroups>
</userTask>

<!-- New -->
<userTask id="eoHeadReview" name="EO Head Review">  
  <candidateGroups>E0_L3</candidateGroups> <!-- Level-based matching -->
</userTask>
```

### 6. Service Integration Layer - Medium Effort (1-2 weeks)

#### Authorization Service Updates:
```java
@Service
public class HybridAuthorizationService {
    
    // New method for role-based authorization
    public AuthorizationCheckResponse checkRoleDetailsAuthorization(
        String userId, String resource, String action) {
        
        List<RoleDetails> userRoles = getUserRoleDetails(userId);
        
        // Build Cerbos principal with new structure
        Principal principal = buildPrincipalWithRoleDetails(userId, userRoles);
        
        return performCerbosCheck(principal, resource, action);
    }
    
    private Principal buildPrincipalWithRoleDetails(String userId, List<RoleDetails> roles) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("roleDetails", roles);
        
        // Also maintain backward compatibility
        Map<String, Boolean> legacyRoles = new HashMap<>();
        for (RoleDetails role : roles) {
            legacyRoles.put(role.getDisplayName(), true);
        }
        attributes.put("roles", legacyRoles);
        
        return Principal.newBuilder()
            .setId(userId)
            .putAllAttr(attributes)
            .build();
    }
}
```

### 7. API & Frontend Integration - Medium Effort (1-2 weeks)

#### OpenAPI Specification Updates:
```yaml
# New RoleDetails schema
RoleDetails:
  type: object
  properties:
    displayName:
      type: string
      example: "E0_L1_IN"
    function:
      type: string
      enum: ["E0", "ER", "CSIS"]
      example: "E0"
    level:
      type: string
      enum: ["L1", "L2", "L3"]
      example: "L1"
    phase:
      type: string
      enum: ["IN", "IU", "OF", "RV"]
      example: "IN"
```

#### Controller Updates:
```java
@RestController
public class UserBusinessAppRoleController {
    
    @GetMapping("/users/{userId}/role-details")
    public ResponseEntity<List<RoleDetails>> getUserRoleDetails(@PathVariable UUID userId) {
        List<RoleDetails> roleDetails = userBusinessAppRoleService.getRoleDetails(userId);
        return ResponseEntity.ok(roleDetails);
    }
}
```

## Migration Strategy

### Phase 1: Schema & Infrastructure (2-3 weeks)
- [ ] Database schema modifications
- [ ] New Liquibase migration scripts
- [ ] Dual support implementation (old + new)
- [ ] Unit test updates

### Phase 2: Service Layer Transformation (2 weeks)
- [ ] Entity model updates
- [ ] Service layer modifications  
- [ ] Repository pattern updates
- [ ] Integration test updates

### Phase 3: Authorization Engine Updates (3-4 weeks)
- [ ] Cerbos policy transformations
- [ ] Principal builder updates
- [ ] Policy validation and testing
- [ ] End-to-end authorization tests

### Phase 4: Workflow Integration (2-3 weeks)
- [ ] Queue mapping updates
- [ ] BPMN definition updates
- [ ] Workflow service integration
- [ ] Queue assignment logic

### Phase 5: API & Frontend (1-2 weeks)
- [ ] OpenAPI specification updates
- [ ] Controller modifications
- [ ] Response format changes
- [ ] API documentation

### Phase 6: Testing & Validation (1 week)
- [ ] Comprehensive integration testing
- [ ] Performance testing
- [ ] Security validation
- [ ] User acceptance testing

## Risk Mitigation

### High-Risk Areas:
1. **Cerbos Policy Complexity**: 64+ role references need transformation
2. **Workflow Integration**: 22 files with GROUP_ patterns  
3. **Queue-Role Mappings**: Complex relationship management
4. **Backwards Compatibility**: Existing sessions and permissions

### Mitigation Strategies:
- **Feature Flags**: Enable gradual rollout
- **Dual Support**: Maintain both old and new structures during transition
- **Comprehensive Testing**: Full authorization scenario validation
- **Rollback Plan**: Quick reversion capability for production issues

### Breaking Changes:
- All existing user role assignments need migration
- Active workflow instances may require attention  
- Cerbos authorization checks will change
- API contracts will evolve

## Testing Strategy

### Unit Tests:
- [ ] Role transformation logic
- [ ] RoleDetails interface implementations
- [ ] Service layer modifications
- [ ] Repository operations

### Integration Tests:
- [ ] Database migration validation
- [ ] Cerbos policy evaluation
- [ ] Workflow queue assignments
- [ ] API endpoint responses

### End-to-End Tests:
- [ ] Complete authorization flows
- [ ] Role assignment workflows
- [ ] Queue-based task assignments
- [ ] Multi-role user scenarios

## Dependencies & Coordination

### Internal Dependencies:
- **Workflow Service**: Queue mappings and BPMN updates
- **OneCMS Service**: Role-based authorization checks
- **API Gateway**: Header-based user context
- **UI Applications**: Role display and management

### External Dependencies:
- **Cerbos Engine**: Policy validation and testing
- **PostgreSQL**: Schema migration and data transformation
- **Flowable Engine**: Candidate group resolution

## Success Criteria

### Technical Criteria:
- [ ] All existing authorization scenarios continue to work
- [ ] New RoleDetails structure fully implemented
- [ ] Cerbos policies successfully transformed
- [ ] Workflow queue assignments function correctly
- [ ] API backward compatibility maintained during transition

### Performance Criteria:
- [ ] Authorization check latency unchanged
- [ ] Database query performance maintained
- [ ] Cerbos policy evaluation performance stable

### Operational Criteria:
- [ ] Zero-downtime deployment achieved
- [ ] Rollback capability validated
- [ ] Monitoring and alerting updated
- [ ] Documentation fully updated

## Timeline Estimate

**Total Duration: 8-10 weeks**
**Team Size: 2-3 developers**

| Phase | Duration | Effort | Dependencies |
|-------|----------|--------|--------------|
| Schema & Infrastructure | 2-3 weeks | High | Database team coordination |
| Service Layer | 2 weeks | Medium | Phase 1 completion |
| Authorization Engine | 3-4 weeks | High | Cerbos expertise required |
| Workflow Integration | 2-3 weeks | High | Workflow team coordination |
| API & Frontend | 1-2 weeks | Medium | UI team coordination |
| Testing & Validation | 1 week | Medium | All phases complete |

## Post-Implementation Tasks

### Cleanup Activities:
- [ ] Remove legacy role fields (after validation period)
- [ ] Clean up old Cerbos policies
- [ ] Remove backwards compatibility code
- [ ] Update all documentation

### Monitoring:
- [ ] Set up role transformation metrics
- [ ] Monitor authorization performance
- [ ] Track policy evaluation success rates
- [ ] Alert on authorization failures

This represents a comprehensive transformation of the role-based authorization system. The structured approach ensures minimal disruption while achieving the new RoleDetails interface requirements.