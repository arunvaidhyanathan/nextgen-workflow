# Hybrid Authorization System - Validation Guide

## Overview

This document provides comprehensive validation procedures for the hybrid authorization system implemented in the NextGen Workflow Entitlement Service. The system supports both database-driven and Cerbos-based authorization engines with UUID-based user identity management.

## System Architecture Validation

### Core Components Validated
- ✅ **Hybrid Schema Entities**: `entitlement_core_users` table with UUID primary keys
- ✅ **UUID-based User Operations**: All user lookups and operations use UUID format
- ✅ **Authorization Engine Switching**: Dynamic switching between Database and Cerbos engines
- ✅ **API Documentation**: OpenAPI 3.0.3 specification with proper UUID formats
- ✅ **System Test Infrastructure**: Comprehensive test endpoints for validation

## Validation Test Suites

### 1. End-to-End Integration Tests
**Location**: `src/test/java/com/workflow/entitlements/integration/HybridSystemEndToEndTest.java`

**Test Coverage**:
- E2E-001: System Status and Health Check
- E2E-002: UUID User Operations Test
- E2E-003: UUID User-Role Assignment Test
- E2E-004: Authorization Engine - Alice Intake Analyst Case Create
- E2E-005: Authorization Engine - Bob Investigator Case Update
- E2E-006: Authorization Engine - Carol Legal Case Review
- E2E-007: Authorization Engine - Henry Admin System Access
- E2E-008: Authorization Engine - Unauthorized Access Denial
- E2E-009: Cross-Department Resource Access Test
- E2E-010: Invalid User Authorization Test

**Execution**:
```bash
mvn test -Dtest=HybridSystemEndToEndTest
```

### 2. Performance Tests
**Location**: `src/test/java/com/workflow/entitlements/performance/HybridAuthorizationPerformanceTest.java`

**Test Coverage**:
- PERF-001: Single Authorization Request Latency (< 500ms)
- PERF-002: Bulk Authorization Requests Performance (avg < 200ms)
- PERF-003: Concurrent Authorization Requests (> 10 req/s throughput)
- PERF-004: System Status Endpoint Performance (< 100ms)
- PERF-005: UUID User Operations Performance (< 150ms avg)
- PERF-006: Memory Usage Under Load (< 50MB increase)

**Execution**:
```bash
mvn test -Dtest=HybridAuthorizationPerformanceTest
```

### 3. Automated Validation Script
**Location**: `src/test/resources/hybrid-system-validation.sh`

**Features**:
- Comprehensive end-to-end system validation
- Health checks and connectivity tests
- Authorization engine functionality validation
- Performance testing (optional)
- Data integrity checks
- Detailed reporting with success/failure metrics

**Execution**:
```bash
# Basic validation
./src/test/resources/hybrid-system-validation.sh

# Verbose output with performance testing
./src/test/resources/hybrid-system-validation.sh --verbose --performance

# Generate comprehensive reports (JSON and Markdown)
./src/test/resources/hybrid-system-validation.sh --verbose --performance --report

# Generate only JSON report
./src/test/resources/hybrid-system-validation.sh --report --report-format json

# Show help and all available options
./src/test/resources/hybrid-system-validation.sh --help
```

### Enhanced Reporting Features
The validation script now includes comprehensive reporting capabilities:

- **Real-time Performance Metrics**: Each test execution time tracked and reported
- **JSON Report Generation**: Machine-readable test results with detailed metrics
- **Markdown Report Generation**: Human-readable formatted test results
- **Detailed Test Results Table**: Visual status overview with color-coded results
- **System Information Capture**: Environment details and configuration settings

**Report Formats Available**:
- `json`: Structured JSON report for CI/CD integration
- `markdown`: Formatted Markdown report for documentation
- `both`: Both JSON and Markdown reports (default)

**Report Contents**:
- Test execution timestamps and durations
- Pass/fail status for each test
- Performance metrics and benchmarks
- System configuration and environment info
- Summary statistics and success rates

## System Test Endpoints

### Core System Testing Endpoints

1. **GET** `/api/system-test/status`
   - **Purpose**: System health and entity counts
   - **Response**: System status, user counts, role assignments
   - **Validation**: `systemReady: true`, entity counts > 0

2. **GET** `/api/system-test/users/uuid-test`
   - **Purpose**: UUID-based user operations validation
   - **Response**: User lookup results with proper UUID format
   - **Validation**: UUID format compliance, user existence checks

3. **GET** `/api/system-test/user-roles/uuid-test`
   - **Purpose**: UUID-based user-role assignments validation
   - **Response**: Role assignments for test users
   - **Validation**: Active roles, proper relationship mapping

4. **POST** `/api/system-test/authorization/engine-test`
   - **Purpose**: Authorization engine functionality testing
   - **Request**: `{"username": "alice.intake", "resource": "case", "action": "create"}`
   - **Response**: Authorization decision with detailed reasoning
   - **Validation**: Proper allow/deny decisions based on user roles

