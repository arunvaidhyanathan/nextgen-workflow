package com.workflow.entitlements.controller;

import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.repository.BusinessAppRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/entitlements/business-app-roles")
@CrossOrigin(origins = "*")
public class BusinessAppRoleController {

    @Autowired
    private BusinessAppRoleRepository businessAppRoleRepository;

    @GetMapping
    public ResponseEntity<List<BusinessAppRole>> getAllBusinessAppRoles() {
        List<BusinessAppRole> roles = businessAppRoleRepository.findAll();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessAppRole> getBusinessAppRoleById(@PathVariable Long id) {
        Optional<BusinessAppRole> role = businessAppRoleRepository.findById(id);
        return role.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/business-application/{businessAppId}")
    public ResponseEntity<List<BusinessAppRole>> getRolesByBusinessApplication(@PathVariable Long businessAppId) {
        List<BusinessAppRole> roles = businessAppRoleRepository.findByBusinessApplicationId(businessAppId);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/business-application/{businessAppId}/active")
    public ResponseEntity<List<BusinessAppRole>> getActiveRolesByBusinessApplication(@PathVariable Long businessAppId) {
        List<BusinessAppRole> activeRoles = businessAppRoleRepository.findByBusinessApplicationIdAndIsActiveTrue(businessAppId);
        return ResponseEntity.ok(activeRoles);
    }

    @GetMapping("/business-application/{businessAppId}/role/{roleName}")
    public ResponseEntity<BusinessAppRole> getRoleByBusinessApplicationAndName(@PathVariable Long businessAppId, @PathVariable String roleName) {
        Optional<BusinessAppRole> role = businessAppRoleRepository.findByBusinessApplicationIdAndRoleName(businessAppId, roleName);
        return role.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<BusinessAppRole>> getActiveBusinessAppRoles() {
        List<BusinessAppRole> activeRoles = businessAppRoleRepository.findByIsActiveTrue();
        return ResponseEntity.ok(activeRoles);
    }

    @PostMapping
    public ResponseEntity<BusinessAppRole> createBusinessAppRole(@RequestBody BusinessAppRole businessAppRole) {
        try {
            BusinessAppRole savedRole = businessAppRoleRepository.save(businessAppRole);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedRole);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessAppRole> updateBusinessAppRole(@PathVariable Long id, @RequestBody BusinessAppRole roleDetails) {
        Optional<BusinessAppRole> optionalRole = businessAppRoleRepository.findById(id);
        
        if (optionalRole.isPresent()) {
            BusinessAppRole role = optionalRole.get();
            role.setRoleName(roleDetails.getRoleName());
            role.setRoleDisplayName(roleDetails.getRoleDisplayName());
            role.setDescription(roleDetails.getDescription());
            role.setIsActive(roleDetails.getIsActive());
            role.setMetadata(roleDetails.getMetadata());
            
            BusinessAppRole updatedRole = businessAppRoleRepository.save(role);
            return ResponseEntity.ok(updatedRole);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusinessAppRole(@PathVariable Long id) {
        if (businessAppRoleRepository.existsById(id)) {
            businessAppRoleRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<BusinessAppRole> activateBusinessAppRole(@PathVariable Long id) {
        Optional<BusinessAppRole> optionalRole = businessAppRoleRepository.findById(id);
        
        if (optionalRole.isPresent()) {
            BusinessAppRole role = optionalRole.get();
            role.setIsActive(true);
            BusinessAppRole updatedRole = businessAppRoleRepository.save(role);
            return ResponseEntity.ok(updatedRole);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BusinessAppRole> deactivateBusinessAppRole(@PathVariable Long id) {
        Optional<BusinessAppRole> optionalRole = businessAppRoleRepository.findById(id);
        
        if (optionalRole.isPresent()) {
            BusinessAppRole role = optionalRole.get();
            role.setIsActive(false);
            BusinessAppRole updatedRole = businessAppRoleRepository.save(role);
            return ResponseEntity.ok(updatedRole);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}