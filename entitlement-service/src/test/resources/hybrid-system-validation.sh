#!/bin/bash

# =============================================================================
# Hybrid Authorization System - Comprehensive Validation Script
# =============================================================================
# This script provides end-to-end validation of the hybrid authorization
# system, including database integrity, API functionality, and performance.
#
# Usage: ./hybrid-system-validation.sh [--verbose] [--performance]
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="http://localhost:8081"
VERBOSE=false
RUN_PERFORMANCE=false
GENERATE_REPORT=false
REPORT_FORMAT="both"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose)
            VERBOSE=true
            shift
            ;;
        --performance)
            RUN_PERFORMANCE=true
            shift
            ;;
        --report)
            GENERATE_REPORT=true
            shift
            ;;
        --report-format)
            REPORT_FORMAT="$2"
            shift 2
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

verbose_log() {
    if [ "$VERBOSE" = true ]; then
        echo -e "${NC}[DEBUG] $1"
    fi
}

# Show help information
show_help() {
    cat << EOF
Hybrid Authorization System - Comprehensive Validation Script

USAGE:
    $0 [OPTIONS]

DESCRIPTION:
    This script provides end-to-end validation of the hybrid authorization
    system, including database integrity, API functionality, and performance.

OPTIONS:
    --verbose           Enable verbose output with detailed logging
    --performance       Run performance tests (requires 'bc' command)
    --report            Generate detailed validation report
    --report-format     Report format: json, markdown, or both (default: both)
    --help              Show this help message and exit

EXAMPLES:
    # Basic validation
    $0
    
    # Verbose validation with performance testing
    $0 --verbose --performance
    
    # Generate reports with all tests
    $0 --verbose --performance --report
    
    # Generate only JSON report
    $0 --report --report-format json

REQUIREMENTS:
    - Service running at http://localhost:8081
    - jq command for JSON parsing
    - bc command for performance calculations (when using --performance)

EXIT CODES:
    0 - All tests passed
    1 - Some tests failed or validation error occurred
EOF
}

# Test result tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
TEST_RESULTS=()
START_TIME=$(date +%s)

run_test() {
    local test_name="$1"
    local test_command="$2"
    local test_start_time=$(date +%s.%N)
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    log_info "Running test: $test_name"
    
    if eval "$test_command" >/dev/null 2>&1; then
        local test_end_time=$(date +%s.%N)
        local test_duration=$(echo "$test_end_time - $test_start_time" | bc -l)
        log_success "✓ $test_name ($(printf '%.3f' $test_duration)s)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        TEST_RESULTS+=("PASS|$test_name|$(printf '%.3f' $test_duration)s")
        return 0
    else
        local test_end_time=$(date +%s.%N)
        local test_duration=$(echo "$test_end_time - $test_start_time" | bc -l)
        log_error "✗ $test_name ($(printf '%.3f' $test_duration)s)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        TEST_RESULTS+=("FAIL|$test_name|$(printf '%.3f' $test_duration)s")
        if [ "$VERBOSE" = true ]; then
            log_error "Error details:"
            eval "$test_command"
        fi
        return 1
    fi
}

# Generate JSON test report
generate_json_report() {
    local json_file="/tmp/hybrid-validation-report-$(date +%Y%m%d_%H%M%S).json"
    
    cat > "$json_file" << EOF
{
  "validation_report": {
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
    "execution_time_seconds": $TOTAL_DURATION,
    "summary": {
      "total_tests": $TOTAL_TESTS,
      "passed_tests": $PASSED_TESTS,
      "failed_tests": $FAILED_TESTS,
      "success_rate": $SUCCESS_RATE
    },
    "test_results": [
EOF

    local first=true
    for result in "${TEST_RESULTS[@]}"; do
        IFS='|' read -r status test_name duration <<< "$result"
        if [ "$first" = true ]; then
            first=false
        else
            echo "," >> "$json_file"
        fi
        cat >> "$json_file" << EOF
      {
        "name": "$test_name",
        "status": "$status",
        "duration": "$duration"
      }
EOF
    done
    
    cat >> "$json_file" << EOF
    ],
    "system_info": {
      "base_url": "$BASE_URL",
      "verbose_mode": $VERBOSE,
      "performance_testing": $RUN_PERFORMANCE
    }
  }
}
EOF
    
    echo "$json_file"
}

