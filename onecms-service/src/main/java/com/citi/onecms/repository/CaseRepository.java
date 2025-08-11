package com.citi.onecms.repository;

import com.citi.onecms.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long> {
    
    Optional<Case> findById(Long id);
    Optional<Case> findByCaseNumber(String caseNumber);
    List<Case> findByAssignedToUserId(String userId);
    List<Case> findByStatus(String status);
    
    @Query("SELECT COUNT(c) FROM Case c WHERE YEAR(c.createdAt) = :year")
    long countByYear(@Param("year") int year);
    
    @Query("SELECT MAX(c.id) FROM Case c")
    Long findMaxCaseId();
    
    @Query("SELECT c FROM Case c WHERE c.assignedToUserId = :userId OR c.createdByUserId = :userId")
    List<Case> findCasesForUser(@Param("userId") String userId);
}