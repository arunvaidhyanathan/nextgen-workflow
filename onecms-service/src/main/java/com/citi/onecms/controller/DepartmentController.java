package com.citi.onecms.controller;

import com.citi.onecms.dto.DepartmentResponse;
import com.citi.onecms.entity.Department;
import com.citi.onecms.repository.DepartmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/departments")
@Tag(name = "Departments", description = "Department and assignment group management endpoints")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    @GetMapping
    @Operation(summary = "Get all active departments", description = "Retrieve all active departments ordered by name")
    public ResponseEntity<List<DepartmentResponse>> getAllActiveDepartments() {
        List<Department> departments = departmentRepository.findAllActiveOrderByName();
        List<DepartmentResponse> departmentResponses = departments.stream()
                .map(DepartmentResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(departmentResponses);
    }

    @GetMapping("/assignment-groups")
    @Operation(summary = "Get assignment groups", description = "Get all departments that can be used as assignment groups")
    public ResponseEntity<List<DepartmentResponse>> getAssignmentGroups() {
        List<Department> departments = departmentRepository.findAllActiveOrderByName();
        List<DepartmentResponse> departmentResponses = departments.stream()
                .map(DepartmentResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(departmentResponses);
    }

    @GetMapping("/region/{region}")
    @Operation(summary = "Get departments by region", description = "Get departments filtered by region")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByRegion(@PathVariable String region) {
        List<Department> departments = departmentRepository.findByDepartmentRegionAndIsActiveTrueOrderByDepartmentName(region);
        List<DepartmentResponse> departmentResponses = departments.stream()
                .map(DepartmentResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(departmentResponses);
    }

    @GetMapping("/function/{function}")
    @Operation(summary = "Get departments by function", description = "Get departments filtered by function")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByFunction(@PathVariable String function) {
        List<Department> departments = departmentRepository.findByDepartmentFunctionAndIsActiveTrueOrderByDepartmentName(function);
        List<DepartmentResponse> departmentResponses = departments.stream()
                .map(DepartmentResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(departmentResponses);
    }

    @GetMapping("/region/{region}/function/{function}")
    @Operation(summary = "Get departments by region and function", description = "Get departments filtered by both region and function")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByRegionAndFunction(
            @PathVariable String region,
            @PathVariable String function) {
        List<Department> departments = departmentRepository.findByRegionAndFunctionAndActive(region, function);
        List<DepartmentResponse> departmentResponses = departments.stream()
                .map(DepartmentResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(departmentResponses);
    }

    @GetMapping("/{departmentCode}")
    @Operation(summary = "Get department by code", description = "Retrieve a specific department by its code")
    public ResponseEntity<DepartmentResponse> getDepartmentByCode(@PathVariable String departmentCode) {
        return departmentRepository.findByDepartmentCode(departmentCode)
                .map(department -> ResponseEntity.ok(new DepartmentResponse(department)))
                .orElse(ResponseEntity.notFound().build());
    }
}