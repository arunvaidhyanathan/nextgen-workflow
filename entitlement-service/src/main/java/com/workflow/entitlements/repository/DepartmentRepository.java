package com.workflow.entitlements.repository;

import com.workflow.entitlements.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    /**
     * Find department by department code
     */
    Optional<Department> findByDepartmentCode(String departmentCode);
    
    /**
     * Find all active departments
     */
    @Query("SELECT d FROM Department d WHERE d.isActive = true ORDER BY d.departmentName")
    List<Department> findAllActive();
    
    /**
     * Check if department code exists
     */
    boolean existsByDepartmentCode(String departmentCode);
    
    /**
     * Find departments by name containing (case-insensitive search)
     */
    @Query("SELECT d FROM Department d WHERE LOWER(d.departmentName) LIKE LOWER(CONCAT('%', :name, '%')) AND d.isActive = true")
    List<Department> findByDepartmentNameContainingIgnoreCase(@Param("name") String name);
}