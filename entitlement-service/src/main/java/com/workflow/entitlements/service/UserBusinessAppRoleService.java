package com.workflow.entitlements.service;

import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.entity.UserBusinessAppRole;
import com.workflow.entitlements.repository.BusinessAppRoleRepository;
import com.workflow.entitlements.repository.UserBusinessAppRoleRepository;
import com.workflow.entitlements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * User Business Application Role management service.
 * Handles user-role assignment relationships and operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserBusinessAppRoleService {
    
    private final UserBusinessAppRoleRepository userBusinessAppRoleRepository;
    private final UserRepository userRepository;
    private final BusinessAppRoleRepository businessAppRoleRepository;
    
    /**
     * Get all user business app roles with pagination
     */
    public Page<UserBusinessAppRole> getAllUserBusinessAppRoles(Pageable pageable) {
        log.debug("Retrieving all user business app roles with pagination: {}", pageable);
        return userBusinessAppRoleRepository.findAll(pageable);
    }
    
    /**
     * Get all user business app roles
     */
    public List<UserBusinessAppRole> getAllUserBusinessAppRoles() {
        log.debug("Retrieving all user business app roles");
        return userBusinessAppRoleRepository.findAll();
    }
    
    /**
     * Get all active user business app roles
     */
    public List<UserBusinessAppRole> getAllActiveUserBusinessAppRoles() {
        log.debug("Retrieving all active user business app roles");
        return userBusinessAppRoleRepository.findByIsActiveTrue();
    }
    
    /**
     * Find user business app role by ID
     */
    public Optional<UserBusinessAppRole> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        log.debug("Finding user business app role by ID: {}", id);
        return userBusinessAppRoleRepository.findById(id);
    }
    
    /**
     * Get user roles by user ID
     */
    public List<UserBusinessAppRole> getUserRolesByUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        log.debug("Retrieving roles for user: {}", userId);
        return userBusinessAppRoleRepository.findByUserId(userId);
    }
    
    /**
     * Get active user roles by user ID
     */
    public List<UserBusinessAppRole> getActiveUserRolesByUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        log.debug("Retrieving active roles for user: {}", userId);
        return userBusinessAppRoleRepository.findByUserIdAndIsActiveTrue(userId);
    }
    
    /**
     * Get users by role ID
     */
    public List<UserBusinessAppRole> getUsersByRoleId(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        log.debug("Retrieving users for role: {}", roleId);
        return userBusinessAppRoleRepository.findByBusinessAppRoleId(roleId);
    }
    
    /**
     * Get active users by role ID
     */
    public List<UserBusinessAppRole> getActiveUsersByRoleId(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        log.debug("Retrieving active users for role: {}", roleId);
        return userBusinessAppRoleRepository.findByBusinessAppRoleIdAndIsActiveTrue(roleId);
    }
    
    /**
     * Find user business app role by user and role ID
     */
    public Optional<UserBusinessAppRole> findByUserIdAndRoleId(UUID userId, Long roleId) {
        if (userId == null || roleId == null) {
            return Optional.empty();
        }
        log.debug("Finding user business app role for user {} and role {}", userId, roleId);
        return userBusinessAppRoleRepository.findByUserIdAndBusinessAppRoleId(userId, roleId);
    }
    
    /**
     * Create user business app role
     */
    @Transactional
    public UserBusinessAppRole createUserBusinessAppRole(UserBusinessAppRole userBusinessAppRole) {
        if (userBusinessAppRole == null) {
            throw new IllegalArgumentException("UserBusinessAppRole cannot be null");
        }
        
        validateUserBusinessAppRoleForCreation(userBusinessAppRole);
        
        log.info("Creating user business app role assignment for user {} and role {}", 
            userBusinessAppRole.getUserId(), userBusinessAppRole.getBusinessAppRole().getId());
        
        UserBusinessAppRole savedRole = userBusinessAppRoleRepository.save(userBusinessAppRole);
        log.info("Successfully created user business app role with ID: {}", savedRole.getId());
        
        return savedRole;
    }
    
    /**
     * Update user business app role
     */
    @Transactional
    public UserBusinessAppRole updateUserBusinessAppRole(Long id, UserBusinessAppRole roleDetails) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        if (roleDetails == null) {
            throw new IllegalArgumentException("Role details cannot be null");
        }
        
        log.info("Updating user business app role: {}", id);
        
        Optional<UserBusinessAppRole> optionalRole = userBusinessAppRoleRepository.findById(id);
        if (optionalRole.isEmpty()) {
            throw new NoSuchElementException("User business app role not found: " + id);
        }
        
        UserBusinessAppRole existingRole = optionalRole.get();
        
        // Update allowed fields
        if (roleDetails.getIsActive() != null) {
            existingRole.setIsActive(roleDetails.getIsActive());
        }
        
        UserBusinessAppRole updatedRole = userBusinessAppRoleRepository.save(existingRole);
        log.info("Successfully updated user business app role: {}", id);
        
        return updatedRole;
    }
    
    /**
     * Delete user business app role (hard delete)
     */
    @Transactional
    public void deleteUserBusinessAppRole(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        
        log.info("Deleting user business app role: {}", id);
        
        if (!userBusinessAppRoleRepository.existsById(id)) {
            throw new NoSuchElementException("User business app role not found: " + id);
        }
        
        userBusinessAppRoleRepository.deleteById(id);
        log.info("Successfully deleted user business app role: {}", id);
    }
    
    /**
     * Activate user business app role
     */
    @Transactional
    public UserBusinessAppRole activateUserBusinessAppRole(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        
        log.info("Activating user business app role: {}", id);
        
        Optional<UserBusinessAppRole> optionalRole = userBusinessAppRoleRepository.findById(id);
        if (optionalRole.isEmpty()) {
            throw new NoSuchElementException("User business app role not found: " + id);
        }
        
        UserBusinessAppRole role = optionalRole.get();
        role.setIsActive(true);
        
        UserBusinessAppRole activatedRole = userBusinessAppRoleRepository.save(role);
        log.info("Successfully activated user business app role: {}", id);
        
        return activatedRole;
    }
    
    /**
     * Deactivate user business app role
     */
    @Transactional
    public UserBusinessAppRole deactivateUserBusinessAppRole(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        
        log.info("Deactivating user business app role: {}", id);
        
        Optional<UserBusinessAppRole> optionalRole = userBusinessAppRoleRepository.findById(id);
        if (optionalRole.isEmpty()) {
            throw new NoSuchElementException("User business app role not found: " + id);
        }
        
        UserBusinessAppRole role = optionalRole.get();
        role.setIsActive(false);
        
        UserBusinessAppRole deactivatedRole = userBusinessAppRoleRepository.save(role);
        log.info("Successfully deactivated user business app role: {}", id);
        
        return deactivatedRole;
    }
    
    /**
     * Assign role to user (create new or activate existing)
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
        
        // Validate user exists
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        
        // Validate role exists
        Optional<BusinessAppRole> role = businessAppRoleRepository.findById(roleId);
        if (role.isEmpty()) {
            throw new NoSuchElementException("Business app role not found: " + roleId);
        }
        
        // Check if assignment already exists
        Optional<UserBusinessAppRole> existingAssignment = 
            userBusinessAppRoleRepository.findByUserIdAndBusinessAppRoleId(userId, roleId);
        
        if (existingAssignment.isPresent()) {
            // Activate existing assignment
            UserBusinessAppRole assignment = existingAssignment.get();
            assignment.setIsActive(true);
            UserBusinessAppRole savedAssignment = userBusinessAppRoleRepository.save(assignment);
            log.info("Activated existing role assignment for user {} and role {}", userId, roleId);
            return savedAssignment;
        } else {
            // Create new assignment
            UserBusinessAppRole newAssignment = UserBusinessAppRole.builder()
                .userId(userId)
                .businessAppRole(role.get())
                .isActive(true)
                .build();
            
            UserBusinessAppRole savedAssignment = userBusinessAppRoleRepository.save(newAssignment);
            log.info("Created new role assignment for user {} and role {}", userId, roleId);
            return savedAssignment;
        }
    }
    
    /**
     * Revoke role from user (deactivate)
     */
    @Transactional
    public void revokeRoleFromUser(UUID userId, Long roleId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID cannot be null");
        }
        
        log.info("Revoking role {} from user {}", roleId, userId);
        
        Optional<UserBusinessAppRole> assignment = 
            userBusinessAppRoleRepository.findByUserIdAndBusinessAppRoleId(userId, roleId);
        
        if (assignment.isEmpty()) {
            throw new NoSuchElementException("User business app role assignment not found for user " + userId + " and role " + roleId);
        }
        
        UserBusinessAppRole role = assignment.get();
        role.setIsActive(false);
        userBusinessAppRoleRepository.save(role);
        log.info("Successfully revoked role {} from user {}", roleId, userId);
    }
    
    /**
     * Check if user business app role exists
     */
    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        return userBusinessAppRoleRepository.existsById(id);
    }
    
    /**
     * Get total user business app role count
     */
    public long getTotalUserBusinessAppRoleCount() {
        return userBusinessAppRoleRepository.count();
    }
    
    /**
     * Get active user business app role count
     */
    public long getActiveUserBusinessAppRoleCount() {
        return userBusinessAppRoleRepository.findByIsActiveTrue().size();
    }
    
    // Private validation methods
    
    private void validateUserBusinessAppRoleForCreation(UserBusinessAppRole role) {
        if (role.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (role.getBusinessAppRole() == null || role.getBusinessAppRole().getId() == null) {
            throw new IllegalArgumentException("Business app role is required");
        }
        
        // Validate user exists
        if (!userRepository.existsById(role.getUserId())) {
            throw new NoSuchElementException("User not found: " + role.getUserId());
        }
        
        // Validate role exists
        if (!businessAppRoleRepository.existsById(role.getBusinessAppRole().getId())) {
            throw new NoSuchElementException("Business app role not found: " + role.getBusinessAppRole().getId());
        }
    }
}