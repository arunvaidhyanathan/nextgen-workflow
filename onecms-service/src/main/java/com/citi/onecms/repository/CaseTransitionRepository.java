package com.citi.onecms.repository;

import com.citi.onecms.entity.CaseTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseTransitionRepository extends JpaRepository<CaseTransition, Long> {
    List<CaseTransition> findByCaseEntityIdOrderByTransitionDateDesc(Long caseId);
    List<CaseTransition> findByPerformedByUserId(String userId);
}