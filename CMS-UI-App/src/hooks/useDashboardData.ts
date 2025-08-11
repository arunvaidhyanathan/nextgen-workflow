import { useWorkItems } from './useWorkItems';
import { useDashboardStats } from './useDashboardStats';

export const useDashboardData = () => {
  const { workItems, isLoading: workItemsLoading, error: workItemsError, refetch: refetchWorkItems } = useWorkItems();
  const { stats, isLoading: statsLoading, error: statsError, refetch: refetchStats } = useDashboardStats();

  const isLoading = workItemsLoading || statsLoading;
  const error = workItemsError || statsError;

  const refetch = () => {
    refetchWorkItems();
    refetchStats();
  };

  return {
    workItems,
    stats,
    isLoading,
    error,
    refetch
  };
};