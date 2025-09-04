package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.EntitlementUserDomainRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EntitlementUserDomainRole entities.
 * Manages user-role assignments for database engine RBAC.
 */
@Repository
public interface EntitlementUserDomainRoleRepository extends JpaRepository<EntitlementUserDomainRole, UUID> {
    
    /**
     * Find user-role assignment
     */
    Optional<EntitlementUserDomainRole> findByUserIdAndRoleId(UUID userId, UUID roleId);
    
    /**
     * Find all roles for a user
     */
    List<EntitlementUserDomainRole> findByUserId(UUID userId);
    
    /**
     * Find all active roles for a user
     */
    List<EntitlementUserDomainRole> findByUserIdAndIsActiveTrue(UUID userId);
    
    /**
     * Find all currently valid roles for a user (active and not expired)
     */
    @Query("SELECT udr FROM EntitlementUserDomainRole udr " +
           "WHERE udr.userId = :userId AND udr.isActive = true " +
           "AND (udr.expiresAt IS NULL OR udr.expiresAt > :now)")
    List<EntitlementUserDomainRole> findValidRolesByUserId(@Param("userId") UUID userId, 
                                                          @Param("now") Instant now);
    
    /**
     * Find all users with a role
     */
    List<EntitlementUserDomainRole> findByRoleId(UUID roleId);
    
    /**
     * Find all active users with a role
     */
    List<EntitlementUserDomainRole> findByRoleIdAndIsActiveTrue(UUID roleId);
    
    /**
     * Find expired role assignments
     */
    @Query("SELECT udr FROM EntitlementUserDomainRole udr " +
           "WHERE udr.expiresAt IS NOT NULL AND udr.expiresAt <= :now AND udr.isActive = true")
    List<EntitlementUserDomainRole> findExpiredAssignments(@Param("now") Instant now);
    
    /**
     * Find assignments by assigner
     */
    List<EntitlementUserDomainRole> findByAssignedBy(UUID assignedBy);
    
    /**
     * Check if user has role
     */
    boolean existsByUserIdAndRoleIdAndIsActiveTrue(UUID userId, UUID roleId);
    
    /**
     * Count active roles for user
     */
    @Query("SELECT COUNT(udr) FROM EntitlementUserDomainRole udr " +
           "WHERE udr.userId = :userId AND udr.isActive = true")
    long countActiveRolesByUserId(@Param("userId") UUID userId);
    
    /**
     * Deactivate user role assignment
     */
    @Query("UPDATE EntitlementUserDomainRole udr SET udr.isActive = false " +
           "WHERE udr.userId = :userId AND udr.roleId = :roleId")
    void deactivateUserRole(@Param("userId") UUID userId, @Param("roleId") UUID roleId);
    
    /**
     * Get role names for a user (for Cerbos principal building)
     */
    @Query("SELECT dr.roleName FROM EntitlementUserDomainRole udr " +
           "JOIN EntitlementDomainRole dr ON udr.roleId = dr.roleId " +
           "WHERE udr.userId = :userId AND udr.isActive = true")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);
}