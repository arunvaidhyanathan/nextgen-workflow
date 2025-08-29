package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.ResourcePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ResourcePermission entities.
 * Manages direct resource-level permissions for database engine ABAC-style access.
 */
@Repository
public interface ResourcePermissionRepository extends JpaRepository<ResourcePermission, UUID> {
    
    /**
     * Find resource permission for user
     */
    Optional<ResourcePermission> findByUserIdAndResourceTypeAndResourceId(
            UUID userId, String resourceType, String resourceId);
    
    /**
     * Find all resource permissions for a user
     */
    List<ResourcePermission> findByUserId(UUID userId);
    
    /**
     * Find all active resource permissions for a user
     */
    List<ResourcePermission> findByUserIdAndIsActiveTrue(UUID userId);
    
    /**
     * Find all currently valid permissions for a user (active and not expired)
     */
    @Query("SELECT rp FROM ResourcePermission rp " +
           "WHERE rp.userId = :userId AND rp.isActive = true " +
           "AND (rp.expiresAt IS NULL OR rp.expiresAt > :now)")
    List<ResourcePermission> findValidPermissionsByUserId(@Param("userId") UUID userId, 
                                                         @Param("now") Instant now);
    
    /**
     * Find permissions for a specific resource
     */
    List<ResourcePermission> findByResourceTypeAndResourceId(String resourceType, String resourceId);
    
    /**
     * Find permissions for all resources of a type
     */
    List<ResourcePermission> findByResourceType(String resourceType);
    
    /**
     * Find expired permissions
     */
    @Query("SELECT rp FROM ResourcePermission rp " +
           "WHERE rp.expiresAt IS NOT NULL AND rp.expiresAt <= :now AND rp.isActive = true")
    List<ResourcePermission> findExpiredPermissions(@Param("now") Instant now);
    
    /**
     * Find permissions granted by a user
     */
    List<ResourcePermission> findByGrantedBy(UUID grantedBy);
    
    /**
     * Check if user has specific resource permission with action
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM entitlements.resource_permissions rp " +
           "WHERE rp.user_id = :userId AND rp.resource_type = :resourceType " +
           "AND rp.resource_id = :resourceId AND :action = ANY(rp.allowed_actions) " +
           "AND rp.is_active = true AND (rp.expires_at IS NULL OR rp.expires_at > :now)",
           nativeQuery = true)
    boolean hasUserResourcePermission(@Param("userId") UUID userId,
                                     @Param("resourceType") String resourceType,
                                     @Param("resourceId") String resourceId,
                                     @Param("action") String action,
                                     @Param("now") Instant now);
    
    /**
     * Count active permissions for user
     */
    @Query("SELECT COUNT(rp) FROM ResourcePermission rp " +
           "WHERE rp.userId = :userId AND rp.isActive = true")
    long countActivePermissionsByUserId(@Param("userId") UUID userId);
    
    /**
     * Deactivate resource permission
     */
    @Query("UPDATE ResourcePermission rp SET rp.isActive = false " +
           "WHERE rp.userId = :userId AND rp.resourceType = :resourceType AND rp.resourceId = :resourceId")
    void deactivateResourcePermission(@Param("userId") UUID userId,
                                     @Param("resourceType") String resourceType,
                                     @Param("resourceId") String resourceId);
}