## Test Data Configuration

### Sample Users (UUID-based)
| User ID | Username | Department | Primary Role |
|---------|----------|------------|--------------|
| 550e8400-e29b-41d4-a716-446655440001 | alice.intake | IU | INTAKE_ANALYST |
| 550e8400-e29b-41d4-a716-446655440002 | bob.investigator | IU | INVESTIGATOR |
| 550e8400-e29b-41d4-a716-446655440003 | carol.legal | LEGAL | LEGAL_COUNSEL |
| 550e8400-e29b-41d4-a716-446655440004 | david.hr | HR | HR_ANALYST |
| 550e8400-e29b-41d4-a716-446655440005 | eve.manager | IU | INVESTIGATION_MANAGER |
| 550e8400-e29b-41d4-a716-446655440008 | henry.admin | IT | ADMIN |

### Authorization Test Scenarios
| Test User | Resource | Action | Expected Result | Reason |
|-----------|----------|--------|------------------|---------|
| alice.intake | case | create | ALLOW | Has INTAKE_ANALYST role |
| bob.investigator | case | update | ALLOW | Has INVESTIGATOR role |
| carol.legal | case | approve | ALLOW | Has LEGAL_COUNSEL role |
| henry.admin | system | admin | ALLOW | Has ADMIN role |
| david.hr | system | admin | DENY | Insufficient permissions |
| nonexistent.user | case | read | ERROR | User not found |

## Performance Benchmarks

### Response Time Targets
- **Single Authorization Request**: < 500ms
- **Bulk Operations (50 requests)**: < 200ms average
- **System Status Endpoint**: < 100ms average
- **UUID User Operations**: < 150ms average

### Throughput Targets
- **Concurrent Requests**: > 10 requests/second
- **System Availability**: > 99% success rate under load
- **Memory Usage**: < 50MB increase under load testing

### Load Testing Parameters
- **Concurrent Threads**: 10
- **Requests per Thread**: 10
- **Total Test Requests**: 100
- **Test Duration**: < 30 seconds

## Validation Checklist

### Pre-Validation Setup
- [ ] Entitlement service running on port 8081
- [ ] PostgreSQL database accessible with sample data loaded
- [ ] All required dependencies installed (Maven, JUnit 5, jq, bc)
- [ ] Test profiles configured (`application-test.yml`)

### System Components
- [ ] Hybrid schema entities properly mapped
- [ ] UUID format consistency across all user operations
- [ ] Authorization engine switching functional
- [ ] API documentation updated with UUID formats
- [ ] System test endpoints accessible

### Functional Validation
- [ ] All end-to-end tests passing (10/10)
- [ ] Performance tests meeting benchmark targets (6/6)
- [ ] Automated validation script completing successfully
- [ ] Authorization decisions accurate for all test scenarios
- [ ] Error handling proper for invalid inputs

### Performance Validation
- [ ] Single request latency under threshold
- [ ] Bulk operation performance acceptable
- [ ] Concurrent request handling robust
- [ ] Memory usage within limits
- [ ] System stability under load

## Troubleshooting

### Common Issues

1. **Service Not Starting**
   - Check PostgreSQL connection
   - Verify Liquibase migrations completed
   - Review application logs for startup errors

2. **Test Failures**
   - Ensure sample data loaded properly
   - Verify user UUIDs match expected values
   - Check authorization engine configuration

3. **Performance Issues**
   - Monitor JVM memory settings
   - Check database connection pool configuration
   - Review query performance and indexing

### Debugging Commands

```bash
# Check service health
curl http://localhost:8081/actuator/health

# Verify system status
curl http://localhost:8081/api/system-test/status | jq

# Test specific user lookup
curl http://localhost:8081/api/system-test/users/uuid-test | jq

# Test authorization with verbose output
curl -X POST http://localhost:8081/api/system-test/authorization/engine-test \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice.intake","resource":"case","action":"create"}' | jq
```

## Validation Results Documentation

### Expected Outcomes
- **All Integration Tests**: PASS (100% success rate)
- **All Performance Tests**: PASS (meeting benchmark targets)
- **Automated Validation Script**: SUCCESS (all phases completed)
- **System Stability**: No memory leaks or performance degradation
- **API Compliance**: Full OpenAPI 3.0.3 specification alignment

### Success Criteria
✅ **Functional Requirements**: All authorization scenarios work correctly
✅ **Performance Requirements**: All benchmarks met or exceeded
✅ **Data Integrity**: UUID format consistency maintained
✅ **System Reliability**: Stable operation under concurrent load
✅ **API Documentation**: Complete and accurate specification

## Conclusion

The hybrid authorization system validation suite provides comprehensive testing coverage for:
- UUID-based user identity management
- Authorization engine functionality and switching
- Performance characteristics under load
- Data integrity and system stability
- API documentation accuracy

This validation framework ensures the system meets all functional and performance requirements for production deployment.