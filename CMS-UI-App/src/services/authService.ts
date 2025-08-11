import { LoginRequest, LoginResponse, UserInfo, SessionValidationResponse } from '../types/auth';

class AuthService {
  private readonly SESSION_ID_KEY = 'session_id';
  private readonly USER_KEY = 'user_info';

  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await this.directApiCall<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials),
    });
    
    if (response.success && response.sessionId) {
      this.setSessionId(response.sessionId);
      this.setUser(response.user);
    }
    
    return response;
  }

  async logout(): Promise<void> {
    try {
      const sessionId = this.getSessionId();
      // Call backend logout endpoint to invalidate session
      await this.directApiCall('/auth/logout', {
        method: 'POST',
        body: JSON.stringify({ sessionId }),
        headers: {
          'X-Session-Id': sessionId || '',
        },
      });
    } catch (error) {
      console.error('Logout API call failed:', error);
    } finally {
      // Always clear local storage regardless of API call result
      this.clearAuth();
    }
  }

  async validateSession(): Promise<boolean> {
    const sessionId = this.getSessionId();
    if (!sessionId) return false;

    try {
      const response = await this.directApiCall<SessionValidationResponse>('/auth/validate-session', {
        method: 'GET',
        headers: {
          'X-Session-Id': sessionId,
        },
      });
      
      if (response.success && response.user) {
        this.setUser(response.user);
        return true;
      }
      return false;
    } catch (error) {
      console.error('Session validation failed:', error);
      this.clearAuth();
      return false;
    }
  }

  setSessionId(sessionId: string): void {
    localStorage.setItem(this.SESSION_ID_KEY, sessionId);
  }

  getSessionId(): string | null {
    return localStorage.getItem(this.SESSION_ID_KEY);
  }

  setUser(user: UserInfo): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  getUser(): UserInfo | null {
    const userStr = localStorage.getItem(this.USER_KEY);
    if (!userStr) return null;
    
    try {
      return JSON.parse(userStr);
    } catch (error) {
      console.error('Error parsing user data from localStorage:', error);
      // Clear corrupted data
      localStorage.removeItem(this.USER_KEY);
      return null;
    }
  }

  clearAuth(): void {
    localStorage.removeItem(this.SESSION_ID_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getSessionId() && !!this.getUser();
  }

  async getCurrentUser(): Promise<UserInfo | null> {
    const sessionId = this.getSessionId();
    if (!sessionId) return null;

    try {
      const response = await this.directApiCall<{success: boolean, user: UserInfo}>('/auth/user', {
        method: 'GET',
        headers: {
          'X-Session-Id': sessionId,
        },
      });
      
      if (response.success && response.user) {
        this.setUser(response.user);
        return response.user;
      }
      return null;
    } catch (error) {
      console.error('Get current user failed:', error);
      return null;
    }
  }

  // Direct API call without using apiService to avoid circular dependency
  private async directApiCall<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    // Use relative URLs to leverage Vite proxy in development
    // In production, this should be set to the actual backend URL
    const baseURL = import.meta.env.VITE_API_BASE_URL || '';
    const url = baseURL ? `${baseURL}${endpoint}` : `/api${endpoint}`;

    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    };

    const response = await fetch(url, config);

    if (!response.ok) {
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
        }
      } catch {
        errorMessage += `: ${response.statusText}`;
      }

      const error = new Error(errorMessage);
      (error as any).status = response.status;
      throw error;
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return response.json();
    }

    return {} as T;
  }
}

export const authService = new AuthService();