# Generate Markdown test report
generate_markdown_report() {
    local md_file="/tmp/hybrid-validation-report-$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$md_file" << EOF
# Hybrid Authorization System - Validation Report

**Generated**: $(date -u +"%Y-%m-%d %H:%M:%S UTC")  
**Execution Time**: ${TOTAL_DURATION}s  
**Success Rate**: ${SUCCESS_RATE}%

## Summary

- **Total Tests**: $TOTAL_TESTS
- **Passed**: $PASSED_TESTS
- **Failed**: $FAILED_TESTS
- **Base URL**: $BASE_URL

## Test Results

| Test Name | Status | Duration |
|-----------|--------|----------|
EOF

    for result in "${TEST_RESULTS[@]}"; do
        IFS='|' read -r status test_name duration <<< "$result"
        if [ "$status" = "PASS" ]; then
            echo "| $test_name | ✅ PASS | $duration |" >> "$md_file"
        else
            echo "| $test_name | ❌ FAIL | $duration |" >> "$md_file"
        fi
    done
    
    cat >> "$md_file" << EOF

## System Information

- **Verbose Mode**: $VERBOSE
- **Performance Testing**: $RUN_PERFORMANCE
- **Test Environment**: Development
- **Validation Script**: hybrid-system-validation.sh

## Conclusion

EOF

    if [ $FAILED_TESTS -eq 0 ]; then
        echo "✅ **All tests passed successfully!** The hybrid authorization system is fully validated and ready for production deployment." >> "$md_file"
    else
        echo "❌ **Some tests failed.** Please review the failed tests and resolve issues before proceeding." >> "$md_file"
    fi
    
    echo "$md_file"
}

# =============================================================================
# PHASE 1: Service Health Checks
# =============================================================================

phase_1_health_checks() {
    log_info "=== PHASE 1: Service Health Checks ==="
    
    # Test 1: Service is running
    run_test "Service Health Check" "curl -f -s $BASE_URL/actuator/health"
    
    # Test 2: System status endpoint
    run_test "System Status Endpoint" "curl -f -s $BASE_URL/api/system-test/status | jq -e '.systemReady == true'"
    
    # Test 3: Database connectivity
    run_test "Database Connectivity" "curl -f -s $BASE_URL/api/system-test/status | jq -e '.entityCounts.totalUsers > 0'"
    
    verbose_log "Phase 1 completed with $PASSED_TESTS/$TOTAL_TESTS tests passing"
}

# =============================================================================
# PHASE 2: UUID Operations Validation
# =============================================================================

phase_2_uuid_operations() {
    log_info "=== PHASE 2: UUID Operations Validation ==="
    
    # Test 4: UUID user operations
    run_test "UUID User Lookup" "curl -f -s $BASE_URL/api/system-test/users/uuid-test | jq -e '.aliceFound == true and .bobFound == true'"
    
    # Test 5: UUID format validation
    run_test "UUID Format Validation" "curl -f -s $BASE_URL/api/system-test/users/uuid-test | jq -e '.aliceUserId | test(\"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$\")'"
    
    # Test 6: UUID user-role assignments
    run_test "UUID User-Role Assignments" "curl -f -s $BASE_URL/api/system-test/user-roles/uuid-test | jq -e '.activeRoles >= 1'"
    
    verbose_log "Phase 2 completed with $((PASSED_TESTS - $(($TOTAL_TESTS - 3))))/$((3)) UUID tests passing"
}

# =============================================================================
# PHASE 3: Authorization Engine Validation
# =============================================================================

