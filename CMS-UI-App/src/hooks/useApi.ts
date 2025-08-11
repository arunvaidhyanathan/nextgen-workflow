import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '../services/api';
import { useAuth } from '../contexts/AuthContext';
import { useToast } from './use-toast';
import { useEffect } from 'react';

export const useApi = () => {
  const { logout } = useAuth();
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const handleApiError = (error: any) => {
    console.error('API Error:', error);
    
    // Check for 401 status code more reliably
    const isUnauthorized = error.response?.status === 401 || 
                          error.status === 401 || 
                          (error.message && error.message.includes('401'));
    
    if (isUnauthorized) {
      logout();
      toast({
        title: "Session expired",
        description: "Please log in again",
        variant: "destructive",
      });
      return;
    }

    // Show generic error message
    toast({
      title: "API Error",
      description: error.message || "An unexpected error occurred",
      variant: "destructive",
    });
  };

  const useApiQuery = <T>(
    key: string[],
    endpoint: string,
    options?: any
  ) => {
    const query = useQuery<T>({
      queryKey: key,
      queryFn: () => apiService.get<T>(endpoint),
      ...options,
    });

    // Handle errors using useEffect
    useEffect(() => {
      if (query.error) {
        handleApiError(query.error);
      }
    }, [query.error]);

    return query;
  };

  const useApiMutation = <T, V = any>(
    endpoint: string,
    method: 'POST' | 'PUT' | 'DELETE' = 'POST',
    options?: any
  ) => {
    return useMutation<T, Error, V>({
      mutationFn: (data: V) => {
        switch (method) {
          case 'POST':
            return apiService.post<T>(endpoint, data);
          case 'PUT':
            return apiService.put<T>(endpoint, data);
          case 'DELETE':
            return apiService.delete<T>(endpoint);
          default:
            throw new Error(`Unsupported method: ${method}`);
        }
      },
      onError: handleApiError,
      onSuccess: (data, variables, context) => {
        // Invalidate specific queries based on the endpoint
        const queryKeysToInvalidate = getQueryKeysForEndpoint(endpoint);
        queryKeysToInvalidate.forEach(key => {
          queryClient.invalidateQueries({ queryKey: key });
        });
        
        // Call custom onSuccess if provided
        if (options?.onSuccess) {
          options.onSuccess(data, variables, context);
        }
      },
      ...options,
    });
  };

  // Helper function to determine which queries to invalidate based on endpoint
  const getQueryKeysForEndpoint = (endpoint: string): string[][] => {
    if (endpoint.includes('/cases')) {
      return [['cases'], ['dashboardStats'], ['workItems']];
    }
    if (endpoint.includes('/tasks')) {
      return [['tasks'], ['workItems'], ['dashboardStats']];
    }
    // Default to invalidating common queries
    return [['dashboardStats'], ['workItems']];
  };

  return {
    useApiQuery,
    useApiMutation,
    apiService,
  };
};

export default useApi;