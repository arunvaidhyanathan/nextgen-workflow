{\rtf1\ansi\ansicpg1252\cocoartf2822
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fswiss\fcharset0 Helvetica;\f1\froman\fcharset0 Times-Bold;\f2\froman\fcharset0 Times-Roman;
\f3\fmodern\fcharset0 Courier;}
{\colortbl;\red255\green255\blue255;\red0\green0\blue0;}
{\*\expandedcolortbl;;\cssrgb\c0\c0\c0;}
{\*\listtable{\list\listtemplateid1\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid1\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid1}
{\list\listtemplateid2\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid101\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid2}
{\list\listtemplateid3\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid201\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid3}
{\list\listtemplateid4\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid301\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid4}
{\list\listtemplateid5\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid401\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid5}
{\list\listtemplateid6\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid501\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid6}
{\list\listtemplateid7\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid601\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid7}
{\list\listtemplateid8\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid701\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid8}
{\list\listtemplateid9\listhybrid{\listlevel\levelnfc23\levelnfcn23\leveljc0\leveljcn0\levelfollow0\levelstartat1\levelspace360\levelindent0{\*\levelmarker \{disc\}}{\leveltext\leveltemplateid801\'01\uc0\u8226 ;}{\levelnumbers;}\fi-360\li720\lin720 }{\listname ;}\listid9}}
{\*\listoverridetable{\listoverride\listid1\listoverridecount0\ls1}{\listoverride\listid2\listoverridecount0\ls2}{\listoverride\listid3\listoverridecount0\ls3}{\listoverride\listid4\listoverridecount0\ls4}{\listoverride\listid5\listoverridecount0\ls5}{\listoverride\listid6\listoverridecount0\ls6}{\listoverride\listid7\listoverridecount0\ls7}{\listoverride\listid8\listoverridecount0\ls8}{\listoverride\listid9\listoverridecount0\ls9}}
\margl1440\margr1440\vieww30040\viewh18340\viewkind0
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\pardirnatural\partightenfactor0

\f0\fs24 \cf0 -- Enhanced RBAC Schema for OneCMS Workflow\
-- Optimized for PostgreSQL with advanced features\
\
-- Create schema\
CREATE SCHEMA IF NOT EXISTS entitlements;\
\
-- Enable extensions\
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";\
CREATE EXTENSION IF NOT EXISTS "btree_gist";\
\
-- Roles table with hierarchy and metadata\
CREATE TABLE entitlements.roles (\
    role_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\
    role_name VARCHAR(100) UNIQUE NOT NULL,\
    parent_role_id UUID REFERENCES entitlements.roles(role_id),\
    department VARCHAR(50),\
    is_active BOOLEAN DEFAULT true,\
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
    metadata JSONB DEFAULT '\{\}'::jsonb\
);\
\
-- Create index for role hierarchy queries\
CREATE INDEX idx_role_hierarchy ON entitlements.roles USING btree(parent_role_id);\
CREATE INDEX idx_role_department ON entitlements.roles USING btree(department);\
CREATE INDEX idx_role_metadata ON entitlements.roles USING gin(metadata);\
\
-- Permissions table with categorization\
CREATE TABLE entitlements.permissions (\
    permission_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\
    permission_name VARCHAR(100) UNIQUE NOT NULL,\
    resource_type VARCHAR(50) NOT NULL, -- 'task', 'case', 'document', etc.\
    action VARCHAR(50) NOT NULL, -- 'create', 'read', 'update', 'delete', 'approve', etc.\
    description TEXT,\
    is_active BOOLEAN DEFAULT true,\
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
    metadata JSONB DEFAULT '\{\}'::jsonb\
);\
\
-- Composite index for permission lookups\
CREATE INDEX idx_permission_resource_action ON entitlements.permissions(resource_type, action);\
CREATE INDEX idx_permission_metadata ON entitlements.permissions USING gin(metadata);\
\
-- Role-Permission mapping with conditions\
CREATE TABLE entitlements.role_permissions (\
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\
    role_id UUID NOT NULL REFERENCES entitlements.roles(role_id) ON DELETE CASCADE,\
    permission_id UUID NOT NULL REFERENCES entitlements.permissions(permission_id) ON DELETE CASCADE,\
    conditions JSONB DEFAULT NULL, -- Additional conditions for this permission\
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
    granted_by VARCHAR(100),\
    expires_at TIMESTAMP DEFAULT NULL,\
    UNIQUE(role_id, permission_id)\
);\
\
-- Index for efficient permission checks\
CREATE INDEX idx_role_permissions_lookup ON entitlements.role_permissions(role_id, permission_id) \
    WHERE expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP;\
CREATE INDEX idx_role_permissions_expiry ON entitlements.role_permissions(expires_at) \
    WHERE expires_at IS NOT NULL;\
\
-- Users table\
CREATE TABLE entitlements.users (\
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\
    username VARCHAR(100) UNIQUE NOT NULL,\
    email VARCHAR(255) UNIQUE NOT NULL,\
    department VARCHAR(50),\
    manager_id UUID REFERENCES entitlements.users(user_id),\
    is_active BOOLEAN DEFAULT true,\
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
    attributes JSONB DEFAULT '\{\}'::jsonb -- For ABAC integration\
);\
\
-- Indexes for user queries\
CREATE INDEX idx_users_department ON entitlements.users(department);\
CREATE INDEX idx_users_manager ON entitlements.users(manager_id);\
CREATE INDEX idx_users_attributes ON entitlements.users USING gin(attributes);\
\
-- User-Role assignments with delegation support\
CREATE TABLE entitlements.user_roles (\
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\
    user_id UUID NOT NULL REFERENCES entitlements.users(user_id) ON DELETE CASCADE,\
    role_id UUID NOT NULL REFERENCES entitlements.roles(role_id) ON DELETE CASCADE,\
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
    assigned_by VARCHAR(100),\
    expires_at TIMESTAMP DEFAULT NULL,\
    is_delegated BOOLEAN DEFAULT false,\
    delegated_from UUID REFERENCES entitlements.users(user_id),\
    delegation_reason TEXT,\
    UNIQUE(user_id, role_id)\
);\
\
-- Index for active role lookups\
CREATE INDEX idx_user_roles_active ON entitlements.user_roles(user_id, role_id) \
    WHERE expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP;\
CREATE INDEX idx_user_roles_delegated ON entitlements.user_roles(delegated_from) \
    WHERE is_delegated = true;\
\
-- Audit log for permission checks\
CREATE TABLE entitlements.permission_audit_log (\
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\
    user_id UUID NOT NULL,\
    resource_type VARCHAR(50),\
    resource_id VARCHAR(255),\
    action VARCHAR(50),\
    permission_granted BOOLEAN,\
    check_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
    context JSONB,\
    ip_address INET,\
    session_id VARCHAR(100)\
) PARTITION BY RANGE (check_timestamp);\
\
-- Create partitions for audit log (monthly)\
CREATE TABLE entitlements.permission_audit_log_2025_01 PARTITION OF entitlements.permission_audit_log\
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');\
CREATE TABLE entitlements.permission_audit_log_2025_02 PARTITION OF entitlements.permission_audit_log\
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');\
-- Add more partitions as needed\
\
-- Index for audit queries\
CREATE INDEX idx_audit_user_timestamp ON entitlements.permission_audit_log(user_id, check_timestamp DESC);\
CREATE INDEX idx_audit_resource ON entitlements.permission_audit_log(resource_type, resource_id);\
\
-- Materialized view for effective permissions (refresh periodically)\
CREATE MATERIALIZED VIEW entitlements.user_effective_permissions AS\
WITH RECURSIVE role_hierarchy AS (\
    -- Direct roles\
    SELECT \
        ur.user_id,\
        ur.role_id,\
        r.role_name,\
        0 as level\
    FROM entitlements.user_roles ur\
    JOIN entitlements.roles r ON ur.role_id = r.role_id\
    WHERE (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)\
        AND r.is_active = true\
    \
    UNION ALL\
    \
    -- Inherited roles through hierarchy\
    SELECT \
        rh.user_id,\
        r.role_id,\
        r.role_name,\
        rh.level + 1\
    FROM role_hierarchy rh\
    JOIN entitlements.roles r ON r.parent_role_id = rh.role_id\
    WHERE r.is_active = true\
)\
SELECT DISTINCT\
    rh.user_id,\
    u.username,\
    u.department,\
    r.role_id,\
    r.role_name,\
    p.permission_id,\
    p.permission_name,\
    p.resource_type,\
    p.action,\
    rp.conditions\
FROM role_hierarchy rh\
JOIN entitlements.users u ON rh.user_id = u.user_id\
JOIN entitlements.roles r ON rh.role_id = r.role_id\
JOIN entitlements.role_permissions rp ON r.role_id = rp.role_id\
JOIN entitlements.permissions p ON rp.permission_id = p.permission_id\
WHERE u.is_active = true\
    AND p.is_active = true\
    AND (rp.expires_at IS NULL OR rp.expires_at > CURRENT_TIMESTAMP);\
\
-- Create indexes on materialized view\
CREATE UNIQUE INDEX idx_user_eff_perms_unique ON entitlements.user_effective_permissions(user_id, permission_id);\
CREATE INDEX idx_user_eff_perms_lookup ON entitlements.user_effective_permissions(user_id, resource_type, action);\
\
-- Function to check permissions with caching\
CREATE OR REPLACE FUNCTION entitlements.check_permission(\
    p_user_id UUID,\
    p_resource_type VARCHAR(50),\
    p_action VARCHAR(50),\
    p_context JSONB DEFAULT NULL\
) RETURNS BOOLEAN AS $$\
DECLARE\
    v_has_permission BOOLEAN;\
    v_conditions JSONB;\
BEGIN\
    -- Check materialized view first (fast path)\
    SELECT EXISTS(\
        SELECT 1 \
        FROM entitlements.user_effective_permissions\
        WHERE user_id = p_user_id\
            AND resource_type = p_resource_type\
            AND action = p_action\
    ) INTO v_has_permission;\
    \
    -- Log the check\
    INSERT INTO entitlements.permission_audit_log \
        (user_id, resource_type, action, permission_granted, context)\
    VALUES \
        (p_user_id, p_resource_type, p_action, v_has_permission, p_context);\
    \
    RETURN v_has_permission;\
END;\
$$ LANGUAGE plpgsql;\
\
-- Function to get user's roles with hierarchy\
CREATE OR REPLACE FUNCTION entitlements.get_user_roles(p_user_id UUID)\
RETURNS TABLE(role_id UUID, role_name VARCHAR, level INT) AS $$\
WITH RECURSIVE role_hierarchy AS (\
    SELECT \
        r.role_id,\
        r.role_name,\
        0 as level\
    FROM entitlements.user_roles ur\
    JOIN entitlements.roles r ON ur.role_id = r.role_id\
    WHERE ur.user_id = p_user_id\
        AND (ur.expires_at IS NULL OR ur.expires_at > CURRENT_TIMESTAMP)\
        AND r.is_active = true\
    \
    UNION ALL\
    \
    SELECT \
        r.role_id,\
        r.role_name,\
        rh.level + 1\
    FROM role_hierarchy rh\
    JOIN entitlements.roles r ON r.parent_role_id = rh.role_id\
    WHERE r.is_active = true\
)\
SELECT * FROM role_hierarchy ORDER BY level;\
$$ LANGUAGE sql;\
\
-- Trigger to update timestamps\
CREATE OR REPLACE FUNCTION entitlements.update_updated_at()\
RETURNS TRIGGER AS $$\
BEGIN\
    NEW.updated_at = CURRENT_TIMESTAMP;\
    RETURN NEW;\
END;\
$$ LANGUAGE plpgsql;\
\
CREATE TRIGGER update_roles_updated_at BEFORE UPDATE ON entitlements.roles\
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at();\
\
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON entitlements.users\
    FOR EACH ROW EXECUTE FUNCTION entitlements.update_updated_at();\
\
-- Initial data population\
INSERT INTO entitlements.roles (role_name, department) VALUES\
    ('ADMIN', 'SYSTEM'),\
    ('EO_INTAKE_ANALYST', 'EO'),\
    ('EO_HEAD', 'EO'),\
    ('EO_OFFICER', 'EO'),\
    ('ER_INTAKE_ANALYST', 'ER'),\
    ('ER_INVESTIGATION_MANAGER', 'ER'),\
    ('ER_INVESTIGATOR', 'ER'),\
    ('CSIS_INTAKE_ANALYST', 'CSIS'),\
    ('CSIS_INTAKE_MANAGER', 'CSIS'),\
    ('CSIS_INVESTIGATION_MANAGER', 'CSIS'),\
    ('CSIS_INVESTIGATOR', 'CSIS'),\
    ('LEGAL_INTAKE_ANALYST', 'LEGAL'),\
    ('LEGAL_INVESTIGATION_MANAGER', 'LEGAL');\
\
-- Insert permissions\
INSERT INTO entitlements.permissions (permission_name, resource_type, action, description) VALUES\
    -- Case permissions\
    ('case:create', 'case', 'create', 'Create new cases'),\
    ('case:read', 'case', 'read', 'View case details'),\
    ('case:update', 'case', 'update', 'Update case information'),\
    ('case:delete', 'case', 'delete', 'Delete cases'),\
    ('case:assign', 'case', 'assign', 'Assign cases to users'),\
    ('case:close', 'case', 'close', 'Close cases'),\
    \
    -- Task permissions\
    ('task:create_case', 'task', 'execute', 'Execute create case task'),\
    ('task:approve_closures', 'task', 'execute', 'Execute approve closures task'),\
    ('task:assign_investigator', 'task', 'execute', 'Execute assign investigator task'),\
    ('task:conduct_investigation', 'task', 'execute', 'Execute investigation task'),\
    ('task:review_case', 'task', 'execute', 'Execute case review task'),\
    ('task:triage_case', 'task', 'execute', 'Execute case triage task'),\
    \
    -- Workflow permissions\
    ('workflow:start', 'workflow', 'start', 'Start new workflow instances'),\
    ('workflow:cancel', 'workflow', 'cancel', 'Cancel workflow instances'),\
    ('workflow:view', 'workflow', 'view', 'View workflow status'),\
    \
    -- Document permissions\
    ('document:upload', 'document', 'upload', 'Upload documents'),\
    ('document:view', 'document', 'view', 'View documents'),\
    ('document:download', 'document', 'download', 'Download documents');\
\
-- Map permissions to roles\
-- EO Head permissions\
INSERT INTO entitlements.role_permissions (role_id, permission_id)\
SELECT r.role_id, p.permission_id\
FROM entitlements.roles r\
CROSS JOIN entitlements.permissions p\
WHERE r.role_name = 'EO_HEAD'\
    AND p.permission_name IN ('task:approve_closures', 'case:read', 'case:update', 'case:close');\
\
-- ER Investigation Manager permissions\
INSERT INTO entitlements.role_permissions (role_id, permission_id)\
SELECT r.role_id, p.permission_id\
FROM entitlements.roles r\
CROSS JOIN entitlements.permissions p\
WHERE r.role_name = 'ER_INVESTIGATION_MANAGER'\
    AND p.permission_name IN ('task:assign_investigator', 'case:read', 'case:assign', 'task:review_case');\
\
-- CSIS permissions\
INSERT INTO entitlements.role_permissions (role_id, permission_id)\
SELECT r.role_id, p.permission_id\
FROM entitlements.roles r\
CROSS JOIN entitlements.permissions p\
WHERE r.role_name IN ('CSIS_INTAKE_MANAGER', 'CSIS_INTAKE_ANALYST')\
    AND p.permission_name IN ('case:create', 'case:read', 'case:update', 'task:triage_case');\
\
-- Create refresh function for materialized view\
CREATE OR REPLACE FUNCTION entitlements.refresh_effective_permissions()\
RETURNS void AS $$\
BEGIN\
    REFRESH MATERIALIZED VIEW CONCURRENTLY entitlements.user_effective_permissions;\
END;\
$$ LANGUAGE plpgsql;\
\
-- Schedule periodic refresh (use pg_cron or external scheduler)\
-- SELECT cron.schedule('refresh-permissions', '*/15 * * * *', 'SELECT entitlements.refresh_effective_permissions();');\
\
\
# Enhanced Cerbos Policy for OneCMS Case Management\
# Version: 0.14.0\
apiVersion: "api.cerbos.dev/v1"\
resourcePolicy:\
  resource: "case"\
  version: "0.14.0"\
  \
  # Import shared roles and policies\
  importDerivedRoles:\
    - common_roles\
    - department_roles\
  \
  # Schemas for validation\
  schemas:\
    principalSchema:\
      ref: "cerbos:///principal.json"\
    resourceSchema:\
      ref: "cerbos:///case_resource.json"\
\
  # Variables for reusable logic\
  variables:\
    import:\
      - common_variables\
    local:\
      is_business_hours: "now().hour() >= 8 && now().hour() <= 18"\
      is_high_priority: "request.resource.attr.priority in ['critical', 'high']"\
      case_age_days: "now().sub(timestamp(request.resource.attr.createdAt)).days()"\
      is_overdue: "case_age_days > request.resource.attr.sla_days"\
      \
  # Derived roles with complex conditions\
  derivedRoles:\
    # Direct assignee to the case\
    - name: assignee\
      parentRoles: ["user"]\
      condition:\
        match:\
          expr: "request.principal.id in request.resource.attr.assigneeIds"\
\
    # Manager of the assigned user\
    - name: assigned_manager\
      parentRoles: ["manager"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.principal.attr.role == 'manager'"\
              - expr: "request.principal.attr.subordinates.exists(s, s in request.resource.attr.assigneeIds)"\
\
    # Unit manager for the case's department\
    - name: unit_manager\
      parentRoles: ["manager"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.principal.attr.role == 'manager'"\
              - expr: "request.principal.attr.department == request.resource.attr.department"\
\
    # Cross-department viewer (for collaboration)\
    - name: cross_dept_viewer\
      parentRoles: ["user"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.sharedDepartments.contains(request.principal.attr.department)"\
              - expr: "request.resource.attr.confidentialityLevel != 'secret'"\
\
    # Escalation handler for overdue cases\
    - name: escalation_handler\
      parentRoles: ["manager", "supervisor"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "V.is_overdue"\
              - expr: "request.principal.attr.permissions.contains('handle_escalations')"\
\
    # Delegated authority (temporary permissions)\
    - name: delegated_authority\
      parentRoles: ["user"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.delegations.exists(d, d.delegateTo == request.principal.id && d.validUntil > now())"\
\
  # Main access rules\
  rules:\
    # ========== CREATE Operations ==========\
    - name: "create-case-authenticated"\
      actions: ["create"]\
      effect: EFFECT_ALLOW\
      roles: ["user"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.principal.attr.is_active == true"\
              - expr: "request.principal.attr.department != null"\
\
    - name: "create-case-rate-limit"\
      actions: ["create"]\
      effect: EFFECT_DENY\
      roles: ["*"]\
      condition:\
        match:\
          expr: "request.principal.attr.cases_created_today >= 10"\
      output:\
        when:\
          conditionNotMet:\
            - expr: "true"\
              val: "Daily case creation limit exceeded"\
\
    # ========== READ Operations ==========\
    - name: "view-assigned-case"\
      actions: ["view", "read"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["assignee", "assigned_manager"]\
\
    - name: "view-department-case"\
      actions: ["view", "read"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["unit_manager"]\
      condition:\
        match:\
          expr: "request.resource.attr.status != 'DRAFT'"\
\
    - name: "view-shared-case"\
      actions: ["view:limited"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["cross_dept_viewer"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.sharingEnabled == true"\
              - expr: "request.resource.attr.confidentialityLevel in ['public', 'internal']"\
\
    # ========== UPDATE Operations ==========\
    - name: "edit-own-case"\
      actions: ["edit", "update"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["assignee"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.status in ['OPEN', 'IN_PROGRESS']"\
              - expr: "!request.resource.attr.locked"\
      output:\
        when:\
          ruleActivated:\
            - expr: "request.audit"\
              val:\
                principal: "request.principal.id"\
                action: "edit"\
                timestamp: "now()"\
\
    - name: "edit-subordinate-case"\
      actions: ["edit", "update", "reassign"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["assigned_manager", "unit_manager"]\
      condition:\
        match:\
          expr: "request.resource.attr.status != 'CLOSED'"\
\
    - name: "add-narrative"\
      actions: ["addNarrative", "addComment"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["assignee", "assigned_manager", "cross_dept_viewer"]\
      condition:\
        match:\
          expr: "request.resource.attr.allowComments == true"\
\
    # ========== WORKFLOW Operations ==========\
    - name: "approve-closure"\
      actions: ["approveClosure", "approve"]\
      effect: EFFECT_ALLOW\
      roles: ["eo_head", "department_head"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.status == 'PENDING_CLOSURE'"\
              - expr: "request.resource.attr.investigationComplete == true"\
              - expr: "request.principal.attr.department == request.resource.attr.department"\
\
    - name: "escalate-case"\
      actions: ["escalate"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["escalation_handler"]\
      condition:\
        match:\
          expr: "V.is_overdue || V.is_high_priority"\
\
    - name: "assign-investigator"\
      actions: ["assign", "reassign"]\
      effect: EFFECT_ALLOW\
      roles: ["investigation_manager"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.status in ['TRIAGED', 'IN_PROGRESS']"\
              - expr: "request.principal.attr.availableInvestigators.size() > 0"\
\
    # ========== DELETE Operations ==========\
    - name: "delete-draft-case"\
      actions: ["delete"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["assignee"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.status == 'DRAFT'"\
              - expr: "request.resource.attr.createdBy == request.principal.id"\
              - expr: "V.case_age_days < 7"\
\
    - name: "archive-closed-case"\
      actions: ["archive"]\
      effect: EFFECT_ALLOW\
      roles: ["records_manager", "admin"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.status == 'CLOSED'"\
              - expr: "V.case_age_days > 30"\
\
    # ========== SPECIAL Operations ==========\
    - name: "emergency-access"\
      actions: ["*"]\
      effect: EFFECT_ALLOW\
      roles: ["emergency_responder"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.principal.attr.emergencyAccess == true"\
              - expr: "request.principal.attr.emergencyAccessExpiry > now()"\
      output:\
        when:\
          ruleActivated:\
            - expr: "true"\
              val:\
                alert: "EMERGENCY_ACCESS_USED"\
                principal: "request.principal.id"\
                resource: "request.resource.id"\
\
    - name: "delegated-actions"\
      actions: ["view", "edit", "addNarrative"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["delegated_authority"]\
      condition:\
        match:\
          expr: "request.resource.attr.delegations.exists(d, d.delegateTo == request.principal.id && d.actions.contains(request.action))"\
\
    # ========== Time-based Restrictions ==========\
    - name: "business-hours-only"\
      actions: ["create", "edit", "delete"]\
      effect: EFFECT_DENY\
      roles: ["contractor", "external_user"]\
      condition:\
        match:\
          expr: "!V.is_business_hours"\
      output:\
        when:\
          ruleActivated:\
            - expr: "true"\
              val: "Action restricted to business hours for external users"\
\
    # ========== Audit & Compliance ==========\
    - name: "audit-all-actions"\
      actions: ["*"]\
      effect: EFFECT_ALLOW\
      roles: ["auditor"]\
      condition:\
        match:\
          expr: "request.action in ['view', 'export', 'report']"\
\
    - name: "compliance-review"\
      actions: ["review", "flag", "report"]\
      effect: EFFECT_ALLOW\
      roles: ["compliance_officer"]\
      condition:\
        match:\
          expr: "request.resource.attr.requiresComplianceReview == true"\
\
    # ========== Default Deny ==========\
    - name: "default-deny"\
      actions: ["*"]\
      effect: EFFECT_DENY\
      roles: ["*"]\
      output:\
        when:\
          ruleActivated:\
            - expr: "true"\
              val:\
                denied: true\
                reason: "No matching permission rule"\
                principal: "request.principal.id"\
                action: "request.action"\
                resource: "request.resource.id"\
\
---\
# Auxiliary Policy for Task Resources\
apiVersion: "api.cerbos.dev/v1"\
resourcePolicy:\
  resource: "task"\
  version: "0.14.0"\
  \
  derivedRoles:\
    - name: task_assignee\
      parentRoles: ["user"]\
      condition:\
        match:\
          expr: "request.principal.id == request.resource.attr.assigneeId"\
    \
    - name: task_candidate\
      parentRoles: ["user"]\
      condition:\
        match:\
          expr: "request.principal.attr.groups.exists(g, g in request.resource.attr.candidateGroups)"\
\
  rules:\
    - name: "claim-task"\
      actions: ["claim"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["task_candidate"]\
      condition:\
        match:\
          expr: "request.resource.attr.status == 'CREATED'"\
\
    - name: "complete-task"\
      actions: ["complete"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["task_assignee"]\
      condition:\
        match:\
          all:\
            of:\
              - expr: "request.resource.attr.status == 'IN_PROGRESS'"\
              - expr: "request.resource.attr.formComplete == true"\
\
    - name: "delegate-task"\
      actions: ["delegate"]\
      effect: EFFECT_ALLOW\
      derivedRoles: ["task_assignee"]\
      condition:\
        match:\
          expr: "request.principal.attr.canDelegate == true"\
\
---\
# Principal Policy for Authentication\
apiVersion: "api.cerbos.dev/v1"\
principalPolicy:\
  principal: "user"\
  version: "0.14.0"\
  \
  rules:\
    - resource: "*"\
      actions:\
        - name: "*"\
          action: "*"\
          effect: EFFECT_DENY\
          condition:\
            match:\
              any:\
                of:\
                  - expr: "request.principal.attr.is_active == false"\
                  - expr: "request.principal.attr.account_locked == true"\
                  - expr: "request.principal.attr.password_expired == true"\
          output:\
            when:\
              ruleActivated:\
                - expr: "request.principal.attr.is_active == false"\
                  val: "Account is inactive"\
                - expr: "request.principal.attr.account_locked == true"\
                  val: "Account is locked"\
                - expr: "request.principal.attr.password_expired == true"\
                  val: "Password has expired"\
\
\
package com.onecms.workflow.services;\
\
import org.flowable.engine.RuntimeService;\
import org.flowable.engine.TaskService;\
import org.flowable.engine.delegate.DelegateExecution;\
import org.flowable.engine.delegate.JavaDelegate;\
import org.springframework.stereotype.Service;\
import org.springframework.beans.factory.annotation.Autowired;\
import org.springframework.transaction.annotation.Transactional;\
import lombok.extern.slf4j.Slf4j;\
import io.micrometer.core.instrument.MeterRegistry;\
import dev.cerbos.sdk.CerbosClient;\
import dev.cerbos.sdk.CheckResult;\
\
import java.util.*;\
import java.util.concurrent.CompletableFuture;\
import javax.sql.DataSource;\
import java.sql.*;\
\
/**\
 * Enhanced Permission Validation Service\
 * Integrates RBAC (PostgreSQL) and ABAC (Cerbos) for comprehensive access control\
 */\
@Service\
@Slf4j\
public class PermissionValidationService implements JavaDelegate \{\
    \
    @Autowired\
    private CerbosClient cerbosClient;\
    \
    @Autowired\
    private DataSource dataSource;\
    \
    @Autowired\
    private MeterRegistry meterRegistry;\
    \
    @Autowired\
    private RuntimeService runtimeService;\
    \
    @Override\
    @Transactional\
    public void execute(DelegateExecution execution) \{\
        String userId = (String) execution.getVariable("initiator");\
        String caseId = (String) execution.getVariable("caseId");\
        String action = (String) execution.getVariable("requestedAction");\
        \
        log.info("Validating permissions for user: \{\} on case: \{\} for action: \{\}", \
                userId, caseId, action);\
        \
        try \{\
            // Parallel permission checks\
            CompletableFuture<Boolean> rbacCheck = CompletableFuture.supplyAsync(\
                () -> checkRBACPermissions(userId, action)\
            );\
            \
            CompletableFuture<Boolean> abacCheck = CompletableFuture.supplyAsync(\
                () -> checkABACPermissions(userId, caseId, action, execution)\
            );\
            \
            // Wait for both checks\
            boolean rbacResult = rbacCheck.get();\
            boolean abacResult = abacCheck.get();\
            \
            boolean hasPermission = rbacResult && abacResult;\
            \
            // Set workflow variables\
            execution.setVariable("permissionGranted", hasPermission);\
            execution.setVariable("rbacCheckResult", rbacResult);\
            execution.setVariable("abacCheckResult", abacResult);\
            \
            // Metrics\
            meterRegistry.counter("permission.checks", \
                "result", hasPermission ? "granted" : "denied",\
                "action", action).increment();\
            \
            if (!hasPermission) \{\
                log.warn("Permission denied for user: \{\} on case: \{\} for action: \{\}", \
                        userId, caseId, action);\
                throw new SecurityException("Insufficient permissions");\
            \}\
            \
        \} catch (Exception e) \{\
            log.error("Error during permission validation", e);\
            execution.setVariable("permissionError", e.getMessage());\
            throw new RuntimeException("Permission validation failed", e);\
        \}\
    \}\
    \
    /**\
     * Check RBAC permissions from PostgreSQL\
     */\
    private boolean checkRBACPermissions(String userId, String action) \{\
        String sql = "SELECT entitlements.check_permission(?, ?, ?, ?::jsonb)";\
        \
        try (Connection conn = dataSource.getConnection();\
             PreparedStatement stmt = conn.prepareStatement(sql)) \{\
            \
            stmt.setObject(1, UUID.fromString(userId));\
            stmt.setString(2, "case");\
            stmt.setString(3, action);\
            stmt.setString(4, "\{\}"); // context\
            \
            try (ResultSet rs = stmt.executeQuery()) \{\
                if (rs.next()) \{\
                    return rs.getBoolean(1);\
                \}\
            \}\
        \} catch (SQLException e) \{\
            log.error("RBAC check failed", e);\
            return false;\
        \}\
        return false;\
    \}\
    \
    /**\
     * Check ABAC permissions using Cerbos\
     */\
    private boolean checkABACPermissions(String userId, String caseId, \
                                         String action, DelegateExecution execution) \{\
        try \{\
            // Build principal\
            Map<String, Object> principalAttrs = new HashMap<>();\
            principalAttrs.put("id", userId);\
            principalAttrs.put("department", execution.getVariable("userDepartment"));\
            principalAttrs.put("role", execution.getVariable("userRole"));\
            principalAttrs.put("groups", execution.getVariable("userGroups"));\
            \
            // Build resource\
            Map<String, Object> resourceAttrs = new HashMap<>();\
            resourceAttrs.put("id", caseId);\
            resourceAttrs.put("status", execution.getVariable("caseStatus"));\
            resourceAttrs.put("department", execution.getVariable("caseDepartment"));\
            resourceAttrs.put("assigneeIds", execution.getVariable("assigneeIds"));\
            resourceAttrs.put("priority", execution.getVariable("priority"));\
            resourceAttrs.put("confidentialityLevel", execution.getVariable("confidentialityLevel"));\
            \
            CheckResult result = cerbosClient.check(\
                Principal.newInstance(userId, "user")\
                    .withAttributes(principalAttrs),\
                Resource.newInstance("case", caseId)\
                    .withAttributes(resourceAttrs),\
                action\
            ).get();\
            \
            return result.isAllowed(action);\
            \
        \} catch (Exception e) \{\
            log.error("ABAC check failed", e);\
            return false;\
        \}\
    \}\
\}\
\
/**\
 * Task Assignment Listener with Load Balancing\
 */\
@Service\
@Slf4j\
public class InvestigationAssignmentListener implements TaskListener \{\
    \
    @Autowired\
    private TaskService taskService;\
    \
    @Autowired\
    private UserWorkloadService workloadService;\
    \
    @Autowired\
    private NotificationService notificationService;\
    \
    @Override\
    public void notify(DelegateTask delegateTask) \{\
        if (TaskListener.EVENTNAME_CREATE.equals(delegateTask.getEventName())) \{\
            String department = (String) delegateTask.getVariable("caseDepartment");\
            String priority = (String) delegateTask.getVariable("priority");\
            \
            // Find best investigator based on workload\
            Optional<String> investigator = workloadService.findBestInvestigator(\
                department, priority\
            );\
            \
            investigator.ifPresent(userId -> \{\
                delegateTask.setAssignee(userId);\
                delegateTask.setVariable("autoAssigned", true);\
                delegateTask.setVariable("assignmentTimestamp", new Date());\
                \
                // Send notification\
                notificationService.notifyAssignment(userId, delegateTask.getId());\
                \
                log.info("Auto-assigned task \{\} to investigator \{\}", \
                        delegateTask.getId(), userId);\
            \});\
        \}\
    \}\
\}\
\
/**\
 * Escalation Service for Overdue Tasks\
 */\
@Service\
@Slf4j\
public class EscalationService implements JavaDelegate \{\
    \
    @Autowired\
    private RuntimeService runtimeService;\
    \
    @Autowired\
    private TaskService taskService;\
    \
    @Autowired\
    private NotificationService notificationService;\
    \
    @Override\
    public void execute(DelegateExecution execution) \{\
        String taskId = (String) execution.getVariable("taskId");\
        String escalationLevel = (String) execution.getVariable("escalationLevel");\
        \
        if (escalationLevel == null) \{\
            escalationLevel = "LEVEL_1";\
        \} else if ("LEVEL_1".equals(escalationLevel)) \{\
            escalationLevel = "LEVEL_2";\
        \} else \{\
            escalationLevel = "LEVEL_3";\
        \}\
        \
        execution.setVariable("escalationLevel", escalationLevel);\
        execution.setVariable("escalationTimestamp", new Date());\
        \
        // Update priority\
        String currentPriority = (String) execution.getVariable("priority");\
        if ("medium".equals(currentPriority)) \{\
            execution.setVariable("priority", "high");\
        \} else if ("low".equals(currentPriority)) \{\
            execution.setVariable("priority", "medium");\
        \}\
        \
        // Notify management\
        String managerId = findEscalationManager(escalationLevel, execution);\
        notificationService.sendEscalationAlert(managerId, taskId, escalationLevel);\
        \
        // Signal the process\
        runtimeService.signalEventReceived("signal_case_escalation", execution.getId());\
        \
        log.warn("Task \{\} escalated to \{\} - assigned to manager \{\}", \
                taskId, escalationLevel, managerId);\
    \}\
    \
    private String findEscalationManager(String level, DelegateExecution execution) \{\
        String department = (String) execution.getVariable("caseDepartment");\
        \
        String sql = """\
            SELECT u.user_id \
            FROM entitlements.users u\
            JOIN entitlements.user_roles ur ON u.user_id = ur.user_id\
            JOIN entitlements.roles r ON ur.role_id = r.role_id\
            WHERE u.department = ?\
            AND r.role_name = ?\
            AND u.is_active = true\
            ORDER BY (SELECT COUNT(*) FROM flowable_task WHERE assignee = u.username)\
            LIMIT 1\
        """;\
        \
        String roleNeeded = switch(level) \{\
            case "LEVEL_1" -> "SUPERVISOR";\
            case "LEVEL_2" -> "MANAGER";\
            case "LEVEL_3" -> "DIRECTOR";\
            default -> "MANAGER";\
        \};\
        \
        // Implementation would query database\
        return "manager-" + UUID.randomUUID();\
    \}\
\}\
\
/**\
 * Metrics Collection Service\
 */\
@Service\
@Slf4j\
public class MetricsCollector implements ExecutionListener \{\
    \
    @Autowired\
    private MeterRegistry meterRegistry;\
    \
    @Autowired\
    private DataSource dataSource;\
    \
    @Override\
    public void notify(DelegateExecution execution) \{\
        String eventName = execution.getEventName();\
        String processId = execution.getProcessInstanceId();\
        \
        // Record process metrics\
        meterRegistry.counter("workflow.process.events",\
            "event", eventName,\
            "process", execution.getProcessDefinitionId()\
        ).increment();\
        \
        // Calculate and record duration for completed processes\
        if ("end".equals(eventName)) \{\
            Date startTime = (Date) execution.getVariable("processStartTime");\
            if (startTime != null) \{\
                long duration = System.currentTimeMillis() - startTime.getTime();\
                meterRegistry.timer("workflow.process.duration",\
                    "process", execution.getProcessDefinitionId()\
                ).record(duration, TimeUnit.MILLISECONDS);\
            \}\
        \}\
        \
        // Store in database for reporting\
        storeMetrics(execution);\
    \}\
    \
    private void storeMetrics(DelegateExecution execution) \{\
        String sql = """\
            INSERT INTO workflow_metrics \
            (process_instance_id, event_type, timestamp, variables, user_id)\
            VALUES (?, ?, ?, ?::jsonb, ?)\
        """;\
        \
        try (Connection conn = dataSource.getConnection();\
             PreparedStatement stmt = conn.prepareStatement(sql)) \{\
            \
            stmt.setString(1, execution.getProcessInstanceId());\
            stmt.setString(2, execution.getEventName());\
            stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));\
            stmt.setString(4, objectMapper.writeValueAsString(execution.getVariables()));\
            stmt.setString(5, (String) execution.getVariable("initiator"));\
            \
            stmt.executeUpdate();\
        \} catch (Exception e) \{\
            log.error("Failed to store metrics", e);\
        \}\
    \}\
\}\
\
/**\
 * Audit Listener for Compliance\
 */\
@Service\
@Slf4j\
public class AuditListener implements TaskListener \{\
    \
    @Autowired\
    private AuditService auditService;\
    \
    @Override\
    public void notify(DelegateTask delegateTask) \{\
        AuditEntry entry = AuditEntry.builder()\
            .taskId(delegateTask.getId())\
            .taskName(delegateTask.getName())\
            .eventType(delegateTask.getEventName())\
            .userId(delegateTask.getAssignee())\
            .timestamp(new Date())\
            .processInstanceId(delegateTask.getProcessInstanceId())\
            .variables(new HashMap<>(delegateTask.getVariables()))\
            .build();\
        \
        auditService.recordAudit(entry);\
        \
        // Check for sensitive operations\
        if (isSensitiveOperation(delegateTask)) \{\
            auditService.flagForReview(entry);\
        \}\
    \}\
    \
    private boolean isSensitiveOperation(DelegateTask task) \{\
        String taskName = task.getName();\
        return taskName.contains("Approve") || \
               taskName.contains("Close") || \
               taskName.contains("Delete");\
    \}\
\}\
\
/**\
 * User Workload Service for Load Balancing\
 */\
@Service\
@Slf4j\
public class UserWorkloadService \{\
    \
    @Autowired\
    private DataSource dataSource;\
    \
    @Autowired\
    private CacheManager cacheManager;\
    \
    public Optional<String> findBestInvestigator(String department, String priority) \{\
        String cacheKey = String.format("best_investigator_%s_%s", department, priority);\
        \
        // Check cache first\
        Cache cache = cacheManager.getCache("investigators");\
        if (cache != null) \{\
            String cached = cache.get(cacheKey, String.class);\
            if (cached != null) \{\
                return Optional.of(cached);\
            \}\
        \}\
        \
        String sql = """\
            WITH user_workload AS (\
                SELECT \
                    u.user_id,\
                    u.username,\
                    COUNT(DISTINCT t.id_) as active_tasks,\
                    COUNT(DISTINCT CASE WHEN t.priority_ > 75 THEN t.id_ END) as high_priority_tasks,\
                    AVG(EXTRACT(EPOCH FROM (NOW() - t.create_time_))/3600) as avg_task_age_hours\
                FROM entitlements.users u\
                LEFT JOIN act_ru_task t ON u.username = t.assignee_\
                WHERE u.department = ?\
                AND u.is_active = true\
                AND EXISTS (\
                    SELECT 1 FROM entitlements.user_roles ur\
                    JOIN entitlements.roles r ON ur.role_id = r.role_id\
                    WHERE ur.user_id = u.user_id\
                    AND r.role_name LIKE '%INVESTIGATOR%'\
                )\
                GROUP BY u.user_id, u.username\
            )\
            SELECT username\
            FROM user_workload\
            ORDER BY \
                CASE ? \
                    WHEN 'critical' THEN active_tasks * 0.5\
                    WHEN 'high' THEN active_tasks * 0.7\
                    ELSE active_tasks\
                END ASC,\
                high_priority_tasks ASC,\
                avg_task_age_hours DESC\
            LIMIT 1\
        """;\
        \
        try (Connection conn = dataSource.getConnection();\
             PreparedStatement stmt = conn.prepareStatement(sql)) \{\
            \
            stmt.setString(1, department);\
            stmt.setString(2, priority);\
            \
            try (ResultSet rs = stmt.executeQuery()) \{\
                if (rs.next()) \{\
                    String username = rs.getString("username");\
                    \
                    // Cache the result for 5 minutes\
                    if (cache != null) \{\
                        cache.put(cacheKey, username);\
                    \}\
                    \
                    return Optional.of(username);\
                \}\
            \}\
        \} catch (SQLException e) \{\
            log.error("Failed to find best investigator", e);\
        \}\
        \
        return Optional.empty();\
    \}\
    \
    public WorkloadStats getUserWorkloadStats(String userId) \{\
        String sql = """\
            SELECT \
                COUNT(DISTINCT t.id_) as total_tasks,\
                COUNT(DISTINCT CASE WHEN t.priority_ > 75 THEN t.id_ END) as high_priority,\
                COUNT(DISTINCT CASE WHEN t.due_date_ < NOW() THEN t.id_ END) as overdue,\
                AVG(EXTRACT(EPOCH FROM (NOW() - t.create_time_))/86400) as avg_age_days\
            FROM act_ru_task t\
            WHERE t.assignee_ = (SELECT username FROM entitlements.users WHERE user_id = ?)\
        """;\
        \
        try (Connection conn = dataSource.getConnection();\
             PreparedStatement stmt = conn.prepareStatement(sql)) \{\
            \
            stmt.setObject(1, UUID.fromString(userId));\
            \
            try (ResultSet rs = stmt.executeQuery()) \{\
                if (rs.next()) \{\
                    return WorkloadStats.builder()\
                        .totalTasks(rs.getInt("total_tasks"))\
                        .highPriorityTasks(rs.getInt("high_priority"))\
                        .overdueTasks(rs.getInt("overdue"))\
                        .averageTaskAge(rs.getDouble("avg_age_days"))\
                        .build();\
                \}\
            \}\
        \} catch (SQLException e) \{\
            log.error("Failed to get workload stats", e);\
        \}\
        \
        return WorkloadStats.empty();\
    \}\
\}\
\
version: '3.8'\
\
services:\
  # PostgreSQL Database with RBAC\
  postgres:\
    image: postgres:15-alpine\
    container_name: onecms-postgres\
    environment:\
      POSTGRES_DB: onecms_workflow\
      POSTGRES_USER: workflow_user\
      POSTGRES_PASSWORD: $\{DB_PASSWORD:-SecurePass123!\}\
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=en_US.utf8"\
    volumes:\
      - postgres_data:/var/lib/postgresql/data\
      - ./sql/init:/docker-entrypoint-initdb.d\
    ports:\
      - "5432:5432"\
    healthcheck:\
      test: ["CMD-SHELL", "pg_isready -U workflow_user -d onecms_workflow"]\
      interval: 10s\
      timeout: 5s\
      retries: 5\
    networks:\
      - onecms-network\
\
  # Cerbos for ABAC\
  cerbos:\
    image: ghcr.io/cerbos/cerbos:0.14.0\
    container_name: onecms-cerbos\
    ports:\
      - "3592:3592"  # gRPC port\
      - "3593:3593"  # HTTP port\
    volumes:\
      - ./cerbos/policies:/policies\
      - ./cerbos/config.yaml:/config.yaml\
    command: ["server", "--config=/config.yaml"]\
    environment:\
      CERBOS_LOG_LEVEL: INFO\
      CERBOS_TELEMETRY_DISABLED: "true"\
    healthcheck:\
      test: ["CMD", "/cerbos", "healthcheck", "ping"]\
      interval: 10s\
      timeout: 5s\
      retries: 5\
    networks:\
      - onecms-network\
\
  # Flowable Engine\
  flowable:\
    image: flowable/flowable-all-in-one:7.2.0\
    container_name: onecms-flowable\
    environment:\
      FLOWABLE_REST_ADMIN_USERNAME: admin\
      FLOWABLE_REST_ADMIN_PASSWORD: $\{FLOWABLE_PASSWORD:-admin\}\
      FLOWABLE_DATABASE_DRIVER_CLASS_NAME: org.postgresql.Driver\
      FLOWABLE_DATABASE_URL: jdbc:postgresql://postgres:5432/onecms_workflow\
      FLOWABLE_DATABASE_USERNAME: workflow_user\
      FLOWABLE_DATABASE_PASSWORD: $\{DB_PASSWORD:-SecurePass123!\}\
      FLOWABLE_IDM_LDAP_ENABLED: "false"\
      FLOWABLE_ASYNC_EXECUTOR_ENABLED: "true"\
      FLOWABLE_ASYNC_EXECUTOR_CORE_POOL_SIZE: 10\
      FLOWABLE_ASYNC_EXECUTOR_MAX_POOL_SIZE: 20\
      FLOWABLE_ASYNC_HISTORY_EXECUTOR_ENABLED: "true"\
      JAVA_OPTS: "-Xmx2g -Xms1g"\
    ports:\
      - "8080:8080"  # REST API\
      - "9090:9090"  # Admin UI\
    depends_on:\
      postgres:\
        condition: service_healthy\
    healthcheck:\
      test: ["CMD", "curl", "-f", "http://localhost:8080/flowable-rest/actuator/health"]\
      interval: 30s\
      timeout: 10s\
      retries: 5\
    volumes:\
      - ./workflows:/opt/flowable/workflows\
      - flowable_data:/opt/flowable/data\
    networks:\
      - onecms-network\
\
  # Redis for Caching\
  redis:\
    image: redis:7-alpine\
    container_name: onecms-redis\
    ports:\
      - "6379:6379"\
    command: redis-server --appendonly yes --maxmemory 512mb --maxmemory-policy allkeys-lru\
    volumes:\
      - redis_data:/data\
    healthcheck:\
      test: ["CMD", "redis-cli", "ping"]\
      interval: 10s\
      timeout: 5s\
      retries: 5\
    networks:\
      - onecms-network\
\
  # Elasticsearch for Audit Logging\
  elasticsearch:\
    image: docker.elastic.co/elasticsearch/elasticsearch:8.10.2\
    container_name: onecms-elasticsearch\
    environment:\
      - discovery.type=single-node\
      - xpack.security.enabled=false\
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"\
      - cluster.name=onecms-cluster\
    ports:\
      - "9200:9200"\
    volumes:\
      - elasticsearch_data:/usr/share/elasticsearch/data\
    healthcheck:\
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]\
      interval: 30s\
      timeout: 10s\
      retries: 5\
    networks:\
      - onecms-network\
\
  # Kibana for Log Visualization\
  kibana:\
    image: docker.elastic.co/kibana/kibana:8.10.2\
    container_name: onecms-kibana\
    environment:\
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200\
      - xpack.security.enabled=false\
    ports:\
      - "5601:5601"\
    depends_on:\
      elasticsearch:\
        condition: service_healthy\
    networks:\
      - onecms-network\
\
  # RabbitMQ for Event Bus\
  rabbitmq:\
    image: rabbitmq:3.12-management-alpine\
    container_name: onecms-rabbitmq\
    environment:\
      RABBITMQ_DEFAULT_USER: $\{RABBITMQ_USER:-workflow\}\
      RABBITMQ_DEFAULT_PASS: $\{RABBITMQ_PASSWORD:-SecurePass123!\}\
      RABBITMQ_DEFAULT_VHOST: onecms\
    ports:\
      - "5672:5672"   # AMQP port\
      - "15672:15672" # Management UI\
    volumes:\
      - rabbitmq_data:/var/lib/rabbitmq\
    healthcheck:\
      test: ["CMD", "rabbitmq-diagnostics", "ping"]\
      interval: 10s\
      timeout: 5s\
      retries: 5\
    networks:\
      - onecms-network\
\
  # Prometheus for Metrics\
  prometheus:\
    image: prom/prometheus:latest\
    container_name: onecms-prometheus\
    command:\
      - '--config.file=/etc/prometheus/prometheus.yml'\
      - '--storage.tsdb.path=/prometheus'\
    ports:\
      - "9091:9090"\
    volumes:\
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml\
      - prometheus_data:/prometheus\
    networks:\
      - onecms-network\
\
  # Grafana for Dashboards\
  grafana:\
    image: grafana/grafana:latest\
    container_name: onecms-grafana\
    environment:\
      GF_SECURITY_ADMIN_USER: $\{GRAFANA_USER:-admin\}\
      GF_SECURITY_ADMIN_PASSWORD: $\{GRAFANA_PASSWORD:-admin\}\
      GF_INSTALL_PLUGINS: grafana-clock-panel,grafana-simple-json-datasource\
    ports:\
      - "3000:3000"\
    volumes:\
      - grafana_data:/var/lib/grafana\
      - ./monitoring/grafana/dashboards:/etc/grafana/provisioning/dashboards\
      - ./monitoring/grafana/datasources:/etc/grafana/provisioning/datasources\
    depends_on:\
      - prometheus\
      - elasticsearch\
    networks:\
      - onecms-network\
\
  # Java Microservice Application\
  workflow-service:\
    build:\
      context: ./backend\
      dockerfile: Dockerfile\
    container_name: onecms-workflow-service\
    environment:\
      SPRING_PROFILES_ACTIVE: docker\
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/onecms_workflow\
      SPRING_DATASOURCE_USERNAME: workflow_user\
      SPRING_DATASOURCE_PASSWORD: $\{DB_PASSWORD:-SecurePass123!\}\
      SPRING_REDIS_HOST: redis\
      SPRING_REDIS_PORT: 6379\
      FLOWABLE_REST_URL: http://flowable:8080/flowable-rest\
      CERBOS_URL: cerbos:3592\
      SPRING_RABBITMQ_HOST: rabbitmq\
      SPRING_RABBITMQ_PORT: 5672\
      SPRING_RABBITMQ_USERNAME: $\{RABBITMQ_USER:-workflow\}\
      SPRING_RABBITMQ_PASSWORD: $\{RABBITMQ_PASSWORD:-SecurePass123!\}\
      SPRING_ELASTICSEARCH_REST_URIS: http://elasticsearch:9200\
      SERVER_PORT: 8081\
      JAVA_OPTS: "-Xmx1g -Xms512m"\
    ports:\
      - "8081:8081"\
    depends_on:\
      postgres:\
        condition: service_healthy\
      redis:\
        condition: service_healthy\
      cerbos:\
        condition: service_healthy\
      flowable:\
        condition: service_healthy\
      rabbitmq:\
        condition: service_healthy\
    healthcheck:\
      test: ["CMD", "curl", "-f", "http://localhost:8081/actuator/health"]\
      interval: 30s\
      timeout: 10s\
      retries: 5\
    volumes:\
      - ./backend/logs:/app/logs\
    networks:\
      - onecms-network\
\
  # React Frontend\
  frontend:\
    build:\
      context: ./frontend\
      dockerfile: Dockerfile\
    container_name: onecms-frontend\
    environment:\
      REACT_APP_API_URL: http://workflow-service:8081\
      REACT_APP_WS_URL: ws://workflow-service:8081/ws\
      REACT_APP_FLOWABLE_URL: http://flowable:8080\
    ports:\
      - "3001:80"\
    depends_on:\
      - workflow-service\
    networks:\
      - onecms-network\
\
  # Nginx Reverse Proxy\
  nginx:\
    image: nginx:alpine\
    container_name: onecms-nginx\
    ports:\
      - "80:80"\
      - "443:443"\
    volumes:\
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf\
      - ./nginx/ssl:/etc/nginx/ssl\
    depends_on:\
      - frontend\
      - workflow-service\
      - flowable\
    networks:\
      - onecms-network\
\
  # Backup Service\
  postgres-backup:\
    image: postgres:15-alpine\
    container_name: onecms-postgres-backup\
    environment:\
      PGPASSWORD: $\{DB_PASSWORD:-SecurePass123!\}\
    volumes:\
      - ./backups:/backups\
    command: >\
      sh -c "while true; do\
        pg_dump -h postgres -U workflow_user -d onecms_workflow > /backups/backup_$$(date +%Y%m%d_%H%M%S).sql;\
        find /backups -name 'backup_*.sql' -mtime +7 -delete;\
        sleep 86400;\
      done"\
    depends_on:\
      postgres:\
        condition: service_healthy\
    networks:\
      - onecms-network\
\
volumes:\
  postgres_data:\
    driver: local\
  flowable_data:\
    driver: local\
  redis_data:\
    driver: local\
  elasticsearch_data:\
    driver: local\
  rabbitmq_data:\
    driver: local\
  prometheus_data:\
    driver: local\
  grafana_data:\
    driver: local\
\
networks:\
  onecms-network:\
    driver: bridge\
    ipam:\
      config:\
        - subnet: 172.25.0.0/16\
\
\
Multi-tenant Application architecture:\
\
package com.nextgen.workflow.config;\
\
import org.flowable.common.engine.impl.cfg.multitenant.TenantInfoHolder;\
import org.flowable.engine.ProcessEngine;\
import org.flowable.engine.ProcessEngineConfiguration;\
import org.flowable.spring.SpringProcessEngineConfiguration;\
import org.flowable.spring.boot.EngineConfigurationConfigurer;\
import org.springframework.beans.factory.annotation.Autowired;\
import org.springframework.boot.context.properties.ConfigurationProperties;\
import org.springframework.context.annotation.Bean;\
import org.springframework.context.annotation.Configuration;\
import org.springframework.core.task.TaskExecutor;\
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;\
import org.springframework.transaction.PlatformTransactionManager;\
\
import javax.sql.DataSource;\
import java.util.Collection;\
import java.util.HashMap;\
import java.util.Map;\
\
/**\
 * Enhanced Multi-Tenant Workflow Configuration\
 * Combines Spring Boot Starter convenience with custom multi-tenant support\
 */\
@Configuration\
public class EnhancedWorkflowConfiguration \{\
\
    @Autowired\
    private DataSource dataSource;\
\
    @Autowired\
    private PlatformTransactionManager transactionManager;\
\
    /**\
     * Configure the Flowable Process Engine for multi-tenancy\
     * Uses shared database with tenant isolation\
     */\
    @Bean\
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> multiTenantConfigurer() \{\
        return engineConfiguration -> \{\
            // Enable multi-tenancy with shared database\
            engineConfiguration.setMultiSchemaMultiTenant(false);\
            engineConfiguration.setTenantInfoHolder(tenantInfoHolder());\
            \
            // Configure async executors for better performance\
            engineConfiguration.setAsyncExecutorActivate(true);\
            engineConfiguration.setAsyncExecutor(asyncExecutor());\
            \
            // Configure history for audit compliance\
            engineConfiguration.setHistoryLevel(ProcessEngineConfiguration.HISTORY_FULL);\
            engineConfiguration.setAsyncHistoryEnabled(true);\
            engineConfiguration.setAsyncHistoryExecutorActivate(true);\
            \
            // Enable event listeners for queue management\
            engineConfiguration.setEventListeners(Arrays.asList(\
                taskCreatedEventListener(),\
                taskCompletedEventListener(),\
                processStartedEventListener()\
            ));\
            \
            // Configure custom deployers\
            engineConfiguration.setCustomPreDeployers(Arrays.asList(\
                tenantAwareDeployer()\
            ));\
            \
            // Performance optimizations\
            engineConfiguration.setDatabaseSchemaUpdate("true");\
            engineConfiguration.setDbIdentityUsed(false);\
            engineConfiguration.setDatabaseTablePrefix("ACT_");\
            \
            // Enable bulk insert for better performance\
            engineConfiguration.setBulkInsertEnabled(true);\
            engineConfiguration.setUseParallelMultiInstanceExecution(true);\
            \
            // Configure job execution\
            engineConfiguration.setDefaultFailedJobWaitTime(10);\
            engineConfiguration.setAsyncFailedJobWaitTime(10);\
        \};\
    \}\
\
    /**\
     * Tenant Info Holder for managing tenant context\
     */\
    @Bean\
    public TenantInfoHolder tenantInfoHolder() \{\
        return new ThreadLocalTenantInfoHolder();\
    \}\
\
    /**\
     * Custom Async Executor for better task processing\
     */\
    @Bean\
    public TaskExecutor asyncExecutor() \{\
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();\
        executor.setCorePoolSize(10);\
        executor.setMaxPoolSize(50);\
        executor.setQueueCapacity(100);\
        executor.setThreadNamePrefix("flowable-async-");\
        executor.setAwaitTerminationSeconds(30);\
        executor.setWaitForTasksToCompleteOnShutdown(true);\
        executor.initialize();\
        return executor;\
    \}\
\
    /**\
     * Task Created Event Listener for Queue Management\
     */\
    @Bean\
    public TaskCreatedEventListener taskCreatedEventListener() \{\
        return new TaskCreatedEventListener(queueTaskService());\
    \}\
\
    /**\
     * Queue Task Service for managing task queues\
     */\
    @Bean\
    public QueueTaskService queueTaskService() \{\
        return new QueueTaskService();\
    \}\
\
    /**\
     * Tenant-Aware Deployer\
     */\
    @Bean\
    public TenantAwareDeployer tenantAwareDeployer() \{\
        return new TenantAwareDeployer();\
    \}\
\
    /**\
     * Workflow Metadata Service with caching\
     */\
    @Bean\
    public WorkflowMetadataService workflowMetadataService() \{\
        return new CachedWorkflowMetadataService();\
    \}\
\
    /**\
     * Process Instance Service with tenant isolation\
     */\
    @Bean\
    public ProcessInstanceService processInstanceService(\
            ProcessEngine processEngine,\
            WorkflowMetadataService metadataService) \{\
        return new TenantAwareProcessInstanceService(\
            processEngine.getRuntimeService(),\
            processEngine.TaskService(),\
            processEngine.HistoryService(),\
            metadataService\
        );\
    \}\
\}\
\
/**\
 * Thread-Local based Tenant Info Holder\
 */\
class ThreadLocalTenantInfoHolder implements TenantInfoHolder \{\
    \
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();\
    private static final ThreadLocal<Map<String, Object>> currentTenantData = new ThreadLocal<>();\
    \
    @Override\
    public String getCurrentTenantId() \{\
        return currentTenant.get();\
    \}\
    \
    @Override\
    public void setCurrentTenantId(String tenantId) \{\
        currentTenant.set(tenantId);\
    \}\
    \
    @Override\
    public void clearCurrentTenantId() \{\
        currentTenant.remove();\
        currentTenantData.remove();\
    \}\
    \
    public void setCurrentTenantData(Map<String, Object> data) \{\
        currentTenantData.set(data);\
    \}\
    \
    public Map<String, Object> getCurrentTenantData() \{\
        return currentTenantData.get();\
    \}\
\}\
\
/**\
 * Tenant-Aware Process Instance Service\
 */\
class TenantAwareProcessInstanceService implements ProcessInstanceService \{\
    \
    private final RuntimeService runtimeService;\
    private final TaskService taskService;\
    private final HistoryService historyService;\
    private final WorkflowMetadataService metadataService;\
    \
    public TenantAwareProcessInstanceService(\
            RuntimeService runtimeService,\
            TaskService taskService,\
            HistoryService historyService,\
            WorkflowMetadataService metadataService) \{\
        this.runtimeService = runtimeService;\
        this.taskService = taskService;\
        this.historyService = historyService;\
        this.metadataService = metadataService;\
    \}\
    \
    @Override\
    public ProcessInstance startProcess(StartProcessRequest request) \{\
        String tenantId = TenantContextHolder.getCurrentTenantId();\
        \
        // Validate workflow metadata for tenant\
        WorkflowMetadata metadata = metadataService.findByProcessDefinitionKey(\
            request.getProcessDefinitionKey(), \
            tenantId\
        );\
        \
        if (metadata == null || !metadata.isActive()) \{\
            throw new WorkflowException("Process definition not available for tenant");\
        \}\
        \
        // Start process with tenant context\
        Map<String, Object> variables = new HashMap<>(request.getVariables());\
        variables.put("tenantId", tenantId);\
        variables.put("businessApp", request.getBusinessApp());\
        \
        ProcessInstance processInstance = runtimeService\
            .createProcessInstanceBuilder()\
            .processDefinitionKey(request.getProcessDefinitionKey())\
            .tenantId(tenantId)\
            .businessKey(request.getBusinessKey())\
            .variables(variables)\
            .start();\
        \
        // Create initial queue tasks based on metadata\
        createQueueTasks(processInstance, metadata);\
        \
        return processInstance;\
    \}\
    \
    private void createQueueTasks(ProcessInstance processInstance, WorkflowMetadata metadata) \{\
        // Implementation for creating queue tasks based on workflow metadata\
        List<Task> tasks = taskService.createTaskQuery()\
            .processInstanceId(processInstance.getId())\
            .list();\
        \
        for (Task task : tasks) \{\
            String queueName = metadata.getQueueMapping(task.getTaskDefinitionKey());\
            if (queueName != null) \{\
                queueTaskService.createQueueTask(task, queueName);\
            \}\
        \}\
    \}\
\}\
\
\
\pard\pardeftab720\sa321\partightenfactor0

\f1\b\fs48 \cf0 \expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Architectural Improvements for NextGen Workflow\
\pard\pardeftab720\sa298\partightenfactor0

\fs36 \cf0 1. Multi-Tenancy Strategy\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 Current State Analysis\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls1\ilvl0
\f2\b0\fs24 \cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Your system uses shared schema with 
\f3\fs26 businessApp
\f2\fs24  as tenant discriminator\
\ls1\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Manual queue management with custom 
\f3\fs26 queue_tasks
\f2\fs24  table\
\ls1\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Basic workflow metadata management\
\pard\pardeftab720\sa280\partightenfactor0

\f1\b\fs28 \cf0 Recommended Improvements\
\pard\pardeftab720\sa319\partightenfactor0

\fs24 \cf0 A. Enhanced Tenant Isolation\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 // Implement proper tenant context propagation\
@Component\
public class TenantInterceptor implements HandlerInterceptor \{\
    @Override\
    public boolean preHandle(HttpServletRequest request, \
                           HttpServletResponse response, \
                           Object handler) \{\
        String tenantId = extractTenantFromRequest(request);\
        TenantContextHolder.setCurrentTenant(tenantId);\
        return true;\
    \}\
    \
    @Override\
    public void afterCompletion(HttpServletRequest request, \
                               HttpServletResponse response, \
                               Object handler, \
                               Exception ex) \{\
        TenantContextHolder.clear();\
    \}\
\}\
\pard\pardeftab720\sa319\partightenfactor0

\f1\b\fs24 \cf0 B. Tenant-Aware Query Enhancement\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls2\ilvl0
\f2\b0 \cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Add automatic tenant filtering to all Flowable queries\
\ls2\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Implement query interceptors using AOP\
\ls2\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Cache tenant-specific metadata for performance\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 2. Workflow Engine Optimization\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 A. Use Flowable's Built-in Features Better\
\pard\pardeftab720\sa319\partightenfactor0

\fs24 \cf0 Event Registry Integration\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 <!-- Enable Flowable Event Registry for better event handling -->\
<dependency>\
    <groupId>org.flowable</groupId>\
    <artifactId>flowable-event-registry-spring-boot-starter</artifactId>\
</dependency>\
\pard\pardeftab720\sa240\partightenfactor0

\f2\fs24 \cf0 Benefits:\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls3\ilvl0\cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Native event-driven architecture\
\ls3\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Better integration with external systems\
\ls3\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Automatic event correlation\
\pard\pardeftab720\sa319\partightenfactor0

\f1\b \cf0 External Worker Pattern\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls4\ilvl0
\f2\b0 \cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Implement external workers for long-running tasks\
\ls4\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Use Flowable's external task service\
\ls4\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Better scalability and fault tolerance\
\pard\pardeftab720\sa280\partightenfactor0

\f1\b\fs28 \cf0 B. Async Processing Improvements\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 @Configuration\
public class AsyncConfiguration \{\
    \
    @Bean\
    public AsyncExecutor asyncExecutor() \{\
        DefaultAsyncJobExecutor executor = new DefaultAsyncJobExecutor();\
        executor.setDefaultAsyncJobAcquireWaitTimeInMillis(5000);\
        executor.setDefaultTimerJobAcquireWaitTimeInMillis(5000);\
        executor.setCorePoolSize(10);\
        executor.setMaxPoolSize(50);\
        executor.setKeepAliveTime(5000);\
        return executor;\
    \}\
    \
    @Bean\
    public AsyncHistoryExecutor asyncHistoryExecutor() \{\
        DefaultAsyncHistoryJobExecutor executor = new DefaultAsyncHistoryJobExecutor();\
        executor.setCorePoolSize(5);\
        executor.setMaxPoolSize(20);\
        return executor;\
    \}\
\}\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 3. Queue Management Enhancement\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 Current Issues\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls5\ilvl0
\f2\b0\fs24 \cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Manual queue task management\
\ls5\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 No built-in dead letter queue handling\
\ls5\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Limited task routing capabilities\
\pard\pardeftab720\sa280\partightenfactor0

\f1\b\fs28 \cf0 Recommended Solution: Flowable Task Service Extension\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 @Service\
public class EnhancedQueueService \{\
    \
    private final TaskService taskService;\
    private final RuntimeService runtimeService;\
    \
    public void routeTaskToQueue(String taskId, String queueName) \{\
        // Use Flowable's candidate groups for queue management\
        Task task = taskService.createTaskQuery()\
            .taskId(taskId)\
            .singleResult();\
        \
        // Set queue as candidate group\
        taskService.addCandidateGroup(taskId, queueName);\
        \
        // Set custom properties for queue metadata\
        taskService.setVariableLocal(taskId, "queueName", queueName);\
        taskService.setVariableLocal(taskId, "queuedAt", new Date());\
        \
        // Trigger queue event\
        runtimeService.dispatchEvent(\
            FlowableEventBuilder.createCustomEvent(\
                "TASK_QUEUED",\
                taskId,\
                queueName\
            )\
        );\
    \}\
    \
    public List<Task> getQueueTasks(String queueName, int maxResults) \{\
        return taskService.createTaskQuery()\
            .taskCandidateGroup(queueName)\
            .orderByTaskPriority().desc()\
            .orderByTaskCreateTime().asc()\
            .listPage(0, maxResults);\
    \}\
\}\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 4. Deployment Strategy\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 A. Blue-Green Deployment for Workflows\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 @Service\
public class WorkflowDeploymentService \{\
    \
    @Transactional\
    public void deployNewVersion(String processKey, String bpmnXml) \{\
        // Deploy new version without affecting running instances\
        Deployment deployment = repositoryService.createDeployment()\
            .name(processKey + "_v" + System.currentTimeMillis())\
            .tenantId(getCurrentTenantId())\
            .addString(processKey + ".bpmn20.xml", bpmnXml)\
            .enableDuplicateFiltering()\
            .deploy();\
        \
        // Mark as candidate version\
        repositoryService.setDeploymentCategory(\
            deployment.getId(), \
            "CANDIDATE"\
        );\
        \
        // After testing, promote to active\
        promoteDeployment(deployment.getId());\
    \}\
    \
    private void promoteDeployment(String deploymentId) \{\
        // Gradually migrate running instances\
        repositoryService.setDeploymentCategory(deploymentId, "ACTIVE");\
        \
        // Optional: Migrate running instances\
        runtimeService.createProcessInstanceMigrationBuilder()\
            .migrateToProcessDefinition(newProcessDefinitionId)\
            .migrate();\
    \}\
\}\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 5. Performance Optimizations\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 A. Database Optimization\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 -- Additional indexes for better performance\
CREATE INDEX idx_act_ru_task_tenant_queue \
ON ACT_RU_TASK(TENANT_ID_, CATEGORY_) \
WHERE ASSIGNEE_ IS NULL;\
\
CREATE INDEX idx_act_hi_procinst_tenant_endtime \
ON ACT_HI_PROCINST(TENANT_ID_, END_TIME_) \
WHERE END_TIME_ IS NOT NULL;\
\
-- Partitioning for history tables\
ALTER TABLE ACT_HI_ACTINST \
PARTITION BY RANGE (EXTRACT(YEAR_MONTH FROM START_TIME_));\
\pard\pardeftab720\sa280\partightenfactor0

\f1\b\fs28 \cf0 B. Caching Strategy\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 @Configuration\
@EnableCaching\
public class CacheConfiguration \{\
    \
    @Bean\
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) \{\
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()\
            .entryTtl(Duration.ofMinutes(10))\
            .serializeKeysWith(RedisSerializationContext.SerializationPair\
                .fromSerializer(new StringRedisSerializer()))\
            .serializeValuesWith(RedisSerializationContext.SerializationPair\
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));\
        \
        return RedisCacheManager.builder(redisConnectionFactory)\
            .cacheDefaults(config)\
            .withCacheConfiguration("processDefinitions", \
                config.entryTtl(Duration.ofHours(1)))\
            .withCacheConfiguration("workflowMetadata", \
                config.entryTtl(Duration.ofHours(1)))\
            .build();\
    \}\
\}\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 6. Monitoring and Observability\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 A. Custom Metrics\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 @Component\
public class WorkflowMetrics \{\
    \
    private final MeterRegistry meterRegistry;\
    \
    @EventHandler\
    public void handleProcessStarted(FlowableEngineEvent event) \{\
        meterRegistry.counter("workflow.process.started",\
            "processDefinitionKey", event.getProcessDefinitionId(),\
            "tenantId", event.getTenantId()\
        ).increment();\
    \}\
    \
    @EventHandler\
    public void handleTaskCompleted(FlowableTaskEvent event) \{\
        meterRegistry.timer("workflow.task.duration",\
            "taskDefinitionKey", event.getTaskDefinitionKey(),\
            "tenantId", event.getTenantId()\
        ).record(Duration.between(\
            event.getTask().getCreateTime().toInstant(),\
            Instant.now()\
        ));\
    \}\
\}\
\pard\pardeftab720\sa280\partightenfactor0

\f1\b\fs28 \cf0 B. Distributed Tracing\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 @Configuration\
public class TracingConfiguration \{\
    \
    @Bean\
    public FlowableTraceInterceptor flowableTraceInterceptor(Tracer tracer) \{\
        return new FlowableTraceInterceptor(tracer);\
    \}\
    \
    @Bean\
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> tracingConfigurer(\
            FlowableTraceInterceptor interceptor) \{\
        return engineConfiguration -> \{\
            engineConfiguration.setCommandInvoker(\
                new TracingCommandInvoker(interceptor, \
                    engineConfiguration.getCommandInvoker())\
            );\
        \};\
    \}\
\}\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 7. Security Enhancements\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 A. Process Definition Security\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 @Service\
public class SecureWorkflowService \{\
    \
    @PreAuthorize("hasPermission(#processDefinitionKey, 'PROCESS', 'DEPLOY')")\
    public void deployProcess(String processDefinitionKey, String bpmnXml) \{\
        // Validate BPMN for security issues\
        validateBpmnSecurity(bpmnXml);\
        \
        // Deploy with security context\
        repositoryService.createDeployment()\
            .addString(processDefinitionKey + ".bpmn20.xml", bpmnXml)\
            .category("SECURE")\
            .deploy();\
    \}\
    \
    private void validateBpmnSecurity(String bpmnXml) \{\
        // Check for script tasks with dangerous code\
        // Validate service task implementations\
        // Ensure proper data access controls\
    \}\
\}\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 8. Testing Strategy\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 A. Integration Testing\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 @SpringBootTest\
@AutoConfigureMockMvc\
@Flowable\
class WorkflowIntegrationTest \{\
    \
    @Autowired\
    private RuntimeService runtimeService;\
    \
    @Test\
    @Deployment(resources = "test-process.bpmn20.xml")\
    @WithMockUser(authorities = "ROLE_USER")\
    void testCompleteWorkflow() \{\
        // Given\
        Map<String, Object> variables = Map.of(\
            "caseId", "TEST-001",\
            "priority", "HIGH"\
        );\
        \
        // When\
        ProcessInstance processInstance = runtimeService\
            .startProcessInstanceByKey("testProcess", variables);\
        \
        // Then\
        assertThat(processInstance).isNotNull();\
        \
        // Complete tasks\
        List<Task> tasks = taskService.createTaskQuery()\
            .processInstanceId(processInstance.getId())\
            .list();\
        \
        assertThat(tasks).hasSize(1);\
        \
        // Complete task\
        taskService.complete(tasks.get(0).getId());\
        \
        // Verify process completed\
        HistoricProcessInstance historicProcess = historyService\
            .createHistoricProcessInstanceQuery()\
            .processInstanceId(processInstance.getId())\
            .singleResult();\
        \
        assertThat(historicProcess.getEndTime()).isNotNull();\
    \}\
\}\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 9. Migration Strategy\
\pard\pardeftab720\sa280\partightenfactor0

\fs28 \cf0 Phase 1: Foundation (Weeks 1-2)\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls6\ilvl0
\f2\b0\fs24 \cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Implement enhanced multi-tenant configuration\
\ls6\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Set up monitoring and metrics\
\ls6\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Enhance security layer\
\pard\pardeftab720\sa280\partightenfactor0

\f1\b\fs28 \cf0 Phase 2: Core Improvements (Weeks 3-4)\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls7\ilvl0
\f2\b0\fs24 \cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Migrate to Flowable Event Registry\
\ls7\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Implement enhanced queue management\
\ls7\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Add caching layer\
\pard\pardeftab720\sa280\partightenfactor0

\f1\b\fs28 \cf0 Phase 3: Advanced Features (Weeks 5-6)\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls8\ilvl0
\f2\b0\fs24 \cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Implement external worker pattern\
\ls8\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Add blue-green deployment\
\ls8\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Set up distributed tracing\
\pard\pardeftab720\sa280\partightenfactor0

\f1\b\fs28 \cf0 Phase 4: Optimization (Weeks 7-8)\
\pard\tx220\tx720\pardeftab720\li720\fi-720\partightenfactor0
\ls9\ilvl0
\f2\b0\fs24 \cf0 \kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Database optimization\
\ls9\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Performance tuning\
\ls9\ilvl0\kerning1\expnd0\expndtw0 \outl0\strokewidth0 {\listtext	\uc0\u8226 	}\expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 Load testing and optimization\
\pard\pardeftab720\sa298\partightenfactor0

\f1\b\fs36 \cf0 10. Recommended Technology Stack Updates\
\pard\pardeftab720\partightenfactor0

\f3\b0\fs26 \cf0 <!-- Add to your pom.xml -->\
<dependencies>\
    <!-- Flowable Event Registry -->\
    <dependency>\
        <groupId>org.flowable</groupId>\
        <artifactId>flowable-event-registry-spring-boot-starter</artifactId>\
        <version>7.2.0</version>\
    </dependency>\
    \
    <!-- Flowable External Job -->\
    <dependency>\
        <groupId>org.flowable</groupId>\
        <artifactId>flowable-external-job-rest</artifactId>\
        <version>7.2.0</version>\
    </dependency>\
    \
    <!-- Micrometer for metrics -->\
    <dependency>\
        <groupId>io.micrometer</groupId>\
        <artifactId>micrometer-registry-prometheus</artifactId>\
    </dependency>\
    \
    <!-- OpenTelemetry for tracing -->\
    <dependency>\
        <groupId>io.opentelemetry</groupId>\
        <artifactId>opentelemetry-spring-boot-starter</artifactId>\
    </dependency>\
    \
    <!-- Resilience4j for circuit breakers -->\
    <dependency>\
        <groupId>io.github.resilience4j</groupId>\
        <artifactId>resilience4j-spring-boot2</artifactId>\
    </dependency>\
</dependencies>\
}