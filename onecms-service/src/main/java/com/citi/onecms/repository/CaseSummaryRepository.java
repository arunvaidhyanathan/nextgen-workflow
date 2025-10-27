package com.citi.onecms.repository;

import com.citi.onecms.entity.CaseSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseSummaryRepository extends JpaRepository<CaseSummary, Long> {
    
    /**
     * Find case summary by case ID and status ID
     */
    Optional<CaseSummary> findByCaseIdAndStatusId(Long caseId, String statusId);
    
    /**
     * Find all summaries for a specific case
     */
    List<CaseSummary> findByCaseIdOrderByCreatedAtAsc(Long caseId);
    
    /**
     * Find all summaries for a specific status across all cases
     */
    List<CaseSummary> findByStatusIdOrderByCreatedAtDesc(String statusId);
    
    /**
     * Find the latest summary for a case (most recent status)
     */
    @Query("SELECT cs FROM CaseSummary cs WHERE cs.caseId = :caseId ORDER BY cs.createdAt DESC LIMIT 1")
    Optional<CaseSummary> findLatestSummaryByCaseId(@Param("caseId") Long caseId);
    
    /**
     * Check if a summary exists for a specific case and status
     */
    boolean existsByCaseIdAndStatusId(Long caseId, String statusId);
    
    /**
     * Delete all summaries for a specific case (cascade delete handling)
     */
    void deleteByCaseId(Long caseId);
    
    /**
     * Count summaries by status for reporting
     */
    long countByStatusId(String statusId);
    
    /**
     * Find cases that have summaries for a specific status
     */
    @Query("SELECT DISTINCT cs.caseId FROM CaseSummary cs WHERE cs.statusId = :statusId")
    List<Long> findCaseIdsWithStatus(@Param("statusId") String statusId);
}