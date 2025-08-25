package com.flowable.wrapper.security;

import com.flowable.wrapper.config.SecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Comprehensive security validator for BPMN 2.0 process definitions.
 * Validates BPMN content against security best practices and organizational policies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BpmnSecurityValidator {
    
    private final SecurityConfig securityConfig;
    
    // Static dangerous patterns that are always checked
    private static final Set<String> BLOCKED_JAVA_PACKAGES_STATIC = Set.of(
        "java.lang.Runtime", "java.lang.ProcessBuilder", "java.io.File", 
        "java.nio.file", "java.lang.System", "java.lang.reflect"
    );
    
    // Suspicious patterns in scripts and expressions
    private static final List<Pattern> DANGEROUS_PATTERNS = Arrays.asList(
        Pattern.compile("Runtime\\.getRuntime", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ProcessBuilder", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\.(exit|getProperty)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Class\\.forName", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\$\\{.*java\\.lang.*\\}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("exec\\s*\\(", Pattern.CASE_INSENSITIVE)
    );
    
    /**
     * Main validation method for BPMN XML content
     */
    public BpmnValidationResult validateBpmnXml(String bpmnXml, String processDefinitionKey) {
        log.info("Starting security validation for BPMN: {}", processDefinitionKey);
        BpmnValidationResult result = new BpmnValidationResult();
        
        try {
            // 1. Basic XML structure validation
            validateXmlStructure(bpmnXml, result);
            if (result.hasBlockingViolations()) {
                return result;
            }
            
            // 2. Parse and validate DOM elements
            Document doc = parseXmlDocument(bpmnXml);
            validateDomElements(doc, result);
            
            // 3. Validate process definition limits
            validateProcessLimits(doc, result);
            
            // 4. Validate script tasks and expressions
            validateScriptsAndExpressions(doc, result);
            
            // 5. Validate service tasks and external calls
            validateServiceTasks(doc, result);
            
            // 6. Validate candidate groups and assignees
            validateTaskAssignments(doc, result);
            
            // 7. Validate timer expressions
            validateTimerExpressions(doc, result);
            
            log.info("BPMN validation completed for {}: {} violations, {} warnings", 
                processDefinitionKey, result.getViolationCount(), result.getWarningCount());
                
        } catch (Exception e) {
            log.error("BPMN validation failed for {}: {}", processDefinitionKey, e.getMessage(), e);
            result.addViolation(BpmnViolationType.PARSING_ERROR, 
                "Failed to parse BPMN XML: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Validate basic XML structure and well-formedness
     */
    private void validateXmlStructure(String bpmnXml, BpmnValidationResult result) {
        if (bpmnXml == null || bpmnXml.trim().isEmpty()) {
            result.addViolation(BpmnViolationType.INVALID_XML, "BPMN XML is null or empty");
            return;
        }
        
        // Check for XML declaration
        if (!bpmnXml.trim().startsWith("<?xml")) {
            result.addWarning("BPMN XML missing XML declaration");
        }
        
        // Check for required BPMN namespace
        if (!bpmnXml.contains("http://www.omg.org/spec/BPMN/20100524/MODEL")) {
            result.addViolation(BpmnViolationType.INVALID_XML, 
                "Missing required BPMN 2.0 namespace");
        }
        
        // Check for suspicious content
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(bpmnXml).find()) {
                result.addViolation(BpmnViolationType.MALICIOUS_CONTENT, 
                    "Potentially dangerous pattern detected: " + pattern.pattern());
            }
        }
    }
    
    /**
     * Parse XML document safely
     */
    private Document parseXmlDocument(String bpmnXml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        
        // Security hardening for XML parsing
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(bpmnXml.getBytes()));
    }
    
    /**
     * Validate DOM elements for security issues
     */
    private void validateDomElements(Document doc, BpmnValidationResult result) {
        // Check for excessive element count
        int totalElements = doc.getElementsByTagName("*").getLength();
        if (totalElements > securityConfig.getMaxProcessElements()) {
            result.addViolation(BpmnViolationType.RESOURCE_LIMIT, 
                String.format("Too many elements in process: %d (max: %d)", 
                    totalElements, securityConfig.getMaxProcessElements()));
        }
        
        // Validate process definition
        NodeList processes = doc.getElementsByTagNameNS("*", "process");
        if (processes.getLength() == 0) {
            result.addViolation(BpmnViolationType.INVALID_STRUCTURE, 
                "No process definition found in BPMN");
        } else if (processes.getLength() > 1) {
            result.addWarning("Multiple process definitions found");
        }
    }
    
    /**
     * Validate process limits and constraints
     */
    private void validateProcessLimits(Document doc, BpmnValidationResult result) {
        // Check for process executable flag
        NodeList processes = doc.getElementsByTagNameNS("*", "process");
        for (int i = 0; i < processes.getLength(); i++) {
            Element process = (Element) processes.item(i);
            String executable = process.getAttribute("isExecutable");
            
            if (!"true".equalsIgnoreCase(executable)) {
                result.addWarning("Process is not marked as executable: " + 
                    process.getAttribute("id"));
            }
        }
        
        // Validate start events
        NodeList startEvents = doc.getElementsByTagNameNS("*", "startEvent");
        if (startEvents.getLength() == 0) {
            result.addViolation(BpmnViolationType.INVALID_STRUCTURE, 
                "Process must have at least one start event");
        }
        
        // Validate end events
        NodeList endEvents = doc.getElementsByTagNameNS("*", "endEvent");
        if (endEvents.getLength() == 0) {
            result.addWarning("Process should have at least one end event");
        }
    }
    
    /**
     * Validate script tasks and expressions for security
     */
    private void validateScriptsAndExpressions(Document doc, BpmnValidationResult result) {
        // Check script tasks
        NodeList scriptTasks = doc.getElementsByTagNameNS("*", "scriptTask");
        for (int i = 0; i < scriptTasks.getLength(); i++) {
            Element scriptTask = (Element) scriptTasks.item(i);
            validateScriptTask(scriptTask, result);
        }
        
        // Check script listeners
        NodeList listeners = doc.getElementsByTagNameNS("*", "executionListener");
        for (int i = 0; i < listeners.getLength(); i++) {
            Element listener = (Element) listeners.item(i);
            if ("script".equals(listener.getAttribute("class"))) {
                validateScriptListener(listener, result);
            }
        }
        
        // Check conditional expressions
        NodeList conditions = doc.getElementsByTagNameNS("*", "conditionExpression");
        for (int i = 0; i < conditions.getLength(); i++) {
            Element condition = (Element) conditions.item(i);
            validateExpression(condition.getTextContent(), "conditionExpression", result);
        }
    }
    
    /**
     * Validate individual script task
     */
    private void validateScriptTask(Element scriptTask, BpmnValidationResult result) {
        String taskId = scriptTask.getAttribute("id");
        String scriptFormat = scriptTask.getAttribute("scriptFormat");
        String script = scriptTask.getTextContent();
        
        // Validate script format
        if (scriptFormat != null && !securityConfig.getAllowedScriptFormats().contains(scriptFormat.toLowerCase())) {
            result.addViolation(BpmnViolationType.UNAUTHORIZED_SCRIPT, 
                String.format("Unauthorized script format '%s' in task '%s'", scriptFormat, taskId));
        }
        
        // Validate script content
        if (script != null && !script.trim().isEmpty()) {
            validateScriptContent(script, taskId, result);
        }
    }
    
    /**
     * Validate script listener
     */
    private void validateScriptListener(Element listener, BpmnValidationResult result) {
        String event = listener.getAttribute("event");
        NodeList scripts = listener.getElementsByTagNameNS("*", "script");
        
        for (int i = 0; i < scripts.getLength(); i++) {
            Element script = (Element) scripts.item(i);
            String scriptContent = script.getTextContent();
            validateScriptContent(scriptContent, "listener:" + event, result);
        }
    }
    
    /**
     * Validate script content for dangerous patterns
     */
    private void validateScriptContent(String script, String context, BpmnValidationResult result) {
        if (script == null) return;
        
        // Check script length
        if (script.length() > securityConfig.getMaxScriptLength()) {
            result.addViolation(BpmnViolationType.RESOURCE_LIMIT, 
                String.format("Script too long in %s: %d chars (max: %d)", 
                    context, script.length(), securityConfig.getMaxScriptLength()));
        }
        
        // Check for dangerous patterns
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(script).find()) {
                result.addViolation(BpmnViolationType.MALICIOUS_CONTENT, 
                    String.format("Dangerous pattern found in %s: %s", context, pattern.pattern()));
            }
        }
        
        // Check for blocked Java packages
        for (String blockedPackage : securityConfig.getBlockedJavaPackages()) {
            if (script.contains(blockedPackage)) {
                result.addViolation(BpmnViolationType.UNAUTHORIZED_API, 
                    String.format("Blocked Java package used in %s: %s", context, blockedPackage));
            }
        }
    }
    
    /**
     * Validate expressions
     */
    private void validateExpression(String expression, String context, BpmnValidationResult result) {
        if (expression == null || expression.trim().isEmpty()) return;
        
        if (expression.length() > MAX_EXPRESSION_LENGTH) {
            result.addViolation(BpmnViolationType.RESOURCE_LIMIT, 
                String.format("Expression too long in %s: %d chars (max: %d)", 
                    context, expression.length(), MAX_EXPRESSION_LENGTH));
        }
        
        // Check for dangerous patterns in expressions
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(expression).find()) {
                result.addViolation(BpmnViolationType.MALICIOUS_CONTENT, 
                    String.format("Dangerous pattern in expression %s: %s", context, pattern.pattern()));
            }
        }
    }
    
    /**
     * Validate service tasks and external calls
     */
    private void validateServiceTasks(Document doc, BpmnValidationResult result) {
        NodeList serviceTasks = doc.getElementsByTagNameNS("*", "serviceTask");
        
        for (int i = 0; i < serviceTasks.getLength(); i++) {
            Element serviceTask = (Element) serviceTasks.item(i);
            String taskId = serviceTask.getAttribute("id");
            String implementation = serviceTask.getAttribute("implementation");
            String clazz = serviceTask.getAttribute("class");
            String delegateExpression = serviceTask.getAttribute("delegateExpression");
            
            // Validate service task implementation
            if ("webService".equals(implementation)) {
                result.addWarning("Web service call found in task: " + taskId);
            }
            
            // Validate Java class usage
            if (clazz != null && !clazz.isEmpty()) {
                validateJavaClass(clazz, taskId, result);
            }
            
            // Validate delegate expressions
            if (delegateExpression != null && !delegateExpression.isEmpty()) {
                validateExpression(delegateExpression, "serviceTask:" + taskId, result);
            }
        }
    }
    
    /**
     * Validate Java class references
     */
    private void validateJavaClass(String className, String context, BpmnValidationResult result) {
        // Check for blocked packages
        for (String blockedPackage : BLOCKED_JAVA_PACKAGES) {
            if (className.startsWith(blockedPackage)) {
                result.addViolation(BpmnViolationType.UNAUTHORIZED_API, 
                    String.format("Blocked Java class in %s: %s", context, className));
            }
        }
        
        // Only allow specific whitelisted packages
        if (!className.startsWith("com.flowable.") && 
            !className.startsWith("com.workflow.") &&
            !className.startsWith("org.flowable.")) {
            result.addWarning("External Java class referenced: " + className + " in " + context);
        }
    }
    
    /**
     * Validate task assignments (candidate groups, assignees)
     */
    private void validateTaskAssignments(Document doc, BpmnValidationResult result) {
        NodeList userTasks = doc.getElementsByTagNameNS("*", "userTask");
        
        for (int i = 0; i < userTasks.getLength(); i++) {
            Element userTask = (Element) userTasks.item(i);
            String taskId = userTask.getAttribute("id");
            String assignee = userTask.getAttribute("assignee");
            String candidateGroups = userTask.getAttribute("candidateGroups");
            
            // Warn if task has no assignment
            if ((assignee == null || assignee.isEmpty()) && 
                (candidateGroups == null || candidateGroups.isEmpty())) {
                result.addWarning("User task has no assignee or candidate groups: " + taskId);
            }
            
            // Validate assignee expressions
            if (assignee != null && !assignee.isEmpty()) {
                validateExpression(assignee, "userTask.assignee:" + taskId, result);
            }
        }
    }
    
    /**
     * Validate timer expressions
     */
    private void validateTimerExpressions(Document doc, BpmnValidationResult result) {
        NodeList timerEvents = doc.getElementsByTagNameNS("*", "timerEventDefinition");
        
        for (int i = 0; i < timerEvents.getLength(); i++) {
            Element timer = (Element) timerEvents.item(i);
            
            // Check time duration
            NodeList durations = timer.getElementsByTagNameNS("*", "timeDuration");
            for (int j = 0; j < durations.getLength(); j++) {
                String duration = durations.item(j).getTextContent();
                validateExpression(duration, "timeDuration", result);
            }
            
            // Check time cycle
            NodeList cycles = timer.getElementsByTagNameNS("*", "timeCycle");
            for (int j = 0; j < cycles.getLength(); j++) {
                String cycle = cycles.item(j).getTextContent();
                validateExpression(cycle, "timeCycle", result);
            }
        }
    }
}