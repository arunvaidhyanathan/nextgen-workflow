import { useQuery } from '@tanstack/react-query';
import { caseService } from '../services/caseService';
import { useAuth } from '../contexts/AuthContext';

interface DashboardStats {
  allOpenCases: number;
  myWorkItems: number;
  openInvestigations: number;
}

export const useDashboardStats = () => {
  const { user, isAuthenticated } = useAuth();

  const {
    data: stats = {
      allOpenCases: 0,
      myWorkItems: 0,
      openInvestigations: 0
    },
    isLoading,
    error,
    refetch
  } = useQuery<DashboardStats>({
    queryKey: ['dashboardStats', user?.userId],
    queryFn: async () => {
      if (!isAuthenticated || !user) {
        return {
          allOpenCases: 0,
          myWorkItems: 0,
          openInvestigations: 0
        };
      }

      try {
        // Fetch data in parallel
        const [myTasks, dashboardStats] = await Promise.all([
          caseService.getMyTasks(),
          caseService.getDashboardStats()
        ]);

        // Calculate stats
        const myWorkItems = myTasks.length;
        const allOpenCases = dashboardStats.allOpenCases || 0;
        const openInvestigations = dashboardStats.openInvestigations || 0;

        return {
          allOpenCases,
          myWorkItems,
          openInvestigations
        };
      } catch (error) {
        console.error('Error fetching dashboard stats:', error);
        throw error;
      }
    },
    enabled: isAuthenticated && !!user,
    staleTime: 2 * 60 * 1000, // 2 minutes
    refetchOnWindowFocus: true,
    refetchInterval: 60 * 1000, // Refetch every minute
  });

  return {
    stats,
    isLoading,
    error,
    refetch
  };
};