phase_3_authorization_engine() {
    log_info "=== PHASE 3: Authorization Engine Validation ==="
    
    # Test 7: Alice intake analyst authorization
    run_test "Alice Intake Authorization" "curl -f -s -X POST $BASE_URL/api/system-test/authorization/engine-test \
        -H 'Content-Type: application/json' \
        -d '{\"username\":\"alice.intake\",\"resource\":\"case\",\"action\":\"create\"}' | \
        jq -e '.authorizationResult.allowed == true'"
    
    # Test 8: Bob investigator authorization
    run_test "Bob Investigator Authorization" "curl -f -s -X POST $BASE_URL/api/system-test/authorization/engine-test \
        -H 'Content-Type: application/json' \
        -d '{\"username\":\"bob.investigator\",\"resource\":\"case\",\"action\":\"update\"}' | \
        jq -e '.authorizationResult.allowed == true'"
    
    # Test 9: Carol legal authorization
    run_test "Carol Legal Authorization" "curl -f -s -X POST $BASE_URL/api/system-test/authorization/engine-test \
        -H 'Content-Type: application/json' \
        -d '{\"username\":\"carol.legal\",\"resource\":\"case\",\"action\":\"approve\"}' | \
        jq -e '.authorizationResult.allowed == true'"
    
    # Test 10: Admin system access
    run_test "Henry Admin Authorization" "curl -f -s -X POST $BASE_URL/api/system-test/authorization/engine-test \
        -H 'Content-Type: application/json' \
        -d '{\"username\":\"henry.admin\",\"resource\":\"system\",\"action\":\"admin\"}' | \
        jq -e '.authorizationResult.allowed == true'"
    
    # Test 11: Unauthorized access denial
    run_test "Unauthorized Access Denial" "curl -f -s -X POST $BASE_URL/api/system-test/authorization/engine-test \
        -H 'Content-Type: application/json' \
        -d '{\"username\":\"david.hr\",\"resource\":\"system\",\"action\":\"admin\"}' | \
        jq -e '.authorizationResult.allowed == false'"
    
    # Test 12: Invalid user handling
    run_test "Invalid User Handling" "curl -f -s -X POST $BASE_URL/api/system-test/authorization/engine-test \
        -H 'Content-Type: application/json' \
        -d '{\"username\":\"nonexistent.user\",\"resource\":\"case\",\"action\":\"read\"}' | \
        jq -e '.error | contains(\"User not found\")'"
    
    verbose_log "Phase 3 completed with authorization engine tests"
}

# =============================================================================
# PHASE 4: Performance Testing (Optional)
# =============================================================================

