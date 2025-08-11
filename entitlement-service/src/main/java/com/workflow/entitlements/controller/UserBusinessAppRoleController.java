package com.workflow.entitlements.controller;

import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.entity.UserBusinessAppRole;
import com.workflow.entitlements.repository.UserBusinessAppRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/entitlements/user-business-app-roles")
@CrossOrigin(origins = "*")
public class UserBusinessAppRoleController {

    @Autowired
    private UserBusinessAppRoleRepository userBusinessAppRoleRepository;

    @GetMapping
    public ResponseEntity<List<UserBusinessAppRole>> getAllUserBusinessAppRoles() {
        List<UserBusinessAppRole> userRoles = userBusinessAppRoleRepository.findAll();
        return ResponseEntity.ok(userRoles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserBusinessAppRole> getUserBusinessAppRoleById(@PathVariable Long id) {
        Optional<UserBusinessAppRole> userRole = userBusinessAppRoleRepository.findById(id);
        return userRole.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserBusinessAppRole>> getUserRolesByUserId(@PathVariable String userId) {
        List<UserBusinessAppRole> userRoles = userBusinessAppRoleRepository.findByUserId(userId);
        return ResponseEntity.ok(userRoles);
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<UserBusinessAppRole>> getActiveUserRolesByUserId(@PathVariable String userId) {
        List<UserBusinessAppRole> activeUserRoles = userBusinessAppRoleRepository.findByUserIdAndIsActiveTrue(userId);
        return ResponseEntity.ok(activeUserRoles);
    }

    @GetMapping("/role/{roleId}")
    public ResponseEntity<List<UserBusinessAppRole>> getUsersByRoleId(@PathVariable Long roleId) {
        List<UserBusinessAppRole> userRoles = userBusinessAppRoleRepository.findByBusinessAppRoleId(roleId);
        return ResponseEntity.ok(userRoles);
    }

    @GetMapping("/role/{roleId}/active")
    public ResponseEntity<List<UserBusinessAppRole>> getActiveUsersByRoleId(@PathVariable Long roleId) {
        List<UserBusinessAppRole> activeUserRoles = userBusinessAppRoleRepository.findByBusinessAppRoleIdAndIsActiveTrue(roleId);
        return ResponseEntity.ok(activeUserRoles);
    }

    @GetMapping("/user/{userId}/role/{roleId}")
    public ResponseEntity<UserBusinessAppRole> getUserBusinessAppRoleByUserAndRole(@PathVariable String userId, @PathVariable Long roleId) {
        Optional<UserBusinessAppRole> userRole = userBusinessAppRoleRepository.findByUserIdAndBusinessAppRoleId(userId, roleId);
        return userRole.map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<UserBusinessAppRole>> getActiveUserBusinessAppRoles() {
        List<UserBusinessAppRole> activeUserRoles = userBusinessAppRoleRepository.findByIsActiveTrue();
        return ResponseEntity.ok(activeUserRoles);
    }

    @PostMapping
    public ResponseEntity<UserBusinessAppRole> createUserBusinessAppRole(@RequestBody UserBusinessAppRole userBusinessAppRole) {
        try {
            UserBusinessAppRole savedUserRole = userBusinessAppRoleRepository.save(userBusinessAppRole);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUserRole);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserBusinessAppRole> updateUserBusinessAppRole(@PathVariable Long id, @RequestBody UserBusinessAppRole userRoleDetails) {
        Optional<UserBusinessAppRole> optionalUserRole = userBusinessAppRoleRepository.findById(id);
        
        if (optionalUserRole.isPresent()) {
            UserBusinessAppRole userRole = optionalUserRole.get();
            userRole.setIsActive(userRoleDetails.getIsActive());
            
            UserBusinessAppRole updatedUserRole = userBusinessAppRoleRepository.save(userRole);
            return ResponseEntity.ok(updatedUserRole);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserBusinessAppRole(@PathVariable Long id) {
        if (userBusinessAppRoleRepository.existsById(id)) {
            userBusinessAppRoleRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserBusinessAppRole> activateUserBusinessAppRole(@PathVariable Long id) {
        Optional<UserBusinessAppRole> optionalUserRole = userBusinessAppRoleRepository.findById(id);
        
        if (optionalUserRole.isPresent()) {
            UserBusinessAppRole userRole = optionalUserRole.get();
            userRole.setIsActive(true);
            UserBusinessAppRole updatedUserRole = userBusinessAppRoleRepository.save(userRole);
            return ResponseEntity.ok(updatedUserRole);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserBusinessAppRole> deactivateUserBusinessAppRole(@PathVariable Long id) {
        Optional<UserBusinessAppRole> optionalUserRole = userBusinessAppRoleRepository.findById(id);
        
        if (optionalUserRole.isPresent()) {
            UserBusinessAppRole userRole = optionalUserRole.get();
            userRole.setIsActive(false);
            UserBusinessAppRole updatedUserRole = userBusinessAppRoleRepository.save(userRole);
            return ResponseEntity.ok(updatedUserRole);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/assign")
    public ResponseEntity<UserBusinessAppRole> assignRoleToUser(@RequestParam String userId, @RequestParam Long roleId) {
        try {
            Optional<UserBusinessAppRole> existingAssignment = userBusinessAppRoleRepository.findByUserIdAndBusinessAppRoleId(userId, roleId);
            
            if (existingAssignment.isPresent()) {
                UserBusinessAppRole userRole = existingAssignment.get();
                userRole.setIsActive(true);
                UserBusinessAppRole updatedUserRole = userBusinessAppRoleRepository.save(userRole);
                return ResponseEntity.ok(updatedUserRole);
            } else {
                UserBusinessAppRole newUserRole = UserBusinessAppRole.builder()
                    .userId(userId)
                    .businessAppRole(BusinessAppRole.builder().id(roleId).build())
                    .isActive(true)
                    .build();
                UserBusinessAppRole savedUserRole = userBusinessAppRoleRepository.save(newUserRole);
                return ResponseEntity.status(HttpStatus.CREATED).body(savedUserRole);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeRoleFromUser(@RequestParam String userId, @RequestParam Long roleId) {
        Optional<UserBusinessAppRole> userRole = userBusinessAppRoleRepository.findByUserIdAndBusinessAppRoleId(userId, roleId);
        
        if (userRole.isPresent()) {
            UserBusinessAppRole role = userRole.get();
            role.setIsActive(false);
            userBusinessAppRoleRepository.save(role);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}