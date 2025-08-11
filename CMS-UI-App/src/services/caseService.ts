import { apiService } from './api';
import { CaseResponse, WorkflowTask, WorkItemDisplayData, AllegationResponse } from '../types/case';

// Constants for status mapping
const TASK_STATUS = {
  INTAKE: 'Intake',
  UNDER_REVIEW: 'Under Review',
  INVESTIGATION: 'Investigation',
  DRAFTED: 'Drafted',
  IN_PROGRESS: 'In Progress',
} as const;

const CASE_SEVERITY = {
  HIGH: 'HIGH',
  CRITICAL: 'CRITICAL',
} as const;

const CASE_TYPE = {
  TRACKED: 'Tracked',
  NON_TRACKED: 'Non-Tracked',
  UNKNOWN: 'Unknown',
} as const;

const FUNCTION_TYPES = {
  HR: 'HR',
  LEGAL: 'LEGAL',
  CSIS: 'CSIS',
  ER: 'ER',
} as const;

const REGIONS = {
  NAM: 'NAM',
  EMEA: 'EMEA',
  APAC: 'APAC',
  UNKNOWN: 'Unknown',
} as const;

const DEFAULT_PRIORITY = 'MEDIUM';
const DEFAULT_REGION = REGIONS.UNKNOWN;

