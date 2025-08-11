import { UserInfo } from '../types/auth';

export const TOKEN_KEY = 'auth_token';
export const USER_KEY = 'user_info';

export const getStoredToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const getStoredUser = (): UserInfo | null => {
  const userStr = localStorage.getItem(USER_KEY);
  if (!userStr) return null;
  
  try {
    return JSON.parse(userStr);
  } catch (error) {
    console.error('Error parsing stored user data:', error);
    // Clear corrupted data
    localStorage.removeItem(USER_KEY);
    return null;
  }
};

export const setStoredToken = (token: string): void => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const setStoredUser = (user: UserInfo): void => {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
};

export const clearStoredAuth = (): void => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
};

/**
 * Helper function to safely parse JWT payload
 * @param token JWT token string
 * @returns Parsed payload or null if parsing fails
 */
const parseJwtPayload = (token: string): any | null => {
  try {
    // Validate token format (should have 3 parts separated by dots)
    const parts = token.split('.');
    if (parts.length !== 3) {
      console.error('Invalid JWT token format: token should have 3 parts separated by dots');
      return null;
    }

    const payload = JSON.parse(atob(parts[1]));
    
    // Validate that exp field exists and is a number
    if (typeof payload.exp !== 'number') {
      console.error('Invalid JWT token: exp field is missing or not a number');
      return null;
    }

    return payload;
  } catch (error) {
    console.error('Error parsing JWT token:', error);
    return null;
  }
};

export const isTokenExpired = (token: string): boolean => {
  const payload = parseJwtPayload(token);
  if (!payload) {
    return true; // Treat invalid tokens as expired
  }

  const currentTime = Date.now() / 1000;
  return payload.exp < currentTime;
};

export const getTokenExpirationTime = (token: string): number | null => {
  const payload = parseJwtPayload(token);
  if (!payload) {
    return null;
  }

  return payload.exp * 1000; // Convert to milliseconds
};