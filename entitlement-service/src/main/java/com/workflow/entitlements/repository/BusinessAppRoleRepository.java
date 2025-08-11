package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.BusinessAppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessAppRoleRepository extends JpaRepository<BusinessAppRole, Long> {
    
    @Query("SELECT r FROM BusinessAppRole r WHERE r.businessApplication.businessAppName = :businessAppName AND r.isActive = true")
    List<BusinessAppRole> findByBusinessAppNameAndIsActiveTrue(@Param("businessAppName") String businessAppName);
    
    @Query("SELECT r FROM BusinessAppRole r WHERE r.businessApplication.businessAppName = :businessAppName AND r.roleName = :roleName")
    Optional<BusinessAppRole> findByBusinessAppNameAndRoleName(@Param("businessAppName") String businessAppName, 
                                                               @Param("roleName") String roleName);
    
    List<BusinessAppRole> findByIsActiveTrue();
    
    @Query("SELECT r FROM BusinessAppRole r WHERE r.businessApplication.id = :businessAppId AND r.isActive = true")
    List<BusinessAppRole> findByBusinessApplicationIdAndIsActiveTrue(@Param("businessAppId") Long businessAppId);
    
    @Query("SELECT r FROM BusinessAppRole r WHERE r.businessApplication.id = :businessAppId")
    List<BusinessAppRole> findByBusinessApplicationId(@Param("businessAppId") Long businessAppId);
    
    @Query("SELECT r FROM BusinessAppRole r WHERE r.businessApplication.id = :businessAppId AND r.roleName = :roleName")
    Optional<BusinessAppRole> findByBusinessApplicationIdAndRoleName(@Param("businessAppId") Long businessAppId, @Param("roleName") String roleName);
}