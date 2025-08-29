package com.workflow.entitlements.service;

import com.workflow.entitlements.entity.Department;
import com.workflow.entitlements.entity.User;
import com.workflow.entitlements.entity.UserDepartment;
import com.workflow.entitlements.repository.DepartmentRepository;
import com.workflow.entitlements.repository.UserDepartmentRepository;
import com.workflow.entitlements.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Department management service.
 * Works with legacy entities until MILESTONE 4 migration to hybrid schema.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentService {
    
    private final DepartmentRepository departmentRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final UserRepository userRepository;
    
    /**
     * Get all departments with pagination
     */
    public Page<Department> getAllDepartments(Pageable pageable) {
        log.debug("Retrieving all departments with pagination: {}", pageable);
        return departmentRepository.findAll(pageable);
    }
    
    /**
     * Get all active departments
     */
    public List<Department> getAllActiveDepartments() {
        log.debug("Retrieving all active departments");
        return departmentRepository.findAllActive();
    }
    
    /**
     * Find department by ID
     */
    public Optional<Department> findById(Long departmentId) {
        if (departmentId == null) {
            log.warn("Attempted to find department with null ID");
            return Optional.empty();
        }
        
        log.debug("Finding department by ID: {}", departmentId);
        return departmentRepository.findById(departmentId);
    }
    
    /**
     * Find department by code
     */
    public Optional<Department> findByDepartmentCode(String departmentCode) {
        if (departmentCode == null || departmentCode.trim().isEmpty()) {
            log.warn("Attempted to find department with null or empty code");
            return Optional.empty();
        }
        
        log.debug("Finding department by code: {}", departmentCode);
        return departmentRepository.findByDepartmentCode(departmentCode.trim().toUpperCase());
    }
    
    /**
     * Create new department
     */
    @Transactional
    public Department createDepartment(Department department) {
        if (department == null) {
            throw new IllegalArgumentException("Department cannot be null");
        }
        
        log.info("Creating new department: {}", department.getDepartmentCode());
        
        // Validation
        validateDepartmentForCreation(department);
        
        // Normalize fields
        department.setDepartmentCode(department.getDepartmentCode().trim().toUpperCase());
        department.setDepartmentName(department.getDepartmentName().trim());
        if (department.getDescription() != null) {
            department.setDescription(department.getDescription().trim());
        }
        
        // Set defaults
        if (department.getIsActive() == null) {
            department.setIsActive(true);
        }
        
        Department savedDepartment = departmentRepository.save(department);
        log.info("Successfully created department: {} with ID: {}", 
                savedDepartment.getDepartmentCode(), savedDepartment.getId());
        
        return savedDepartment;
    }
    
    /**
     * Update existing department
     */
    @Transactional
    public Department updateDepartment(Long departmentId, Department departmentDetails) {
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID cannot be null");
        }
        if (departmentDetails == null) {
            throw new IllegalArgumentException("Department details cannot be null");
        }
        
        log.info("Updating department: {}", departmentId);
        
        Department existingDepartment = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NoSuchElementException("Department not found: " + departmentId));
        
        // Validate unique constraints (excluding current department)
        validateDepartmentForUpdate(departmentDetails, existingDepartment);
        
        // Update fields
        if (departmentDetails.getDepartmentCode() != null) {
            existingDepartment.setDepartmentCode(departmentDetails.getDepartmentCode().trim().toUpperCase());
        }
        if (departmentDetails.getDepartmentName() != null) {
            existingDepartment.setDepartmentName(departmentDetails.getDepartmentName().trim());
        }
        if (departmentDetails.getDescription() != null) {
            existingDepartment.setDescription(departmentDetails.getDescription().trim());
        }
        if (departmentDetails.getIsActive() != null) {
            existingDepartment.setIsActive(departmentDetails.getIsActive());
        }
        
        Department updatedDepartment = departmentRepository.save(existingDepartment);
        log.info("Successfully updated department: {}", updatedDepartment.getId());
        
        return updatedDepartment;
    }
    
    /**
     * Activate department
     */
    @Transactional
    public Department activateDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID cannot be null");
        }
        
        log.info("Activating department: {}", departmentId);
        
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NoSuchElementException("Department not found: " + departmentId));
        
        department.setIsActive(true);
        Department activatedDepartment = departmentRepository.save(department);
        
        log.info("Successfully activated department: {}", activatedDepartment.getId());
        return activatedDepartment;
    }
    
    /**
     * Deactivate department
     */
    @Transactional
    public Department deactivateDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID cannot be null");
        }
        
        log.info("Deactivating department: {}", departmentId);
        
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new NoSuchElementException("Department not found: " + departmentId));
        
        department.setIsActive(false);
        Department deactivatedDepartment = departmentRepository.save(department);
        
        log.info("Successfully deactivated department: {}", deactivatedDepartment.getId());
        return deactivatedDepartment;
    }
    
    /**
     * Delete department (soft delete by deactivating)
     */
    @Transactional
    public void deleteDepartment(Long departmentId) {
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID cannot be null");
        }
        
        log.info("Soft-deleting department: {}", departmentId);
        deactivateDepartment(departmentId);
    }
    
    /**
     * Get users in department (placeholder - will be implemented in MILESTONE 4)
     */
    public List<User> getDepartmentUsers(Long departmentId) {
        if (departmentId == null) {
            return List.of();
        }
        
        log.debug("Retrieving users in department: {}", departmentId);
        // TODO: Implement when repository method is available in MILESTONE 4
        return List.of();
    }
    
    /**
     * Assign user to department
     */
    @Transactional
    public UserDepartment assignUserToDepartment(UUID userId, Long departmentId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID cannot be null");
        }
        
        log.info("Assigning user {} to department {}", userId, departmentId);
        
        // Validate user and department exist
        if (!userRepository.existsById(userId)) {
            throw new NoSuchElementException("User not found: " + userId);
        }
        if (!departmentRepository.existsById(departmentId)) {
            throw new NoSuchElementException("Department not found: " + departmentId);
        }
        
        // TODO: Check if assignment already exists in MILESTONE 4 when repository method is fixed
        // For now, create new assignment
        
        // Create new assignment
        Department department = departmentRepository.findById(departmentId).get();
        UserDepartment newAssignment = UserDepartment.builder()
                .userId(userId)
                .department(department)
                .isActive(true)
                .build();
        
        UserDepartment savedAssignment = userDepartmentRepository.save(newAssignment);
        log.info("Successfully assigned user {} to department {}", userId, departmentId);
        
        return savedAssignment;
    }
    
    /**
     * Remove user from department
     */
    @Transactional
    public void removeUserFromDepartment(UUID userId, Long departmentId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (departmentId == null) {
            throw new IllegalArgumentException("Department ID cannot be null");
        }
        
        log.info("Removing user {} from department {}", userId, departmentId);
        
        // TODO: Implement user removal from department in MILESTONE 4 when repository methods are fixed
        log.warn("User department removal not implemented yet - will be fixed in MILESTONE 4");
    }
    
    /**
     * Check if department exists
     */
    public boolean existsById(Long departmentId) {
        if (departmentId == null) {
            return false;
        }
        return departmentRepository.existsById(departmentId);
    }
    
    /**
     * Check if department code exists
     */
    public boolean existsByDepartmentCode(String departmentCode) {
        if (departmentCode == null || departmentCode.trim().isEmpty()) {
            return false;
        }
        return departmentRepository.existsByDepartmentCode(departmentCode.trim().toUpperCase());
    }
    
    /**
     * Get total department count
     */
    public long getTotalDepartmentCount() {
        return departmentRepository.count();
    }
    
    /**
     * Get active department count
     */
    public long getActiveDepartmentCount() {
        return departmentRepository.findAllActive().size();
    }
    
    // Private validation methods
    
    private void validateDepartmentForCreation(Department department) {
        if (department.getDepartmentCode() == null || department.getDepartmentCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Department code is required");
        }
        if (department.getDepartmentName() == null || department.getDepartmentName().trim().isEmpty()) {
            throw new IllegalArgumentException("Department name is required");
        }
        
        String departmentCode = department.getDepartmentCode().trim().toUpperCase();
        
        // Check unique constraints
        if (departmentRepository.existsByDepartmentCode(departmentCode)) {
            throw new IllegalArgumentException("Department code already exists: " + departmentCode);
        }
        
        // Validate department code format
        if (!isValidDepartmentCode(departmentCode)) {
            throw new IllegalArgumentException("Invalid department code format: " + departmentCode);
        }
    }
    
    private void validateDepartmentForUpdate(Department departmentDetails, Department existingDepartment) {
        if (departmentDetails.getDepartmentCode() != null) {
            String departmentCode = departmentDetails.getDepartmentCode().trim().toUpperCase();
            if (!departmentCode.equals(existingDepartment.getDepartmentCode()) && 
                departmentRepository.existsByDepartmentCode(departmentCode)) {
                throw new IllegalArgumentException("Department code already exists: " + departmentCode);
            }
            if (!isValidDepartmentCode(departmentCode)) {
                throw new IllegalArgumentException("Invalid department code format: " + departmentCode);
            }
        }
    }
    
    private boolean isValidDepartmentCode(String departmentCode) {
        return departmentCode != null && departmentCode.matches("^[A-Z0-9_-]{2,20}$");
    }
}