import React, { createContext, useContext, useEffect, useState } from 'react';
import { AuthState, UserInfo } from '../types/auth';
import { authService } from '../services/authService';

interface AuthContextType extends AuthState {
  login: (username: string, password: string) => Promise<boolean>;
  logout: () => Promise<void>;
  validateSession: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: React.ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>({
    isAuthenticated: false,
    user: null,
    sessionId: null,
  });

  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check for existing session on app load
    const initializeAuth = async () => {
      const sessionId = authService.getSessionId();
      const user = authService.getUser();

      if (sessionId && user) {
        // Validate session with backend
        const isValid = await authService.validateSession();
        if (isValid) {
          setAuthState({
            isAuthenticated: true,
            user,
            sessionId,
          });
        } else {
          authService.clearAuth();
        }
      }
      setIsLoading(false);
    };

    initializeAuth();
  }, []);

  const login = async (username: string, password: string): Promise<boolean> => {
    try {
      const response = await authService.login({ username, password });
      
      if (response.success) {
        setAuthState({
          isAuthenticated: true,
          user: response.user,
          sessionId: response.sessionId,
        });
        return true;
      }
      return false;
    } catch (error) {
      console.error('Login failed:', error);
      return false;
    }
  };

  const logout = async (): Promise<void> => {
    await authService.logout();
    setAuthState({
      isAuthenticated: false,
      user: null,
      sessionId: null,
    });
  };

  const validateSession = async (): Promise<boolean> => {
    const isValid = await authService.validateSession();
    if (!isValid) {
      setAuthState({
        isAuthenticated: false,
        user: null,
        sessionId: null,
      });
    }
    return isValid;
  };

  const value: AuthContextType = {
    ...authState,
    login,
    logout,
    validateSession,
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-lg">Loading...</div>
      </div>
    );
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};