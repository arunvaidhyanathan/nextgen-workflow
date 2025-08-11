import { authService } from './authService';

// Use relative URLs to leverage Vite proxy in development
// In production, this should be set to the actual backend URL
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

class ApiService {
  private baseURL: string;
  private readonly MAX_RETRIES = 3;
  private readonly RETRY_DELAY = 1000; // 1 second

  constructor() {
    this.baseURL = API_BASE_URL;
  }

  private async delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  private async retryRequest<T>(
    requestFn: () => Promise<T>,
    retries: number = this.MAX_RETRIES
  ): Promise<T> {
    for (let attempt = 1; attempt <= retries; attempt++) {
      try {
        return await requestFn();
      } catch (error: any) {
        // Don't retry auth errors or client errors (4xx except 401)
        if (error.status === 401 || (error.status >= 400 && error.status < 500 && error.status !== 401)) {
          throw error;
        }

        // Don't retry on the last attempt
        if (attempt === retries) {
          throw error;
        }

        // Exponential backoff delay
        const delayMs = this.RETRY_DELAY * Math.pow(2, attempt - 1);
        console.log(`Request failed (attempt ${attempt}/${retries}), retrying in ${delayMs}ms...`);
        await this.delay(delayMs);
      }
    }

    throw new Error('Max retries exceeded');
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {},
    skipAuth: boolean = false
  ): Promise<T> {
    return this.retryRequest(async () => {
      // Use relative URLs to leverage Vite proxy in development
      const url = this.baseURL ? `${this.baseURL}${endpoint}` : `/api${endpoint}`;
      
      const config: RequestInit = {
        headers: {
          'Content-Type': 'application/json',
          ...options.headers,
        },
        ...options,
      };

      // Add session ID and user ID headers if available and not skipped
      if (!skipAuth) {
        const sessionId = authService.getSessionId();
        if (sessionId) {
          config.headers = {
            ...config.headers,
            'X-Session-Id': sessionId,
          };
        }

        // Add user ID header if available (required for workflow service)
        const user = authService.getUser();
        if (user && user.id) {
          config.headers = {
            ...config.headers,
            'X-User-Id': user.id,
          };
        }
      }

      const response = await fetch(url, config);
      
      if (!response.ok) {
        // Handle 401 Unauthorized - session expired
        if (response.status === 401 && !skipAuth) {
          console.log('❌ Session expired, clearing auth and redirecting to login...');
          authService.clearAuth();
          
          // Redirect to login page
          if (typeof window !== 'undefined') {
            window.location.href = '/';
          }
          
          const error = new Error('Session expired. Please log in again.');
          (error as any).status = 401;
          throw error;
        }
        
        throw await this.createErrorFromResponse(response);
      }

      return this.parseResponse<T>(response);
    });
  }

  private async createErrorFromResponse(response: Response): Promise<Error> {
    let errorMessage = `HTTP ${response.status}`;
    try {
      const errorText = await response.text();
      if (errorText) {
        try {
          const errorData = JSON.parse(errorText);
          errorMessage += `: ${errorData.message || errorData.error || response.statusText}`;
        } catch {
          errorMessage += `: ${errorText || response.statusText}`;
        }
      } else {
        errorMessage += `: ${response.statusText}`;
      }
    } catch {
      errorMessage += `: ${response.statusText}`;
    }
    
    const error = new Error(errorMessage);
    (error as any).status = response.status;
    (error as any).response = { status: response.status };
    return error;
  }

  private async parseResponse<T>(response: Response): Promise<T> {
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      const text = await response.text();
      if (!text.trim()) {
        return {} as T; // Return empty object for empty JSON responses
      }
      
      try {
        return JSON.parse(text);
      } catch (parseError) {
        console.error('Failed to parse JSON response:', parseError);
        console.error('Response text preview:', text.substring(0, 1000) + '...');
        throw new Error(`Invalid JSON response: ${(parseError as Error).message}`);
      }
    } else {
      return {} as T; // Return empty object for non-JSON responses
    }
  }

  async get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  async post<T>(endpoint: string, data?: any, skipAuth?: boolean): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: data ? JSON.stringify(data) : undefined,
    }, skipAuth);
  }

  async put<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  async delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }

  async patch<T>(endpoint: string, data?: any): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: data ? JSON.stringify(data) : undefined,
    });
  }

  // Health check endpoint without authentication
  async healthCheck(): Promise<{ status: string }> {
    return this.request('/actuator/health', { method: 'GET' }, true);
  }
}

export const apiService = new ApiService();