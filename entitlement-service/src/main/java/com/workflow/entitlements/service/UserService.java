package com.workflow.entitlements.service;

import com.workflow.entitlements.entity.User;
import com.workflow.entitlements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * User management service.
 * Works with User entity (entitlement_core_users table) and basic operations only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * Get all users with pagination support
     */
    public Page<User> getAllUsers(Pageable pageable) {
        log.debug("Retrieving all users with pagination: {}", pageable);
        return userRepository.findAll(pageable);
    }
    
    /**
     * Get all active users
     */
    public List<User> getAllActiveUsers() {
        log.debug("Retrieving all active users");
        return userRepository.findByIsActiveTrue();
    }
    
    /**
     * Find user by ID
     */
    public Optional<User> findById(UUID userId) {
        if (userId == null) {
            log.warn("Attempted to find user with null ID");
            return Optional.empty();
        }
        
        log.debug("Finding user by ID: {}", userId);
        return userRepository.findById(userId);
    }
    
    /**
     * Find user by username
     */
    public Optional<User> findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            log.warn("Attempted to find user with null or empty username");
            return Optional.empty();
        }
        
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username.trim());
    }
    
    /**
     * Find user by email
     */
    public Optional<User> findByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            log.warn("Attempted to find user with null or empty email");
            return Optional.empty();
        }
        
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email.trim().toLowerCase());
    }
    
    /**
     * Create a new user with validation
     */
    @Transactional
    public User createUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        log.info("Creating new user: {}", user.getUsername());
        
        // Validation
        validateUserForCreation(user);
        
        // Normalize fields
        user.setUsername(user.getUsername().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
        
        // Set defaults
        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }
        if (user.getGlobalAttributes() == null) {
            user.setGlobalAttributes(new HashMap<>());
        }
        
        User savedUser = userRepository.save(user);
        log.info("Successfully created user: {} with ID: {}", savedUser.getUsername(), savedUser.getUserId());
        
        return savedUser;
    }
    
    /**
     * Update existing user
     */
    @Transactional
    public User updateUser(UUID userId, User userDetails) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (userDetails == null) {
            throw new IllegalArgumentException("User details cannot be null");
        }
        
        log.info("Updating user: {}", userId);
        
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        
        // Validate unique constraints (excluding current user)
        validateUserForUpdate(userDetails, existingUser);
        
        // Update fields
        if (userDetails.getUsername() != null) {
            existingUser.setUsername(userDetails.getUsername().trim());
        }
        if (userDetails.getEmail() != null) {
            existingUser.setEmail(userDetails.getEmail().trim().toLowerCase());
        }
        if (userDetails.getFirstName() != null) {
            existingUser.setFirstName(userDetails.getFirstName().trim());
        }
        if (userDetails.getLastName() != null) {
            existingUser.setLastName(userDetails.getLastName().trim());
        }
        if (userDetails.getIsActive() != null) {
            existingUser.setIsActive(userDetails.getIsActive());
        }
        if (userDetails.getGlobalAttributes() != null) {
            existingUser.setGlobalAttributes(userDetails.getGlobalAttributes());
        }
        
        User updatedUser = userRepository.save(existingUser);
        log.info("Successfully updated user: {}", updatedUser.getUserId());
        
        return updatedUser;
    }
    
    /**
     * Activate user
     */
    @Transactional
    public User activateUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        log.info("Activating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        
        user.setIsActive(true);
        User activatedUser = userRepository.save(user);
        
        log.info("Successfully activated user: {}", activatedUser.getUserId());
        return activatedUser;
    }
    
    /**
     * Deactivate user
     */
    @Transactional
    public User deactivateUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        log.info("Deactivating user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        
        user.setIsActive(false);
        User deactivatedUser = userRepository.save(user);
        
        log.info("Successfully deactivated user: {}", deactivatedUser.getUserId());
        return deactivatedUser;
    }
    
    /**
     * Delete user (soft delete by deactivating)
     */
    @Transactional
    public void deleteUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        log.info("Soft-deleting user: {}", userId);
        deactivateUser(userId);
    }
    
    /**
     * Hard delete user (use with caution)
     */
    @Transactional
    public void hardDeleteUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        log.warn("Hard-deleting user: {}", userId);
        
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        
        // Delete user
        userRepository.deleteById(userId);
        
        log.warn("Successfully hard-deleted user: {}", userId);
    }
    
    /**
     * Check if user exists
     */
    public boolean existsById(UUID userId) {
        if (userId == null) {
            return false;
        }
        return userRepository.existsById(userId);
    }
    
    /**
     * Get total user count
     */
    public long getTotalUserCount() {
        return userRepository.count();
    }
    
    /**
     * Get active user count
     */
    public long getActiveUserCount() {
        return userRepository.findByIsActiveTrue().size();
    }
    
    /**
     * Search users by keyword using existing search method
     */
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        
        log.debug("Searching users with keyword: {}", keyword);
        return userRepository.findBySearchCriteria(keyword.trim());
    }
    
    // Private validation methods
    
    private void validateUserForCreation(User user) {
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        
        String username = user.getUsername().trim();
        String email = user.getEmail().trim().toLowerCase();
        
        // Check unique constraints
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        
        // Validate email format
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        
        // Validate username format
        if (!isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username format: " + username);
        }
    }
    
    private void validateUserForUpdate(User userDetails, User existingUser) {
        if (userDetails.getUsername() != null) {
            String username = userDetails.getUsername().trim();
            if (!username.equals(existingUser.getUsername())) {
                Optional<User> existing = userRepository.findByUsername(username);
                if (existing.isPresent() && !existing.get().getUserId().equals(existingUser.getUserId())) {
                    throw new IllegalArgumentException("Username already exists: " + username);
                }
            }
            if (!isValidUsername(username)) {
                throw new IllegalArgumentException("Invalid username format: " + username);
            }
        }
        
        if (userDetails.getEmail() != null) {
            String email = userDetails.getEmail().trim().toLowerCase();
            if (!email.equals(existingUser.getEmail())) {
                Optional<User> existing = userRepository.findByEmail(email);
                if (existing.isPresent() && !existing.get().getUserId().equals(existingUser.getUserId())) {
                    throw new IllegalArgumentException("Email already exists: " + email);
                }
            }
            if (!isValidEmail(email)) {
                throw new IllegalArgumentException("Invalid email format: " + email);
            }
        }
    }
    
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    private boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9._-]{3,50}$");
    }
}