# BPMN Security Validation

## Overview

Comprehensive security validation system for BPMN 2.0 process definitions to prevent malicious content and ensure organizational compliance.

## Security Features

### 1. **XML Structure Validation**
- Well-formed XML validation
- Required BPMN 2.0 namespace verification
- XML External Entity (XXE) attack prevention
- DOCTYPE declaration blocking

### 2. **Content Security Scanning**
- Dangerous pattern detection in scripts and expressions
- Malicious code pattern recognition
- SQL injection and command injection prevention

### 3. **Resource Limits**
- Maximum process elements limit (default: 200)
- Script length restrictions (default: 1000 chars)
- Expression length restrictions (default: 500 chars)
- Deployment size limits (default: 5MB)

### 4. **Script Security**
- Allowed script format enforcement (groovy, javascript, juel)
- Blocked Java package detection
- Runtime command execution prevention
- File system access restrictions

### 5. **Service Task Validation**
- Java class reference validation
- Whitelisted package enforcement
- External web service call monitoring
- Delegate expression security

## Configuration

Configure security settings in `application.yml`:

```yaml
workflow:
  security:
    validation-enabled: true
    strict-mode: false
    max-process-elements: 200
    max-script-length: 1000
    max-expression-length: 500
    max-deployment-size: 5242880  # 5MB
    allowed-script-formats:
      - groovy
      - javascript
      - juel
    blocked-java-packages:
      - java.lang.Runtime
      - java.lang.ProcessBuilder
      - java.io.File
      - java.nio.file
      - java.lang.System
      - java.lang.reflect
    whitelisted-packages:
      - com.flowable.
      - com.workflow.
      - org.flowable.
```

## Violation Types

### Critical Violations (Block Deployment)
- **INVALID_XML**: Malformed or invalid XML structure
- **PARSING_ERROR**: XML parsing failed
- **MALICIOUS_CONTENT**: Potentially malicious content detected
- **UNAUTHORIZED_API**: Unauthorized API or system access
- **UNAUTHORIZED_SCRIPT**: Unauthorized scripting language or content
- **RESOURCE_LIMIT**: Resource limits exceeded

### Warnings (Allow Deployment)
- **INVALID_STRUCTURE**: BPMN structure best practice violations
- **SECURITY_WARNING**: Security best practice recommendations
- **PERFORMANCE_WARNING**: Performance impact warnings

## Dangerous Patterns Detected

The validator detects these potentially dangerous patterns:

```regex
- Runtime\.getRuntime          # Java Runtime execution
- ProcessBuilder               # Process spawning
- System\.(exit|getProperty)   # System manipulation
- Class\.forName              # Dynamic class loading
- \$\{.*java\.lang.*\}        # Expression language Java access
- eval\s*\(                   # Dynamic code evaluation
- exec\s*\(                   # Command execution
```

## Security Best Practices

### ✅ **Allowed Patterns**
```xml
<!-- Safe user task assignment -->
<userTask id="task1" assignee="${initiator}" candidateGroups="managers"/>

<!-- Safe condition expression -->
<conditionExpression>${status == 'approved'}</conditionExpression>

<!-- Safe service task with whitelisted class -->
<serviceTask id="serviceTask" class="com.workflow.service.NotificationService"/>

<!-- Safe script with allowed format -->
<scriptTask scriptFormat="groovy">
  <script>execution.setVariable('result', 'processed')</script>
</scriptTask>
```

### ❌ **Blocked Patterns**
```xml
<!-- Dangerous system access -->
<scriptTask scriptFormat="groovy">
  <script>Runtime.getRuntime().exec("rm -rf /")</script>
</scriptTask>

<!-- File system access -->
<serviceTask class="java.io.File"/>

<!-- Dynamic class loading -->
<script>Class.forName('malicious.Class')</script>

<!-- Expression injection -->
<conditionExpression>${java.lang.Runtime.getRuntime().exec('cmd')}</conditionExpression>
```

## Integration Points

### 1. **Workflow Deployment**
Security validation is automatically triggered during:
- BPMN workflow deployment (`/deploy`)
- File-based workflow deployment (`/deploy-from-file`)

### 2. **API Endpoints**
All workflow metadata endpoints include security validation:
- `POST /api/{businessApp}/workflow-metadata/deploy`
- `POST /api/{businessApp}/workflow-metadata/deploy-from-file`

### 3. **Error Handling**
Security violations return appropriate HTTP status codes:
- **400 Bad Request**: Validation failures
- **403 Forbidden**: Unauthorized content detected
- **500 Internal Error**: Parsing or system errors

## Monitoring and Alerting

Security validation events are logged and can be monitored:

```java
// Log levels by severity
log.error("BPMN security validation failed: {}", violations);
log.warn("BPMN security validation warnings: {}", warnings);
log.info("BPMN security validation passed for: {}", processKey);
```

### Metrics Integration
Custom metrics for monitoring:
- `workflow_security_violations_total`
- `workflow_security_warnings_total`
- `workflow_deployments_blocked_total`

## Testing Security Validation

### Test Cases Covered
1. **Valid BPMN**: Should pass validation
2. **Malicious Scripts**: Should block deployment
3. **Resource Limits**: Should enforce limits
4. **Unauthorized APIs**: Should detect and block
5. **XML Structure**: Should validate structure

### Example Test BPMN
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL">
  <process id="testProcess" isExecutable="true">
    <startEvent id="start"/>
    <scriptTask id="malicious" scriptFormat="groovy">
      <script>Runtime.getRuntime().exec("malicious-command")</script>
    </scriptTask>
    <endEvent id="end"/>
  </process>
</definitions>
```
**Expected Result**: `MALICIOUS_CONTENT` violation, deployment blocked.

## Future Enhancements

1. **AI-based Pattern Detection**: Machine learning for advanced threat detection
2. **Custom Validation Rules**: Organization-specific validation rules
3. **Audit Trail Integration**: Complete security audit logging
4. **Real-time Scanning**: Continuous monitoring of deployed processes
5. **Policy Templates**: Pre-defined security policy templates

## Compliance

This validation system helps ensure compliance with:
- **OWASP Security Guidelines**
- **Enterprise Security Policies**
- **Data Protection Regulations**
- **Industry Security Standards**