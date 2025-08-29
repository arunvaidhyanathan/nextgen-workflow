package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.EntitlementAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for EntitlementAuditLog entities.
 * Provides access to audit logs for both RBAC and ABAC engines.
 */
@Repository
public interface EntitlementAuditLogRepository extends JpaRepository<EntitlementAuditLog, UUID> {
    
    /**
     * Find audit logs by user
     */
    List<EntitlementAuditLog> findByUserId(UUID userId);
    
    /**
     * Find audit logs by user with pagination
     */
    Page<EntitlementAuditLog> findByUserId(UUID userId, Pageable pageable);
    
    /**
     * Find audit logs by event type
     */
    List<EntitlementAuditLog> findByEventType(String eventType);
    
    /**
     * Find audit logs by event type with pagination
     */
    Page<EntitlementAuditLog> findByEventType(String eventType, Pageable pageable);
    
    /**
     * Find audit logs by resource
     */
    List<EntitlementAuditLog> findByResourceTypeAndResourceId(String resourceType, String resourceId);
    
    /**
     * Find audit logs by resource type
     */
    List<EntitlementAuditLog> findByResourceType(String resourceType);
    
    /**
     * Find audit logs by decision
     */
    List<EntitlementAuditLog> findByDecision(String decision);
    
    /**
     * Find audit logs by engine type
     */
    List<EntitlementAuditLog> findByEngineType(String engineType);
    
    /**
     * Find audit logs by session
     */
    List<EntitlementAuditLog> findBySessionId(String sessionId);
    
    /**
     * Find audit logs within time range
     */
    @Query("SELECT al FROM EntitlementAuditLog al " +
           "WHERE al.eventTimestamp >= :startTime AND al.eventTimestamp <= :endTime " +
           "ORDER BY al.eventTimestamp DESC")
    List<EntitlementAuditLog> findByTimeRange(@Param("startTime") Instant startTime, 
                                             @Param("endTime") Instant endTime);
    
    /**
     * Find audit logs within time range with pagination
     */
    @Query("SELECT al FROM EntitlementAuditLog al " +
           "WHERE al.eventTimestamp >= :startTime AND al.eventTimestamp <= :endTime " +
           "ORDER BY al.eventTimestamp DESC")
    Page<EntitlementAuditLog> findByTimeRange(@Param("startTime") Instant startTime, 
                                             @Param("endTime") Instant endTime, 
                                             Pageable pageable);
    
    /**
     * Find authorization check logs for user and resource
     */
    @Query("SELECT al FROM EntitlementAuditLog al " +
           "WHERE al.eventType = 'AUTHORIZATION_CHECK' " +
           "AND al.userId = :userId AND al.resourceType = :resourceType " +
           "AND al.resourceId = :resourceId " +
           "ORDER BY al.eventTimestamp DESC")
    List<EntitlementAuditLog> findAuthorizationHistory(@Param("userId") UUID userId,
                                                      @Param("resourceType") String resourceType,
                                                      @Param("resourceId") String resourceId);
    
    /**
     * Count authorization decisions by type
     */
    @Query("SELECT al.decision, COUNT(al) FROM EntitlementAuditLog al " +
           "WHERE al.eventType = 'AUTHORIZATION_CHECK' " +
           "AND al.eventTimestamp >= :startTime " +
           "GROUP BY al.decision")
    List<Object[]> countAuthorizationDecisions(@Param("startTime") Instant startTime);
    
    /**
     * Count events by engine type
     */
    @Query("SELECT al.engineType, COUNT(al) FROM EntitlementAuditLog al " +
           "WHERE al.eventTimestamp >= :startTime " +
           "GROUP BY al.engineType")
    List<Object[]> countEventsByEngine(@Param("startTime") Instant startTime);
    
    /**
     * Find recent failed authorization attempts
     */
    @Query("SELECT al FROM EntitlementAuditLog al " +
           "WHERE al.eventType = 'AUTHORIZATION_CHECK' " +
           "AND al.decision = 'DENY' " +
           "AND al.eventTimestamp >= :since " +
           "ORDER BY al.eventTimestamp DESC")
    List<EntitlementAuditLog> findRecentFailedAuthorizations(@Param("since") Instant since);
}