phase_4_performance_testing() {
    if [ "$RUN_PERFORMANCE" != true ]; then
        log_info "=== PHASE 4: Performance Testing (Skipped) ==="
        log_warning "Use --performance flag to run performance tests"
        return
    fi
    
    log_info "=== PHASE 4: Performance Testing ==="
    
    # Test 13: Response time test
    log_info "Running response time test..."
    RESPONSE_TIME=$(curl -o /dev/null -s -w '%{time_total}' $BASE_URL/api/system-test/status)
    RESPONSE_TIME_MS=$(echo "$RESPONSE_TIME * 1000" | bc)
    
    if (( $(echo "$RESPONSE_TIME < 1.0" | bc -l) )); then
        log_success "✓ Response Time Test: ${RESPONSE_TIME_MS}ms (< 1000ms)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log_error "✗ Response Time Test: ${RESPONSE_TIME_MS}ms (>= 1000ms)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    # Test 14: Throughput test
    log_info "Running throughput test (10 concurrent requests)..."
    THROUGHPUT_START=$(date +%s.%N)
    
    for i in {1..10}; do
        curl -f -s $BASE_URL/api/system-test/status > /dev/null &
    done
    wait
    
    THROUGHPUT_END=$(date +%s.%N)
    THROUGHPUT_DURATION=$(echo "$THROUGHPUT_END - $THROUGHPUT_START" | bc)
    THROUGHPUT_RPS=$(echo "10 / $THROUGHPUT_DURATION" | bc -l)
    
    if (( $(echo "$THROUGHPUT_RPS > 5" | bc -l) )); then
        log_success "✓ Throughput Test: $(printf '%.2f' $THROUGHPUT_RPS) req/s (> 5 req/s)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        log_error "✗ Throughput Test: $(printf '%.2f' $THROUGHPUT_RPS) req/s (<= 5 req/s)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

# =============================================================================
# PHASE 5: Data Integrity Validation
# =============================================================================

phase_5_data_integrity() {
    log_info "=== PHASE 5: Data Integrity Validation ==="
    
    # Test 15: User count consistency
    run_test "User Count Consistency" "curl -f -s $BASE_URL/api/system-test/status | jq -e '.entityCounts.totalUsers >= .entityCounts.activeUsers'"
    
    # Test 16: Role assignment consistency
    run_test "Role Assignment Consistency" "curl -f -s $BASE_URL/api/system-test/status | jq -e '.entityCounts.totalRoleAssignments >= .entityCounts.activeRoleAssignments'"
    
    # Test 17: UUID format consistency
    run_test "UUID Format Consistency" "curl -f -s $BASE_URL/api/system-test/users/uuid-test | jq -e 'if .aliceUserId then (.aliceUserId | test(\"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$\")) else false end'"
    
    verbose_log "Phase 5 completed with data integrity validations"
}

# =============================================================================
# Main Execution
# =============================================================================

main() {
    log_info "Starting Hybrid Authorization System Validation"
    log_info "Base URL: $BASE_URL"
    log_info "Verbose Mode: $VERBOSE"
    log_info "Performance Testing: $RUN_PERFORMANCE"
    echo ""
    
    # Check if service is accessible
    if ! curl -f -s "$BASE_URL/actuator/health" >/dev/null; then
        log_error "Service is not accessible at $BASE_URL"
        log_error "Please ensure the entitlement-service is running"
        exit 1
    fi
    
    # Check if jq is available
    if ! command -v jq &> /dev/null; then
        log_error "jq is required but not installed. Please install jq to run this script."
        exit 1
    fi
    
    # Check if bc is available for performance tests
    if [ "$RUN_PERFORMANCE" = true ] && ! command -v bc &> /dev/null; then
        log_warning "bc is required for performance tests but not installed."
        log_warning "Performance tests will be skipped."
        RUN_PERFORMANCE=false
    fi
    
    # Run validation phases
    phase_1_health_checks
    echo ""
    phase_2_uuid_operations
    echo ""
    phase_3_authorization_engine
    echo ""
    phase_4_performance_testing
    echo ""
    phase_5_data_integrity
    echo ""
    
    # Calculate total execution time
    END_TIME=$(date +%s)
    TOTAL_DURATION=$((END_TIME - START_TIME))
    
    # Final summary
    log_info "=== VALIDATION SUMMARY ==="
    log_info "Execution Time: ${TOTAL_DURATION}s"
    log_info "Total Tests: $TOTAL_TESTS"
    log_success "Passed: $PASSED_TESTS"
    if [ $FAILED_TESTS -gt 0 ]; then
        log_error "Failed: $FAILED_TESTS"
    else
        log_success "Failed: $FAILED_TESTS"
    fi
    
    SUCCESS_RATE=$(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)
    log_info "Success Rate: ${SUCCESS_RATE}%"
    
    # Detailed test results table
    if [ "$VERBOSE" = true ] || [ $FAILED_TESTS -gt 0 ]; then
        echo ""
        log_info "=== DETAILED TEST RESULTS ==="
        printf "%-8s %-60s %-10s\n" "STATUS" "TEST NAME" "DURATION"
        printf "%-8s %-60s %-10s\n" "------" "---------" "--------"
        
        for result in "${TEST_RESULTS[@]}"; do
            IFS='|' read -r status test_name duration <<< "$result"
            if [ "$status" = "PASS" ]; then
                printf "${GREEN}%-8s${NC} %-60s %-10s\n" "PASS" "$test_name" "$duration"
            else
                printf "${RED}%-8s${NC} %-60s %-10s\n" "FAIL" "$test_name" "$duration"
            fi
        done
    fi
    
    # Generate reports if requested
    if [ "$GENERATE_REPORT" = true ] || [ "$VERBOSE" = true ]; then
        echo ""
        log_info "=== REPORT GENERATION ==="
        
        if [ "$REPORT_FORMAT" = "json" ] || [ "$REPORT_FORMAT" = "both" ]; then
            JSON_REPORT_FILE=$(generate_json_report)
            log_success "JSON report generated: $JSON_REPORT_FILE"
        fi
        
        if [ "$REPORT_FORMAT" = "markdown" ] || [ "$REPORT_FORMAT" = "both" ]; then
            MARKDOWN_REPORT_FILE=$(generate_markdown_report)
            log_success "Markdown report generated: $MARKDOWN_REPORT_FILE"
        fi
    fi
    
    if [ $FAILED_TESTS -eq 0 ]; then
        log_success "🎉 All tests passed! Hybrid authorization system is validated."
        exit 0
    else
        log_error "❌ Some tests failed. Please review the results above."
        exit 1
    fi
}

# Execute main function
main "$@"