package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.BusinessApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessApplicationRepository extends JpaRepository<BusinessApplication, Long> {
    
    Optional<BusinessApplication> findByBusinessAppName(String businessAppName);
    
    List<BusinessApplication> findByIsActiveTrue();
    
    boolean existsByBusinessAppName(String businessAppName);
}