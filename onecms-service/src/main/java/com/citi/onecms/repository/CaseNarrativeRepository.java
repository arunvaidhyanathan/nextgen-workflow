package com.citi.onecms.repository;

import com.citi.onecms.entity.CaseNarrative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseNarrativeRepository extends JpaRepository<CaseNarrative, Long> {
    
    Optional<CaseNarrative> findByNarrativeId(String narrativeId);
    
    List<CaseNarrative> findByCaseIdOrderByCreatedAt(String caseId);
    
    List<CaseNarrative> findByCaseIdAndIsRecalledFalseOrderByCreatedAt(String caseId);
    
    @Query("SELECT cn FROM CaseNarrative cn WHERE cn.caseId = :caseId AND cn.narrativeType = :type ORDER BY cn.createdAt")
    List<CaseNarrative> findByCaseIdAndType(@Param("caseId") String caseId, @Param("type") String type);
    
    @Query("SELECT cn FROM CaseNarrative cn WHERE cn.investigationFunction = :function ORDER BY cn.createdAt DESC")
    List<CaseNarrative> findByInvestigationFunction(@Param("function") String function);
    
    @Query("SELECT cn FROM CaseNarrative cn WHERE cn.createdBy = :userId ORDER BY cn.createdAt DESC")
    List<CaseNarrative> findByCreatedBy(@Param("userId") String userId);
    
    @Query("SELECT COUNT(cn) FROM CaseNarrative cn WHERE cn.caseId = :caseId AND cn.isRecalled = false")
    long countActiveByCaseId(@Param("caseId") String caseId);
    
    @Query("SELECT COUNT(cn) FROM CaseNarrative cn WHERE cn.caseId = :caseId AND cn.isRecalled = true")
    long countRecalledByCaseId(@Param("caseId") String caseId);
}