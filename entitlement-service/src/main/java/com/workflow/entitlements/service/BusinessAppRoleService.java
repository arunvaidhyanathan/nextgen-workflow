package com.workflow.entitlements.service;

import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.entity.BusinessApplication;
import com.workflow.entitlements.entity.User;
import com.workflow.entitlements.entity.UserBusinessAppRole;
import com.workflow.entitlements.repository.BusinessAppRoleRepository;
import com.workflow.entitlements.repository.BusinessApplicationRepository;
import com.workflow.entitlements.repository.UserBusinessAppRoleRepository;
import com.workflow.entitlements.repository.UserRepository;
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
import java.util.UUID;

/**
 * Business Application Role management service.
 * Works with legacy entities until MILESTONE 4 migration to hybrid schema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessAppRoleService {
    
    private final BusinessAppRoleRepository businessAppRoleRepository;
    private final BusinessApplicationRepository businessApplicationRepository;
    private final UserBusinessAppRoleRepository userBusinessAppRoleRepository;
    private final UserRepository userRepository;
    
    /**
     * Get all business app roles with pagination
     */
    public Page<BusinessAppRole> getAllBusinessAppRoles(Pageable pageable) {
        log.debug("Retrieving all business app roles with pagination: {}", pageable);
        return businessAppRoleRepository.findAll(pageable);
    }
    
    /**
     * Get all active business app roles
     */
    public List<BusinessAppRole> getAllActiveBusinessAppRoles() {
        log.debug("Retrieving all active business app roles");
        return businessAppRoleRepository.findByIsActiveTrue();
    }
    
    /**
     * Find business app role by ID
     */
    public Optional<BusinessAppRole> findById(Long roleId) {
        if (roleId == null) {
            log.warn("Attempted to find business app role with null ID");
            return Optional.empty();
        }
        
        log.debug("Finding business app role by ID: {}", roleId);
        return businessAppRoleRepository.findById(roleId);
    }
    
    /**
     * Find business app role by role name and application
     */
    public Optional<BusinessAppRole> findByRoleNameAndApplication(String roleName, Long applicationId) {
        if (roleName == null || roleName.trim().isEmpty() || applicationId == null) {
            log.warn("Attempted to find business app role with null/empty parameters");
            return Optional.empty();
        }
        
        log.debug("Finding business app role by role name: {} and application: {}", roleName, applicationId);
        return businessAppRoleRepository.findByBusinessApplicationIdAndRoleName(applicationId, roleName.trim());
    }
    
    /**
     * Get roles for specific business application
     */
    public List<BusinessAppRole> getRolesByApplication(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        
        log.debug("Retrieving roles for business application: {}", applicationId);
        return businessAppRoleRepository.findByBusinessApplicationId(applicationId);
    }
    
    /**
     * Get active roles for specific business application
     */
    public List<BusinessAppRole> getActiveRolesByApplication(Long applicationId) {
        if (applicationId == null) {
            return List.of();
        }
        
        log.debug("Retrieving active roles for business application: {}", applicationId);
        return businessAppRoleRepository.findByBusinessApplicationIdAndIsActiveTrue(applicationId);
    }
    
    /**
     * Create new business app role
     */
    @Transactional
    public BusinessAppRole createBusinessAppRole(BusinessAppRole role) {
        if (role == null) {
            throw new IllegalArgumentException("Business app role cannot be null");
        }
        
        log.info("Creating new business app role: {} for application: {}", 
                role.getRoleName(), role.getBusinessApplication().getId());
        
        // Validation
        validateBusinessAppRoleForCreation(role);
        
        // Normalize fields
        role.setRoleName(role.getRoleName().trim());
        role.setRoleDisplayName(role.getRoleDisplayName().trim());
        if (role.getDescription() != null) {
            role.setDescription(role.getDescription().trim());
        }
        
        // Set defaults
        if (role.getIsActive() == null) {
            role.setIsActive(true);
        }
        if (role.getMetadata() == null) {
            role.setMetadata(new HashMap<>());
        }
        role.setCreatedAt(Instant.now());
        role.setUpdatedAt(Instant.now());
        
        BusinessAppRole savedRole = businessAppRoleRepository.save(role);
        log.info("Successfully created business app role: {} with ID: {}", 
                savedRole.getRoleName(), savedRole.getId());
        
        return savedRole;
    }
    
    /**
     * Update existing business app role
     */
    @Transactional
    public BusinessAppRole updateBusinessAppRole(Long roleId, BusinessAppRole roleDetails) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        if (roleDetails == null) {
            throw new IllegalArgumentException("Role details cannot be null");
        }
        
        log.info("Updating business app role: {}", roleId);
        
        BusinessAppRole existingRole = businessAppRoleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Business app role not found: " + roleId));
        
        // Validate unique constraints (excluding current role)
        validateBusinessAppRoleForUpdate(roleDetails, existingRole);
        
        // Update fields
        if (roleDetails.getRoleName() != null) {
            existingRole.setRoleName(roleDetails.getRoleName().trim());
        }
        if (roleDetails.getRoleDisplayName() != null) {
            existingRole.setRoleDisplayName(roleDetails.getRoleDisplayName().trim());
        }
        if (roleDetails.getDescription() != null) {
            existingRole.setDescription(roleDetails.getDescription().trim());
        }
        if (roleDetails.getIsActive() != null) {
            existingRole.setIsActive(roleDetails.getIsActive());
        }
        if (roleDetails.getMetadata() != null) {
            existingRole.setMetadata(roleDetails.getMetadata());
        }
        
        existingRole.setUpdatedAt(Instant.now());
        
        BusinessAppRole updatedRole = businessAppRoleRepository.save(existingRole);
        log.info("Successfully updated business app role: {}", updatedRole.getId());
        
        return updatedRole;
    }
    
    /**
     * Activate business app role
     */
    @Transactional
    public BusinessAppRole activateBusinessAppRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        
        log.info("Activating business app role: {}", roleId);
        
        BusinessAppRole role = businessAppRoleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Business app role not found: " + roleId));
        
        role.setIsActive(true);
        role.setUpdatedAt(Instant.now());
        BusinessAppRole activatedRole = businessAppRoleRepository.save(role);
        
        log.info("Successfully activated business app role: {}", activatedRole.getId());
        return activatedRole;
    }
    
    /**
     * Deactivate business app role
     */
    @Transactional
    public BusinessAppRole deactivateBusinessAppRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        
        log.info("Deactivating business app role: {}", roleId);
        
        BusinessAppRole role = businessAppRoleRepository.findById(roleId)
                .orElseThrow(() -> new NoSuchElementException("Business app role not found: " + roleId));
        
        role.setIsActive(false);
        role.setUpdatedAt(Instant.now());
        BusinessAppRole deactivatedRole = businessAppRoleRepository.save(role);
        
        log.info("Successfully deactivated business app role: {}", deactivatedRole.getId());
        return deactivatedRole;
    }
    
    /**
     * Delete business app role (soft delete by deactivating)
     */
    @Transactional
    public void deleteBusinessAppRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        
        log.info("Soft-deleting business app role: {}", roleId);
        deactivateBusinessAppRole(roleId);
    }
    
    /**
     * Get users assigned to role (placeholder - will be implemented in MILESTONE 4)
     */
    public List<User> getRoleUsers(Long roleId) {
        if (roleId == null) {
            return List.of();
        }
        
        log.debug("Retrieving users assigned to role: {}", roleId);
        // TODO: Implement when repository method is available in MILESTONE 4
        return List.of();
    }
    
    /**
     * Assign role to user
     */
    @Transactional
    public UserBusinessAppRole assignRoleToUser(UUID userId, Long roleId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        
        log.info("Assigning role {} to user {}", roleId, userId);
        
        // Validate user and role exist
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        if (!businessAppRoleRepository.existsById(roleId)) {
            throw new NoSuchElementException("Business app role not found: " + roleId);
        }
        
        // TODO: Check if assignment already exists in MILESTONE 4 when repository method is fixed
        // For now, create new assignment
        
        // Create new assignment
        BusinessAppRole role = businessAppRoleRepository.findById(roleId).get();
        UserBusinessAppRole newAssignment = UserBusinessAppRole.builder()
                .userId(userId)
                .businessAppRole(role)
                .isActive(true)
                .build();
        
        UserBusinessAppRole savedAssignment = userBusinessAppRoleRepository.save(newAssignment);
        log.info("Successfully assigned role {} to user {}", roleId, userId);
        
        return savedAssignment;
    }
    
    /**
     * Remove role from user
     */
    @Transactional
    public void removeRoleFromUser(UUID userId, Long roleId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        
        log.info("Removing role {} from user {}", roleId, userId);
        
        // TODO: Implement role removal in MILESTONE 4 when repository methods are fixed
        log.warn("Role removal not implemented yet - will be fixed in MILESTONE 4");
    }
    
    /**
     * Check if business app role exists
     */
    public boolean existsById(Long roleId) {
        if (roleId == null) {
            return false;
        }
        return businessAppRoleRepository.existsById(roleId);
    }
    
    /**
     * Get total business app role count
     */
    public long getTotalBusinessAppRoleCount() {
        return businessAppRoleRepository.count();
    }
    
    /**
     * Get active business app role count
     */
    public long getActiveBusinessAppRoleCount() {
        return businessAppRoleRepository.findByIsActiveTrue().size();
    }
    
    // Private validation methods
    
    private void validateBusinessAppRoleForCreation(BusinessAppRole role) {
        if (role.getRoleName() == null || role.getRoleName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role name is required");
        }
        if (role.getRoleDisplayName() == null || role.getRoleDisplayName().trim().isEmpty()) {
            throw new IllegalArgumentException("Role display name is required");
        }
        if (role.getBusinessApplication() == null) {
            throw new IllegalArgumentException("Business application is required");
        }
        
        String roleName = role.getRoleName().trim();
        Long applicationId = role.getBusinessApplication().getId();
        
        // Validate application exists
        if (!businessApplicationRepository.existsById(applicationId)) {
            throw new NoSuchElementException("Business application not found: " + applicationId);
        }
        
        // Check unique constraints
        if (businessAppRoleRepository.findByBusinessApplicationIdAndRoleName(applicationId, roleName).isPresent()) {
            throw new IllegalArgumentException("Role name already exists for this application: " + roleName);
        }
        
        // Validate role name format
        if (!isValidRoleName(roleName)) {
            throw new IllegalArgumentException("Invalid role name format: " + roleName);
        }
    }
    
    private void validateBusinessAppRoleForUpdate(BusinessAppRole roleDetails, BusinessAppRole existingRole) {
        if (roleDetails.getRoleName() != null) {
            String roleName = roleDetails.getRoleName().trim();
            Long applicationId = existingRole.getBusinessApplication().getId();
            
            if (!roleName.equals(existingRole.getRoleName())) {
                Optional<BusinessAppRole> existing = businessAppRoleRepository.findByBusinessApplicationIdAndRoleName(applicationId, roleName);
                if (existing.isPresent() && !existing.get().getId().equals(existingRole.getId())) {
                    throw new IllegalArgumentException("Role name already exists for this application: " + roleName);
                }
            }
            if (!isValidRoleName(roleName)) {
                throw new IllegalArgumentException("Invalid role name format: " + roleName);
            }
        }
    }
    
    private boolean isValidRoleName(String roleName) {
        return roleName != null && roleName.matches("^[a-zA-Z0-9_-]{2,50}$");
    }
}