class CaseService {
  async getMyCases(page = 0, size = 20, status?: string): Promise<CaseResponse[]> {
    // Validate input parameters
    if (page < 0 || size < 0) {
      throw new Error('Page and size parameters must be non-negative integers');
    }
    
    if (!Number.isInteger(page) || !Number.isInteger(size)) {
      throw new Error('Page and size parameters must be integers');
    }

    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      
      if (status) {
        params.append('status', status);
      }

      console.log('🔄 Fetching my cases...');
      const cases = await apiService.get<CaseResponse[]>(`/cms/v1/cases/dashboard-cases?${params.toString()}`);
      console.log('✅ Cases fetched:', cases.length, 'cases');
      return cases;
    } catch (error) {
      console.error('❌ Error fetching cases:', error);
      throw error;
    }
  }

  async getMyTasks(): Promise<WorkflowTask[]> {
    try {
      console.log('🔄 Fetching my tasks...');
      const tasks = await apiService.get<WorkflowTask[]>('/workflow/my-tasks');
      console.log('✅ Tasks fetched:', tasks.length, 'tasks');
      return tasks;
    } catch (error) {
      console.error('❌ Error fetching tasks:', error);
      throw error;
    }
  }

  async getCaseDetails(caseNumber: string): Promise<CaseResponse> {
    return apiService.get<CaseResponse>(`/cms/v1/cases/${caseNumber}`);
  }

  async getCaseDetailsWithFallback(caseNumber: string): Promise<CaseResponse | null> {
    try {
      // Try the individual case endpoint first
      console.log(`🔄 Attempting to fetch case details for: ${caseNumber}`);
      const result = await this.getCaseDetails(caseNumber);
      console.log('✅ Individual case endpoint succeeded');
      return result;
    } catch (error) {
      console.warn('⚠️ Individual case endpoint failed, trying dashboard endpoint:', error);
      // Fallback: Get all cases and find the specific one
      try {
        console.log('🔄 Fetching from dashboard endpoint as fallback...');
        const allCases = await this.getMyCases(0, 100); // Get more cases to find the one we need
        console.log(`📋 Dashboard returned ${allCases.length} cases, searching for case: ${caseNumber}`);
        
        const foundCase = allCases.find(c => {
          const match = c.caseNumber === caseNumber || c.caseId === caseNumber;
          if (match) {
            console.log(`✅ Found matching case: ${c.caseNumber} (ID: ${c.caseId})`);
          }
          return match;
        });
        
        if (!foundCase) {
          console.warn(`❌ Case ${caseNumber} not found in dashboard results`);
          console.log('Available cases:', allCases.map(c => ({ number: c.caseNumber, id: c.caseId })));
        }
        
        return foundCase || null;
      } catch (dashboardError) {
        console.error('❌ Dashboard endpoint also failed:', dashboardError);
        return null;
      }
    }
  }

  async getTasksForCase(caseId: string): Promise<WorkflowTask[]> {
    return apiService.get<WorkflowTask[]>(`/workflow/cases/${caseId}/tasks`);
  }

  async assignTask(taskId: string, userId: string): Promise<string> {
    return apiService.post<string>(`/workflow/tasks/${taskId}/assign?userId=${userId}`);
  }

  async completeTask(taskId: string, variables?: Record<string, any>): Promise<string> {
    return apiService.post<string>(`/workflow/tasks/${taskId}/complete`, variables);
  }

  async createCase(caseData: any): Promise<CaseResponse> {
    try {
      console.log('🔄 Creating new case...', caseData);
      const response = await apiService.post<CaseResponse>('/cms/v1/cases', caseData);
      console.log('✅ Case created successfully:', response);
      return response;
    } catch (error) {
      console.error('❌ Error creating case:', error);
      throw error;
    }
  }

  async testAuth(): Promise<any> {
    try {
      console.log('🔄 Testing authentication...');
      const result = await apiService.get('/cms/v1/cases/auth-test');
      console.log('✅ Auth test result:', result);
      return result;
    } catch (error) {
      console.error('❌ Auth test failed:', error);
      throw error;
    }
  }

  async submitCase(caseNumber: string): Promise<any> {
    try {
      console.log('🔄 Submitting case:', caseNumber);
      const result = await apiService.post(`/cms/v1/cases/${caseNumber}/submit`);
      console.log('✅ Case submitted successfully:', result);
      return result;
    } catch (error) {
      console.error('❌ Case submission failed:', error);
      throw error;
    }
  }

  async getDashboardStats(): Promise<any> {
    try {
      console.log('🔄 Fetching dashboard stats...');
      const stats = await apiService.get('/cms/v1/cases/dashboard-stats');
      console.log('✅ Dashboard stats fetched:', stats);
      return stats;
    } catch (error) {
      console.error('❌ Error fetching dashboard stats:', error);
      throw error;
    }
  }

  // Transform cases directly to work items for display
  transformCasesToWorkItems(cases: CaseResponse[]): WorkItemDisplayData[] {
    if (!cases || cases.length === 0) {
      return [];
    }

    return cases.map(caseData => {
      const agingDays = this.calculateAgingDays(caseData.createdAt);

      return {
        id: caseData.caseNumber,
        status: this.mapCaseStatusToDisplay(caseData.status),
        caseType: this.determineCaseTypeFromCase(caseData),
        overallAging: agingDays,
        iuAging: agingDays, // For now, same as overall aging
        function: this.mapCaseFunctionFromAllegations(caseData.allegations),
        group: this.mapCaseAssignmentGroup(caseData.assignedTo),
        officer: this.formatOfficerName(caseData.assignedTo),
        dateReported: this.formatDate(caseData.createdAt),
        priority: caseData.priority || DEFAULT_PRIORITY,
        title: caseData.title || 'Untitled Case',
      };
    });
  }

  // Transform backend data to display format for the dashboard table (legacy method)
  transformTasksToWorkItems(tasks: WorkflowTask[], cases: CaseResponse[]): WorkItemDisplayData[] {
    if (!tasks || tasks.length === 0) {
      return [];
    }

    const caseMap = new Map(cases?.map(c => [c.caseNumber, c]) || []);
    
    return tasks.map(task => {
      const caseData = caseMap.get(task.variables?.caseNumber || task.caseId);
      const agingDays = this.calculateAgingDays(task.created);

      // Ensure we have a valid ID
      const taskId = task.variables?.caseNumber || task.caseId || task.taskId;
      if (!taskId) {
        console.error('Task missing valid identifier:', task);
        throw new Error('Task data is missing required identifier fields');
      }

      return {
        id: taskId,
        status: this.mapTaskToStatus(task.taskName),
        caseType: this.determineCaseType(caseData),
        overallAging: agingDays,
        iuAging: agingDays, // For now, same as overall aging
        function: this.mapTaskToFunction(task.taskName, task.candidateGroups),
        group: this.mapCandidateGroupToRegion(task.candidateGroups),
        officer: this.formatOfficerName(task.assignee),
        dateReported: this.formatDate(caseData?.createdAt || task.created),
        priority: caseData?.priority || DEFAULT_PRIORITY,
        title: caseData?.title || task.taskName || 'Untitled Task',
      };
    });
  }

  private calculateAgingDays(createdDateString: string): number {
    try {
      const createdDate = createdDateString ? new Date(createdDateString) : new Date();
      
      // Validate the date
      if (isNaN(createdDate.getTime())) {
        console.warn('Invalid date provided, using current date as fallback:', createdDateString);
        return 0; // Return 0 days for invalid dates
      }

      const now = new Date();
      const timeDiff = now.getTime() - createdDate.getTime();
      const daysDiff = Math.floor(timeDiff / (1000 * 60 * 60 * 24));
      
      return Math.max(0, daysDiff);
    } catch (error) {
      console.error('Error calculating aging days:', error);
      return 0; // Return 0 as safe fallback
    }
  }

  private formatOfficerName(assignee: string | null | undefined): string {
    if (!assignee) return 'Unassigned';
    
    // If it's already formatted with ID, return as is
    if (assignee.includes('(') && assignee.includes(')')) {
      return assignee;
    }
    
    // Otherwise, format it nicely
    return `${assignee} (${assignee.toUpperCase()})`;
  }

  private mapTaskToStatus(taskName: string): string {
    if (!taskName) return TASK_STATUS.IN_PROGRESS;
    
    const lowerTaskName = taskName.toLowerCase();
    if (lowerTaskName.includes('intake')) return TASK_STATUS.INTAKE;
    if (lowerTaskName.includes('review')) return TASK_STATUS.UNDER_REVIEW;
    if (lowerTaskName.includes('investigation')) return TASK_STATUS.INVESTIGATION;
    if (lowerTaskName.includes('draft')) return TASK_STATUS.DRAFTED;
    return TASK_STATUS.IN_PROGRESS;
  }

  private mapCaseStatusToDisplay(status: string): string {
    switch (status) {
      case 'OPEN': return TASK_STATUS.INTAKE;
      case 'IN_PROGRESS': return TASK_STATUS.IN_PROGRESS;
      case 'UNDER_REVIEW': return TASK_STATUS.UNDER_REVIEW;
      case 'CLOSED': return TASK_STATUS.DRAFTED;
      case 'ARCHIVED': return TASK_STATUS.DRAFTED;
      default: return TASK_STATUS.IN_PROGRESS;
    }
  }

  private determineCaseTypeFromCase(caseData: CaseResponse): string {
    // Determine if tracked based on severity or allegations
    const hasHighSeverity = caseData.allegations?.some(a => 
      a.severity === CASE_SEVERITY.HIGH || a.severity === CASE_SEVERITY.CRITICAL
    );
    const hasMultipleDepartments = new Set(
      caseData.allegations?.map(a => a.departmentClassification)
    ).size > 1;

    return hasHighSeverity || hasMultipleDepartments ? CASE_TYPE.TRACKED : CASE_TYPE.NON_TRACKED;
  }

  private mapCaseFunctionFromAllegations(allegations: AllegationResponse[]): string {
    if (!allegations || allegations.length === 0) {
      return FUNCTION_TYPES.ER;
    }

    const functions = new Set<string>();
    
    allegations.forEach(allegation => {
      const assignedGroup = allegation.assignedGroup?.toUpperCase() || '';
      const department = allegation.departmentClassification?.toUpperCase() || '';
      
      if (assignedGroup.includes('HR') || department.includes('HR')) {
        functions.add(FUNCTION_TYPES.HR);
      }
      if (assignedGroup.includes('LEGAL') || department.includes('LEGAL')) {
        functions.add(FUNCTION_TYPES.LEGAL);
      }
      if (assignedGroup.includes('CSIS') || assignedGroup.includes('SECURITY')) {
        functions.add(FUNCTION_TYPES.CSIS);
      }
    });

    return functions.size > 0 ? Array.from(functions).join(', ') : FUNCTION_TYPES.ER;
  }

  private mapCaseAssignmentGroup(assignedTo: string): string {
    if (!assignedTo) return DEFAULT_REGION;
    
    const assignedUpper = assignedTo.toUpperCase();
    if (assignedUpper.includes('NAM') || assignedUpper.includes('NORTH_AMERICA')) return REGIONS.NAM;
    if (assignedUpper.includes('EMEA') || assignedUpper.includes('EUROPE')) return REGIONS.EMEA;
    if (assignedUpper.includes('APAC') || assignedUpper.includes('ASIA')) return REGIONS.APAC;
    
    return DEFAULT_REGION;
  }

  private determineCaseType(caseData?: CaseResponse): string {
    if (!caseData) return CASE_TYPE.UNKNOWN;
    
    // Determine if tracked based on severity or allegations
    const hasHighSeverity = caseData.allegations?.some(a => 
      a.severity === CASE_SEVERITY.HIGH || a.severity === CASE_SEVERITY.CRITICAL
    );
    const hasMultipleDepartments = new Set(
      caseData.allegations?.map(a => a.departmentClassification)
    ).size > 1;

    return hasHighSeverity || hasMultipleDepartments ? CASE_TYPE.TRACKED : CASE_TYPE.NON_TRACKED;
  }

  private mapTaskToFunction(taskName: string, candidateGroups: string): string {
    const functions = [];
    
    if (candidateGroups?.includes('HR') || taskName?.toLowerCase().includes('hr')) {
      functions.push(FUNCTION_TYPES.HR);
    }
    if (candidateGroups?.includes('LEGAL') || taskName?.toLowerCase().includes('legal')) {
      functions.push(FUNCTION_TYPES.LEGAL);
    }
    if (candidateGroups?.includes('CSIS') || candidateGroups?.includes('SECURITY')) {
      functions.push(FUNCTION_TYPES.CSIS);
    }
    if (candidateGroups?.includes('INTAKE') || taskName?.toLowerCase().includes('intake')) {
      functions.push(FUNCTION_TYPES.ER);
    }

    return functions.length > 0 ? functions.join(', ') : FUNCTION_TYPES.ER;
  }

  private mapCandidateGroupToRegion(candidateGroups: string): string {
    if (!candidateGroups) {
      console.warn('Missing candidate groups data - this should be fixed in the backend');
      return DEFAULT_REGION;
    }
    
    // Map based on candidate groups - deterministic mapping
    if (candidateGroups.includes('NAM') || candidateGroups.includes('NORTH_AMERICA')) return REGIONS.NAM;
    if (candidateGroups.includes('EMEA') || candidateGroups.includes('EUROPE')) return REGIONS.EMEA;
    if (candidateGroups.includes('APAC') || candidateGroups.includes('ASIA')) return REGIONS.APAC;
    
    // Log data quality issue and return deterministic fallback
    console.warn('Unable to determine region from candidate groups:', candidateGroups, '- this should be fixed in the backend');
    return DEFAULT_REGION;
  }

  private formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-GB', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }
}

export const caseService = new CaseService();