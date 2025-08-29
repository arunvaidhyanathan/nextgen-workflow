package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.EntitlementPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repository for EntitlementPermission entities.
 * Provides access to permissions for database engine RBAC.
 */
@Repository
public interface EntitlementPermissionRepository extends JpaRepository<EntitlementPermission, UUID> {
    
    /**
     * Find permission by resource type and action
     */
    Optional<EntitlementPermission> findByResourceTypeAndAction(String resourceType, String action);
    
    /**
     * Find all permissions for a resource type
     */
    List<EntitlementPermission> findByResourceType(String resourceType);
    
    /**
     * Find all permissions for specific actions
     */
    List<EntitlementPermission> findByActionIn(Set<String> actions);
    
    /**
     * Find permissions assigned to a role
     */
    @Query("SELECT p FROM EntitlementPermission p " +
           "JOIN EntitlementRolePermission rp ON p.permissionId = rp.permissionId " +
           "WHERE rp.roleId = :roleId AND rp.isActive = true")
    List<EntitlementPermission> findPermissionsByRoleId(@Param("roleId") UUID roleId);
    
    /**
     * Find permissions assigned to a user through roles
     */
    @Query("SELECT DISTINCT p FROM EntitlementPermission p " +
           "JOIN EntitlementRolePermission rp ON p.permissionId = rp.permissionId " +
           "JOIN EntitlementUserDomainRole udr ON rp.roleId = udr.roleId " +
           "WHERE udr.userId = :userId AND udr.isActive = true AND rp.isActive = true")
    List<EntitlementPermission> findPermissionsByUserId(@Param("userId") UUID userId);
    
    /**
     * Check if user has specific permission
     */
    @Query("SELECT COUNT(p) > 0 FROM EntitlementPermission p " +
           "JOIN EntitlementRolePermission rp ON p.permissionId = rp.permissionId " +
           "JOIN EntitlementUserDomainRole udr ON rp.roleId = udr.roleId " +
           "WHERE udr.userId = :userId AND p.resourceType = :resourceType AND p.action = :action " +
           "AND udr.isActive = true AND rp.isActive = true")
    boolean hasUserPermission(@Param("userId") UUID userId, 
                             @Param("resourceType") String resourceType, 
                             @Param("action") String action);
    
    /**
     * Check if permission exists
     */
    boolean existsByResourceTypeAndAction(String resourceType, String action);
}