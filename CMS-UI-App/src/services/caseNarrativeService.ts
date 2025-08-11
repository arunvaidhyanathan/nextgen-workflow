import { apiService } from './api';

export interface CaseNarrative {
  id: number;
  narrativeId: string;
  caseId: string;
  investigationFunction?: string;
  narrativeType?: string;
  narrativeTitle?: string;
  narrativeText: string;
  isRecalled: boolean;
  createdBy?: number;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CaseNarrativeRequest {
  caseId: string;
  investigationFunction?: string;
  narrativeType?: string;
  narrativeTitle?: string;
  narrativeText: string;
  isRecalled?: boolean;
}

export interface NarrativeCounts {
  active: number;
  recalled: number;
  total: number;
}

class CaseNarrativeService {
  private api = apiService;

  async getNarrativesByCaseId(caseId: string): Promise<CaseNarrative[]> {
    return this.api.get<CaseNarrative[]>(`/cms/v1/case-narratives/case/${caseId}`);
  }

  async getActiveNarrativesByCaseId(caseId: string): Promise<CaseNarrative[]> {
    return this.api.get<CaseNarrative[]>(`/cms/v1/case-narratives/case/${caseId}/active`);
  }

  async getNarrativeById(narrativeId: string): Promise<CaseNarrative> {
    return this.api.get<CaseNarrative>(`/cms/v1/case-narratives/${narrativeId}`);
  }

  async createNarrative(request: CaseNarrativeRequest): Promise<CaseNarrative> {
    return this.api.post<CaseNarrative>('/cms/v1/case-narratives', request);
  }

  async updateNarrative(narrativeId: string, request: CaseNarrativeRequest): Promise<CaseNarrative> {
    return this.api.put<CaseNarrative>(`/cms/v1/case-narratives/${narrativeId}`, request);
  }

  async recallNarrative(narrativeId: string): Promise<void> {
    return this.api.patch(`/cms/v1/case-narratives/${narrativeId}/recall`);
  }

  async deleteNarrative(narrativeId: string): Promise<void> {
    return this.api.delete(`/cms/v1/case-narratives/${narrativeId}`);
  }

  async getNarrativesByType(caseId: string, narrativeType: string): Promise<CaseNarrative[]> {
    return this.api.get<CaseNarrative[]>(`/cms/v1/case-narratives/case/${caseId}/type/${narrativeType}`);
  }

  async getNarrativeCounts(caseId: string): Promise<NarrativeCounts> {
    return this.api.get<NarrativeCounts>(`/cms/v1/case-narratives/case/${caseId}/counts`);
  }
}

export const caseNarrativeService = new CaseNarrativeService();