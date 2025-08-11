package com.citi.onecms.repository;

import com.citi.onecms.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByDepartmentId(Long departmentId);
    Optional<Department> findByDepartmentCode(String departmentCode);
    List<Department> findByIsActiveTrue();
    List<Department> findByManagerUserId(String managerUserId);
    boolean existsByDepartmentCode(String departmentCode);
    
    // New methods for assignment groups
    List<Department> findByDepartmentRegionAndIsActiveTrueOrderByDepartmentName(String region);
    List<Department> findByDepartmentFunctionAndIsActiveTrueOrderByDepartmentName(String function);
    
    @Query("SELECT d FROM Department d WHERE d.departmentRegion = ?1 AND d.departmentFunction = ?2 AND d.isActive = true ORDER BY d.departmentName")
    List<Department> findByRegionAndFunctionAndActive(String region, String function);
    
    @Query("SELECT d FROM Department d WHERE d.isActive = true ORDER BY d.departmentName")
    List<Department> findAllActiveOrderByName();
}