export interface AllegationResponse {
  allegationId: string;
  allegationType: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  description: string;
  departmentClassification: string;
  assignedGroup: string;
  flowablePlanItemId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CaseResponse {
  caseId: string;
  caseNumber: string;
  title: string;
  description: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'OPEN' | 'IN_PROGRESS' | 'UNDER_REVIEW' | 'CLOSED' | 'ARCHIVED';
  complainantName: string;
  complainantEmail: string;
  workflowInstanceKey: number;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  assignedTo: string;
  allegations: AllegationResponse[];
  occurrenceDate?: string;
  dateReportedToCiti?: string;
  dateReceivedByEscalationChannel?: string;
  howWasComplaintEscalated?: string;
  dataSourceId?: string;
  clusterCountry?: string;
  legalHold?: string;
  lastUpdatedBy?: string;
}

export interface WorkflowTask {
  taskId: string;
  taskName: string;
  description: string;
  processInstanceId: string;
  processDefinitionId: string;
  assignee: string;
  candidateGroups: string;
  taskDefinitionKey: string;
  created: string;
  dueDate: string | null;
  priority: number;
  variables: Record<string, any>;
  formKey: string;
  caseId: string;
  category: string;
}

export interface WorkItemDisplayData {
  id: string;
  status: string;
  caseType: string;
  overallAging: number;
  iuAging: number;
  function: string;
  group: string;
  officer: string;
  dateReported: string;
  priority: string;
  title: string;
}