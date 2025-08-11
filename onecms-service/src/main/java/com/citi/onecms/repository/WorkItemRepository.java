package com.citi.onecms.repository;

import com.citi.onecms.entity.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, Long> {
    
    Optional<WorkItem> findByWorkItemId(String workItemId);
    Optional<WorkItem> findByTaskKey(Long taskKey);
    List<WorkItem> findByAssignedToUserIdAndStatus(String userId, String status);
    List<WorkItem> findByStatus(String status);
    
    @Query("SELECT wi FROM WorkItem wi WHERE wi.assignedToUserId = :userId AND wi.status != 'COMPLETED'")
    List<WorkItem> findActiveWorkItemsForUser(@Param("userId") String userId);
}