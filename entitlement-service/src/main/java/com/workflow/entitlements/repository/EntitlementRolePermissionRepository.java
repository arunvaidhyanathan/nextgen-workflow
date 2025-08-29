package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.EntitlementRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EntitlementRolePermission entities.
 * Manages role-permission mappings for database engine RBAC.
 */
@Repository
public interface EntitlementRolePermissionRepository extends JpaRepository<EntitlementRolePermission, UUID> {
    
    /**
     * Find role-permission mapping
     */
    Optional<EntitlementRolePermission> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    
    /**
     * Find all permissions for a role
     */
    List<EntitlementRolePermission> findByRoleId(UUID roleId);
    
    /**
     * Find all active permissions for a role
     */
    List<EntitlementRolePermission> findByRoleIdAndIsActiveTrue(UUID roleId);
    
    /**
     * Find all roles with a permission
     */
    List<EntitlementRolePermission> findByPermissionId(UUID permissionId);
    
    /**
     * Find all active role-permission mappings
     */
    List<EntitlementRolePermission> findByIsActiveTrue();
    
    /**
     * Count permissions for a role
     */
    @Query("SELECT COUNT(rp) FROM EntitlementRolePermission rp " +
           "WHERE rp.roleId = :roleId AND rp.isActive = true")
    long countActivePermissionsByRoleId(@Param("roleId") UUID roleId);
    
    /**
     * Check if role has permission
     */
    boolean existsByRoleIdAndPermissionIdAndIsActiveTrue(UUID roleId, UUID permissionId);
    
    /**
     * Delete role-permission mapping (soft delete by setting inactive)
     */
    @Query("UPDATE EntitlementRolePermission rp SET rp.isActive = false " +
           "WHERE rp.roleId = :roleId AND rp.permissionId = :permissionId")
    void deactivateRolePermission(@Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);
}