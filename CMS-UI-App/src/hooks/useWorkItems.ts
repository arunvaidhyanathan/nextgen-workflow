import { useQuery } from '@tanstack/react-query';
import { caseService } from '../services/caseService';
import { WorkItemDisplayData } from '../types/case';
import { useAuth } from '../contexts/AuthContext';

export const useWorkItems = () => {
  const { user, isAuthenticated } = useAuth();

  const {
    data: workItems = [],
    isLoading,
    error,
    refetch
  } = useQuery<WorkItemDisplayData[]>({
    queryKey: ['workItems', user?.userId],
    queryFn: async () => {
      if (!isAuthenticated || !user) {
        return [];
      }

      try {
        console.log('🔄 Fetching dashboard cases for logged-in user...');
        // Fetch all cases from dashboard-cases endpoint
        const cases = await caseService.getMyCases(0, 100);
        console.log('Dashboard cases fetched:', cases.length, 'cases');

        // Transform cases directly to work items for display
        return caseService.transformCasesToWorkItems(cases);
      } catch (error) {
        console.error('Error fetching work items:', error);
        throw error;
      }
    },
    enabled: isAuthenticated && !!user,
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: true,
    refetchInterval: 30 * 1000, // Refetch every 30 seconds for real-time updates
  });

  return {
    workItems,
    isLoading,
    error,
    refetch,
    count: workItems.length
  };
};