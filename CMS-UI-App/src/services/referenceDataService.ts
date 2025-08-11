import { apiService } from './api';

export interface EscalationMethod {
  id: number;
  methodCode: string;
  methodName: string;
  description?: string;
  isActive: boolean;
}

export interface DataSource {
  id: number;
  sourceCode: string;
  sourceName: string;
  description?: string;
  isActive: boolean;
}

export interface CountryCluster {
  id: number;
  countryCode: string;
  countryName: string;
  clusterName?: string;
  region?: string;
  isActive: boolean;
}

export interface Department {
  id: number;
  departmentId: number;
  departmentCode: string;
  departmentName: string;
  departmentDescription?: string;
  departmentRegion?: string;
  departmentFunction?: string;
  isActive: boolean;
}

class ReferenceDataService {
  private api = apiService;

  async getEscalationMethods(): Promise<EscalationMethod[]> {
    return this.api.get<EscalationMethod[]>('/cms/v1/reference-data/escalation-methods');
  }

  async getDataSources(): Promise<DataSource[]> {
    return this.api.get<DataSource[]>('/cms/v1/reference-data/data-sources');
  }

  async getCountryClusters(): Promise<CountryCluster[]> {
    return this.api.get<CountryCluster[]>('/cms/v1/reference-data/countries-clusters');
  }

  async getCountriesByRegion(region: string): Promise<CountryCluster[]> {
    return this.api.get<CountryCluster[]>(`/cms/v1/reference-data/countries-clusters/region/${region}`);
  }

  async getCountriesByCluster(clusterName: string): Promise<CountryCluster[]> {
    return this.api.get<CountryCluster[]>(`/cms/v1/reference-data/countries-clusters/cluster/${clusterName}`);
  }

  async getDepartments(): Promise<Department[]> {
    return this.api.get<Department[]>('/cms/v1/departments');
  }

  async getAssignmentGroups(): Promise<Department[]> {
    return this.api.get<Department[]>('/cms/v1/departments/assignment-groups');
  }

  async getDepartmentsByRegion(region: string): Promise<Department[]> {
    return this.api.get<Department[]>(`/cms/v1/departments/region/${region}`);
  }

  async getDepartmentsByFunction(functionName: string): Promise<Department[]> {
    return this.api.get<Department[]>(`/cms/v1/departments/function/${functionName}`);
  }

  async getDepartmentsByRegionAndFunction(region: string, functionName: string): Promise<Department[]> {
    return this.api.get<Department[]>(`/cms/v1/departments/region/${region}/function/${functionName}`);
  }
}

export const referenceDataService = new ReferenceDataService();