package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.UserBusinessAppRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBusinessAppRoleRepository extends JpaRepository<UserBusinessAppRole, Long> {
    
    @Query("SELECT uar FROM UserBusinessAppRole uar " +
           "JOIN FETCH uar.businessAppRole bar " +
           "JOIN FETCH bar.businessApplication ba " +
           "WHERE uar.userId = :userId AND ba.businessAppName = :businessAppName AND uar.isActive = true")
    List<UserBusinessAppRole> findActiveUserRoles(@Param("userId") String userId, 
                                                  @Param("businessAppName") String businessAppName);
    
    @Query("SELECT uar FROM UserBusinessAppRole uar " +
           "JOIN FETCH uar.businessAppRole bar " +
           "JOIN FETCH bar.businessApplication ba " +
           "WHERE uar.userId = :userId AND uar.isActive = true")
    List<UserBusinessAppRole> findAllActiveUserRoles(@Param("userId") String userId);
    
    @Query("SELECT uar FROM UserBusinessAppRole uar " +
           "JOIN FETCH uar.businessAppRole bar " +
           "JOIN FETCH bar.businessApplication ba " +
           "WHERE ba.businessAppName = :businessAppName AND bar.roleName = :roleName AND uar.isActive = true")
    List<UserBusinessAppRole> findUsersByBusinessAppAndRole(@Param("businessAppName") String businessAppName,
                                                            @Param("roleName") String roleName);
    
    @Query("SELECT uar FROM UserBusinessAppRole uar " +
           "WHERE uar.userId = :userId AND uar.businessAppRole.id = :businessAppRoleId")
    Optional<UserBusinessAppRole> findByUserIdAndBusinessAppRoleId(@Param("userId") String userId,
                                                                   @Param("businessAppRoleId") Long businessAppRoleId);
    
    boolean existsByUserIdAndBusinessAppRoleIdAndIsActiveTrue(String userId, Long businessAppRoleId);
    
    List<UserBusinessAppRole> findByUserId(String userId);
    
    List<UserBusinessAppRole> findByUserIdAndIsActiveTrue(String userId);
    
    List<UserBusinessAppRole> findByBusinessAppRoleId(Long businessAppRoleId);
    
    List<UserBusinessAppRole> findByBusinessAppRoleIdAndIsActiveTrue(Long businessAppRoleId);
    
    List<UserBusinessAppRole> findByIsActiveTrue();
}