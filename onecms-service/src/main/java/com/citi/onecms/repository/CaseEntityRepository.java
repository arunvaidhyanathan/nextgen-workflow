package com.citi.onecms.repository;

import com.citi.onecms.entity.CaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseEntityRepository extends JpaRepository<CaseEntity, Long> {
    
    Optional<CaseEntity> findByEntityId(String entityId);
    
    List<CaseEntity> findByCaseIdOrderByCreatedAt(String caseId);
    
    List<CaseEntity> findByCaseIdAndEntityType(String caseId, CaseEntity.EntityType entityType);
    
    @Query("SELECT ce FROM CaseEntity ce WHERE ce.caseId = :caseId AND ce.relationshipType LIKE %:relationshipType%")
    List<CaseEntity> findByCaseIdAndRelationshipTypeContaining(@Param("caseId") String caseId, 
                                                               @Param("relationshipType") String relationshipType);
    
    @Query("SELECT ce FROM CaseEntity ce WHERE ce.soeid = :soeid")
    List<CaseEntity> findBySoeid(@Param("soeid") String soeid);
    
    @Query("SELECT ce FROM CaseEntity ce WHERE ce.emailAddress = :email")
    List<CaseEntity> findByEmailAddress(@Param("email") String email);
    
    @Query("SELECT ce FROM CaseEntity ce WHERE ce.investigationFunction = :function ORDER BY ce.createdAt")
    List<CaseEntity> findByInvestigationFunction(@Param("function") String function);
}