package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.EntitlementDomainRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EntitlementDomainRole entities.
 * Provides access to domain roles for database engine RBAC.
 */
@Repository
public interface EntitlementDomainRoleRepository extends JpaRepository<EntitlementDomainRole, UUID> {
    
    /**
     * Find role by domain and role name
     */
    Optional<EntitlementDomainRole> findByDomainIdAndRoleName(UUID domainId, String roleName);
    
    /**
     * Find all roles in a domain
     */
    List<EntitlementDomainRole> findByDomainId(UUID domainId);
    
    /**
     * Find all active roles in a domain
     */
    List<EntitlementDomainRole> findByDomainIdAndIsActiveTrue(UUID domainId);
    
    /**
     * Find roles by level
     */
    List<EntitlementDomainRole> findByRoleLevel(String roleLevel);
    
    /**
     * Find roles by maker-checker type
     */
    List<EntitlementDomainRole> findByMakerCheckerType(String makerCheckerType);
    
    /**
     * Find all active roles
     */
    List<EntitlementDomainRole> findByIsActiveTrue();
    
    /**
     * Find roles assigned to a user
     */
    @Query("SELECT dr FROM EntitlementDomainRole dr " +
           "JOIN EntitlementUserDomainRole udr ON dr.roleId = udr.roleId " +
           "WHERE udr.userId = :userId AND udr.isActive = true")
    List<EntitlementDomainRole> findRolesByUserId(@Param("userId") UUID userId);
    
    /**
     * Check if role name exists in domain
     */
    boolean existsByDomainIdAndRoleName(UUID domainId, String roleName);
}