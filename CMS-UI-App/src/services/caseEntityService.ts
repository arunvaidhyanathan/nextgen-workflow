import { apiService } from './api';

export interface CaseEntity {
  id: number;
  entityId: string;
  caseId: string;
  entityType: 'PERSON' | 'ORGANIZATION';
  investigationFunction?: string;
  relationshipType?: string;
  // Person fields
  soeid?: string;
  geid?: string;
  firstName?: string;
  middleName?: string;
  lastName?: string;
  emailAddress?: string;
  phoneNumber?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  // Organization fields
  organizationName?: string;
  organizationType?: string;
  createdAt: string;
  updatedAt: string;
  displayName: string;
}

export interface CaseEntityRequest {
  caseId: string;
  entityType: 'PERSON' | 'ORGANIZATION';
  investigationFunction?: string;
  relationshipType?: string;
  // Person fields
  soeid?: string;
  geid?: string;
  firstName?: string;
  middleName?: string;
  lastName?: string;
  emailAddress?: string;
  phoneNumber?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  // Organization fields
  organizationName?: string;
  organizationType?: string;
}

class CaseEntityService {
  private api = apiService;

  async getEntitiesByCaseId(caseId: string): Promise<CaseEntity[]> {
    return this.api.get<CaseEntity[]>(`/cms/v1/case-entities/case/${caseId}`);
  }

  async getEntitiesByCaseIdAndType(caseId: string, entityType: 'PERSON' | 'ORGANIZATION'): Promise<CaseEntity[]> {
    return this.api.get<CaseEntity[]>(`/cms/v1/case-entities/case/${caseId}/type/${entityType}`);
  }

  async getEntityById(entityId: string): Promise<CaseEntity> {
    return this.api.get<CaseEntity>(`/cms/v1/case-entities/${entityId}`);
  }

  async createEntity(request: CaseEntityRequest): Promise<CaseEntity> {
    return this.api.post<CaseEntity>('/cms/v1/case-entities', request);
  }

  async updateEntity(entityId: string, request: CaseEntityRequest): Promise<CaseEntity> {
    return this.api.put<CaseEntity>(`/cms/v1/case-entities/${entityId}`, request);
  }

  async deleteEntity(entityId: string): Promise<void> {
    return this.api.delete(`/cms/v1/case-entities/${entityId}`);
  }

  async searchEntitiesBySoeid(soeid: string): Promise<CaseEntity[]> {
    return this.api.get<CaseEntity[]>(`/cms/v1/case-entities/search/soeid/${soeid}`);
  }

  async searchEntitiesByEmail(email: string): Promise<CaseEntity[]> {
    return this.api.get<CaseEntity[]>(`/cms/v1/case-entities/search/email/${email}`);
  }
}

export const caseEntityService = new CaseEntityService();