package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {
    
    /**
     * Find all active department assignments for a user
     */
    @Query("SELECT ud FROM UserDepartment ud " +
           "JOIN FETCH ud.department d " +
           "WHERE ud.userId = :userId AND ud.isActive = true AND d.isActive = true " +
           "ORDER BY d.departmentName")
    List<UserDepartment> findActiveUserDepartments(@Param("userId") UUID userId);
    
    /**
     * Find all users in a specific department
     */
    @Query("SELECT ud FROM UserDepartment ud " +
           "JOIN FETCH ud.department d " +
           "WHERE d.departmentCode = :departmentCode AND ud.isActive = true AND d.isActive = true " +
           "ORDER BY ud.userId")
    List<UserDepartment> findActiveUsersInDepartment(@Param("departmentCode") String departmentCode);
    
    /**
     * Get department codes for a specific user
     */
    @Query("SELECT d.departmentCode FROM UserDepartment ud " +
           "JOIN ud.department d " +
           "WHERE ud.userId = :userId AND ud.isActive = true AND d.isActive = true")
    List<String> findUserDepartmentCodes(@Param("userId") UUID userId);
    
    /**
     * Check if user belongs to a specific department
     */
    @Query("SELECT COUNT(ud) > 0 FROM UserDepartment ud " +
           "JOIN ud.department d " +
           "WHERE ud.userId = :userId AND d.departmentCode = :departmentCode " +
           "AND ud.isActive = true AND d.isActive = true")
    boolean isUserInDepartment(@Param("userId") UUID userId, @Param("departmentCode") String departmentCode);
    
    /**
     * Find all department assignments for multiple users
     */
    @Query("SELECT ud FROM UserDepartment ud " +
           "JOIN FETCH ud.department d " +
           "WHERE ud.userId IN :userIds AND ud.isActive = true AND d.isActive = true")
    List<UserDepartment> findActiveUserDepartmentsByUserIds(@Param("userIds") List<UUID> userIds);
}