package com.workflow.entitlements.service;

import com.workflow.entitlements.entity.BusinessApplication;
import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.repository.BusinessApplicationRepository;
import com.workflow.entitlements.repository.BusinessAppRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Business Application management service.
 * Works with legacy entities until MILESTONE 4 migration to hybrid schema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessApplicationService {
    
    private final BusinessApplicationRepository businessApplicationRepository;
    private final BusinessAppRoleRepository businessAppRoleRepository;
    
    /**
     * Get all business applications with pagination
     */
    public Page<BusinessApplication> getAllBusinessApplications(Pageable pageable) {
        log.debug("Retrieving all business applications with pagination: {}", pageable);
        return businessApplicationRepository.findAll(pageable);
    }
    
    /**
     * Get all active business applications
     */
    public List<BusinessApplication> getAllActiveBusinessApplications() {
        log.debug("Retrieving all active business applications");
        return businessApplicationRepository.findByIsActiveTrue();
    }
    
    /**
     * Find business application by ID
     */
    public Optional<BusinessApplication> findById(Long applicationId) {
        if (applicationId == null) {
            log.warn("Attempted to find business application with null ID");
            return Optional.empty();
        }
        
        log.debug("Finding business application by ID: {}", applicationId);
        return businessApplicationRepository.findById(applicationId);
    }
    
    /**
     * Find business application by business app name
     */
    public Optional<BusinessApplication> findByBusinessAppName(String businessAppName) {
        if (businessAppName == null || businessAppName.trim().isEmpty()) {
            log.warn("Attempted to find business application with null or empty business app name");
            return Optional.empty();
        }
        
        log.debug("Finding business application by business app name: {}", businessAppName);
        return businessApplicationRepository.findByBusinessAppName(businessAppName.trim());
    }
    
    /**
     * Create new business application
     */
    @Transactional
    public BusinessApplication createBusinessApplication(BusinessApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("Business application cannot be null");
        }
        
        log.info("Creating new business application: {}", application.getBusinessAppName());
        
        // Validation
        validateBusinessApplicationForCreation(application);
        
        // Normalize fields
        application.setBusinessAppName(application.getBusinessAppName().trim());
        if (application.getDescription() != null) {
            application.setDescription(application.getDescription().trim());
        }
        
        // Set defaults
        if (application.getIsActive() == null) {
            application.setIsActive(true);
        }
        if (application.getMetadata() == null) {
            application.setMetadata(new HashMap<>());
        }
        application.setCreatedAt(Instant.now());
        application.setUpdatedAt(Instant.now());
        
        BusinessApplication savedApplication = businessApplicationRepository.save(application);
        log.info("Successfully created business application: {} with ID: {}", 
                savedApplication.getBusinessAppName(), savedApplication.getId());
        
        return savedApplication;
    }
    
    /**
     * Update existing business application
     */
    @Transactional
    public BusinessApplication updateBusinessApplication(Long applicationId, BusinessApplication applicationDetails) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        if (applicationDetails == null) {
            throw new IllegalArgumentException("Application details cannot be null");
        }
        
        log.info("Updating business application: {}", applicationId);
        
        BusinessApplication existingApplication = businessApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("Business application not found: " + applicationId));
        
        // Validate unique constraints (excluding current application)
        validateBusinessApplicationForUpdate(applicationDetails, existingApplication);
        
        // Update fields
        if (applicationDetails.getBusinessAppName() != null) {
            existingApplication.setBusinessAppName(applicationDetails.getBusinessAppName().trim());
        }
        if (applicationDetails.getDescription() != null) {
            existingApplication.setDescription(applicationDetails.getDescription().trim());
        }
        if (applicationDetails.getIsActive() != null) {
            existingApplication.setIsActive(applicationDetails.getIsActive());
        }
        if (applicationDetails.getMetadata() != null) {
            existingApplication.setMetadata(applicationDetails.getMetadata());
        }
        
        existingApplication.setUpdatedAt(Instant.now());
        
        BusinessApplication updatedApplication = businessApplicationRepository.save(existingApplication);
        log.info("Successfully updated business application: {}", updatedApplication.getId());
        
        return updatedApplication;
    }
    
    /**
     * Activate business application
     */
    @Transactional
    public BusinessApplication activateBusinessApplication(Long applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        
        log.info("Activating business application: {}", applicationId);
        
        BusinessApplication application = businessApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("Business application not found: " + applicationId));
        
        application.setIsActive(true);
        application.setUpdatedAt(Instant.now());
        BusinessApplication activatedApplication = businessApplicationRepository.save(application);
        
        log.info("Successfully activated business application: {}", activatedApplication.getId());
        return activatedApplication;
    }
    
    /**
     * Deactivate business application
     */
    @Transactional
    public BusinessApplication deactivateBusinessApplication(Long applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        
        log.info("Deactivating business application: {}", applicationId);
        
        BusinessApplication application = businessApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NoSuchElementException("Business application not found: " + applicationId));
        
        application.setIsActive(false);
        application.setUpdatedAt(Instant.now());
        BusinessApplication deactivatedApplication = businessApplicationRepository.save(application);
        
        log.info("Successfully deactivated business application: {}", deactivatedApplication.getId());
        return deactivatedApplication;
    }
    
    /**
     * Delete business application (soft delete by deactivating)
     */
    @Transactional
    public void deleteBusinessApplication(Long applicationId) {
        if (applicationId == null) {
            throw new IllegalArgumentException("Application ID cannot be null");
        }
        
        log.info("Soft-deleting business application: {}", applicationId);
        deactivateBusinessApplication(applicationId);
    }
    
    /**
     * Get roles for business application
     */
    public List<BusinessAppRole> getApplicationRoles(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        
        log.debug("Retrieving roles for business application: {}", applicationId);
        return businessAppRoleRepository.findByBusinessApplicationId(applicationId);
    }
    
    /**
     * Get active roles for business application
     */
    public List<BusinessAppRole> getActiveApplicationRoles(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        
        log.debug("Retrieving active roles for business application: {}", applicationId);
        return businessAppRoleRepository.findByBusinessApplicationIdAndIsActiveTrue(applicationId);
    }
    
    /**
     * Check if business application exists
     */
    public boolean existsById(Long applicationId) {
        if (applicationId == null) {
            return false;
        }
        return businessApplicationRepository.existsById(applicationId);
    }
    
    /**
     * Check if business app name exists
     */
    public boolean existsByBusinessAppName(String businessAppName) {
        if (businessAppName == null || businessAppName.trim().isEmpty()) {
            return false;
        }
        return businessApplicationRepository.existsByBusinessAppName(businessAppName.trim());
    }
    
    /**
     * Get total business application count
     */
    public long getTotalBusinessApplicationCount() {
        return businessApplicationRepository.count();
    }
    
    /**
     * Get active business application count
     */
    public long getActiveBusinessApplicationCount() {
        return businessApplicationRepository.findByIsActiveTrue().size();
    }
    
    /**
     * Get business applications by status
     */
    public List<BusinessApplication> getBusinessApplicationsByStatus(Boolean isActive) {
        if (isActive == null) {
            return businessApplicationRepository.findAll();
        }
        
        log.debug("Retrieving business applications with active status: {}", isActive);
        if (isActive) {
            return businessApplicationRepository.findByIsActiveTrue();
        } else {
            // TODO: Add findByIsActiveFalse method to repository in MILESTONE 4
            return businessApplicationRepository.findAll().stream()
                    .filter(app -> !app.getIsActive())
                    .toList();
        }
    }
    
    // Private validation methods
    
    private void validateBusinessApplicationForCreation(BusinessApplication application) {
        if (application.getBusinessAppName() == null || application.getBusinessAppName().trim().isEmpty()) {
            throw new IllegalArgumentException("Business app name is required");
        }
        
        String businessAppName = application.getBusinessAppName().trim();
        
        // Check unique constraints
        if (businessApplicationRepository.existsByBusinessAppName(businessAppName)) {
            throw new IllegalArgumentException("Business app name already exists: " + businessAppName);
        }
        
        // Validate business app name format
        if (!isValidBusinessAppName(businessAppName)) {
            throw new IllegalArgumentException("Invalid business app name format: " + businessAppName);
        }
    }
    
    private void validateBusinessApplicationForUpdate(BusinessApplication applicationDetails, 
                                                    BusinessApplication existingApplication) {
        if (applicationDetails.getBusinessAppName() != null) {
            String businessAppName = applicationDetails.getBusinessAppName().trim();
            if (!businessAppName.equals(existingApplication.getBusinessAppName()) && 
                businessApplicationRepository.existsByBusinessAppName(businessAppName)) {
                throw new IllegalArgumentException("Business app name already exists: " + businessAppName);
            }
            if (!isValidBusinessAppName(businessAppName)) {
                throw new IllegalArgumentException("Invalid business app name format: " + businessAppName);
            }
        }
    }
    
    private boolean isValidBusinessAppName(String businessAppName) {
        return businessAppName != null && businessAppName.matches("^[a-zA-Z0-9-_]{3,50}$");
    }
}