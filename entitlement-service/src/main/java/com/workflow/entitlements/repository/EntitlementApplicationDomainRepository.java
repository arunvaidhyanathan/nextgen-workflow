package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.EntitlementApplicationDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for EntitlementApplicationDomain entities.
 * Provides access to application domains for database engine multi-tenancy.
 */
@Repository
public interface EntitlementApplicationDomainRepository extends JpaRepository<EntitlementApplicationDomain, UUID> {
    
    /**
     * Find domain by name
     */
    Optional<EntitlementApplicationDomain> findByDomainName(String domainName);
    
    /**
     * Find all active domains
     */
    List<EntitlementApplicationDomain> findByIsActiveTrue();
    
    /**
     * Find domains by tiered flag
     */
    List<EntitlementApplicationDomain> findByIsTiered(Boolean isTiered);
    
    /**
     * Find domains by metadata JSON query
     */
    @Query("SELECT d FROM EntitlementApplicationDomain d WHERE " +
           "JSON_EXTRACT(d.domainMetadata, '$.tenant') = :tenant")
    List<EntitlementApplicationDomain> findByTenant(@Param("tenant") String tenant);
    
    /**
     * Check if domain name exists and is active
     */
    boolean existsByDomainNameAndIsActiveTrue(String domainName);
}