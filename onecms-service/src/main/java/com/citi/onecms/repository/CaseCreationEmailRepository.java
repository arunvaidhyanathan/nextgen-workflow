package com.citi.onecms.repository;

import com.citi.onecms.entity.CaseCreationEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseCreationEmailRepository extends JpaRepository<CaseCreationEmail, Long> {
    
    /**
     * Find case creation email by call ID
     */
    Optional<CaseCreationEmail> findByCallId(Long callId);
    
    /**
     * Find all records by status for processing queue
     */
    List<CaseCreationEmail> findByStatusOrderByCreatedAtAsc(String status);
    
    /**
     * Find by sender email for duplicate detection
     */
    List<CaseCreationEmail> findBySenderEmailOrderByCreatedAtDesc(String senderEmail);
    
    /**
     * Find records that are linked to a specific case
     */
    List<CaseCreationEmail> findByCaseId(Long caseId);
    
    /**
     * Find pending records for background processing
     */
    @Query("SELECT cce FROM CaseCreationEmail cce WHERE cce.status = 'PENDING' ORDER BY cce.createdAt ASC")
    List<CaseCreationEmail> findPendingRecords();
    
    /**
     * Update status for a specific call ID
     */
    @Query("UPDATE CaseCreationEmail cce SET cce.status = :status, cce.updatedAt = CURRENT_TIMESTAMP WHERE cce.callId = :callId")
    int updateStatusByCallId(@Param("callId") Long callId, @Param("status") String status);
    
    /**
     * Count records by status for monitoring
     */
    long countByStatus(String status);
    
    /**
     * Find records by employee ID if available
     */
    List<CaseCreationEmail> findByEmployeeId(String employeeId);
}