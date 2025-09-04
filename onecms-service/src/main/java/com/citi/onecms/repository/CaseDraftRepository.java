package com.citi.onecms.repository;

import com.citi.onecms.entity.CaseDraft;
import com.citi.onecms.entity.CaseDraft.DraftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseDraftRepository extends JpaRepository<CaseDraft, Long> {
    
    /**
     * Find case draft by process instance ID
     */
    Optional<CaseDraft> findByProcessInstanceId(String processInstanceId);
    
    /**
     * Find case drafts by user who created them
     */
    List<CaseDraft> findByCreatedByUserIdOrderByCreatedAtDesc(String createdByUserId);
    
    /**
     * Find case drafts by user with pagination
     */
    Page<CaseDraft> findByCreatedByUserIdOrderByCreatedAtDesc(String createdByUserId, Pageable pageable);
    
    /**
     * Find case drafts by status
     */
    List<CaseDraft> findByDraftStatusOrderByCreatedAtDesc(DraftStatus draftStatus);
    
    /**
     * Find case drafts by user and status
     */
    List<CaseDraft> findByCreatedByUserIdAndDraftStatusOrderByCreatedAtDesc(
            String createdByUserId, DraftStatus draftStatus);
    
    /**
     * Find case drafts created between dates
     */
    List<CaseDraft> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find case drafts by department
     */
    List<CaseDraft> findByDepartmentIdOrderByCreatedAtDesc(Long departmentId);
    
    /**
     * Find case draft by case ID
     */
    Optional<CaseDraft> findByCaseId(Long caseId);
    
    /**
     * Find case drafts by case type
     */
    List<CaseDraft> findByCaseTypeIdOrderByCreatedAtDesc(Long caseTypeId);
    
    /**
     * Find case drafts by initial task queue
     */
    List<CaseDraft> findByInitialTaskQueueOrderByCreatedAtDesc(String initialTaskQueue);
    
    /**
     * Custom query to find case drafts with workflow details
     */
    @Query("SELECT cd FROM CaseDraft cd " +
           "LEFT JOIN FETCH cd.caseEntity c " +
           "LEFT JOIN FETCH cd.caseType ct " +
           "WHERE cd.createdByUserId = :userId " +
           "ORDER BY cd.createdAt DESC")
    List<CaseDraft> findByUserIdWithDetails(@Param("userId") String userId);
    
    /**
     * Custom query to count drafts by status for a user
     */
    @Query("SELECT COUNT(cd) FROM CaseDraft cd " +
           "WHERE cd.createdByUserId = :userId " +
           "AND cd.draftStatus = :status")
    Long countByUserIdAndStatus(@Param("userId") String userId, 
                               @Param("status") DraftStatus status);
    
    /**
     * Custom query to find active drafts with task information
     */
    @Query("SELECT cd FROM CaseDraft cd " +
           "WHERE cd.draftStatus = 'DRAFT' " +
           "AND cd.taskStatus IN ('OPEN', 'CLAIMED') " +
           "ORDER BY cd.createdAt DESC")
    List<CaseDraft> findActiveDraftsWithTasks();
    
    /**
     * Find case drafts by business key
     */
    Optional<CaseDraft> findByBusinessKey(String businessKey);
    
    /**
     * Find case drafts by complaint received method
     */
    List<CaseDraft> findByComplaintReceivedMethodOrderByCreatedAtDesc(String complaintReceivedMethod);
    
    /**
     * Check if a case draft exists for a given case ID
     */
    boolean existsByCaseId(Long caseId);
    
    /**
     * Check if a case draft exists for a given process instance
     */
    boolean existsByProcessInstanceId(String processInstanceId);
}