package com.workflow.entitlements.controller;

import com.workflow.entitlements.entity.BusinessApplication;
import com.workflow.entitlements.entity.BusinessAppRole;
import com.workflow.entitlements.service.BusinessApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * REST Controller for Business Application management operations.
 * Now uses BusinessApplicationService instead of direct repository access.
 */
@RestController
@RequestMapping("/api/entitlements/business-applications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class BusinessApplicationController {

    private final BusinessApplicationService businessApplicationService;

    /**
     * Get all business applications with optional pagination
     */
    @GetMapping
    public ResponseEntity<List<BusinessApplication>> getAllBusinessApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            if (page < 0 || size <= 0) {
                // If invalid pagination, return all active applications
                List<BusinessApplication> apps = businessApplicationService.getAllActiveBusinessApplications();
                return ResponseEntity.ok(apps);
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<BusinessApplication> appPage = businessApplicationService.getAllBusinessApplications(pageable);
            return ResponseEntity.ok(appPage.getContent());
            
        } catch (Exception e) {
            log.error("Error retrieving all business applications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get business application by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BusinessApplication> getBusinessApplicationById(@PathVariable Long id) {
        try {
            Optional<BusinessApplication> app = businessApplicationService.findById(id);
            return app.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving business application by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get business application by business app name
     */
    @GetMapping("/name/{businessAppName}")
    public ResponseEntity<BusinessApplication> getBusinessApplicationByName(@PathVariable String businessAppName) {
        try {
            Optional<BusinessApplication> app = businessApplicationService.findByBusinessAppName(businessAppName);
            return app.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Error retrieving business application by name: {}", businessAppName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all active business applications
     */
    @GetMapping("/active")
    public ResponseEntity<List<BusinessApplication>> getActiveBusinessApplications() {
        try {
            List<BusinessApplication> activeApps = businessApplicationService.getAllActiveBusinessApplications();
            return ResponseEntity.ok(activeApps);
        } catch (Exception e) {
            log.error("Error retrieving active business applications", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get business applications by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<BusinessApplication>> getBusinessApplicationsByStatus(@PathVariable String status) {
        try {
            Boolean isActive = null;
            if ("active".equalsIgnoreCase(status)) {
                isActive = true;
            } else if ("inactive".equalsIgnoreCase(status)) {
                isActive = false;
            }
            
            List<BusinessApplication> apps = businessApplicationService.getBusinessApplicationsByStatus(isActive);
            return ResponseEntity.ok(apps);
        } catch (Exception e) {
            log.error("Error retrieving business applications by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get application roles
     */
    @GetMapping("/{id}/roles")
    public ResponseEntity<List<BusinessAppRole>> getApplicationRoles(@PathVariable Long id) {
        try {
            List<BusinessAppRole> roles = businessApplicationService.getApplicationRoles(id);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error retrieving roles for business application: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active application roles
     */
    @GetMapping("/{id}/roles/active")
    public ResponseEntity<List<BusinessAppRole>> getActiveApplicationRoles(@PathVariable Long id) {
        try {
            List<BusinessAppRole> roles = businessApplicationService.getActiveApplicationRoles(id);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            log.error("Error retrieving active roles for business application: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get application statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Object> getBusinessApplicationStats() {
        try {
            long totalApps = businessApplicationService.getTotalBusinessApplicationCount();
            long activeApps = businessApplicationService.getActiveBusinessApplicationCount();
            
            var stats = java.util.Map.of(
                "totalApplications", totalApps,
                "activeApplications", activeApps,
                "inactiveApplications", totalApps - activeApps
            );
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving business application statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create new business application
     */
    @PostMapping
    public ResponseEntity<Object> createBusinessApplication(@RequestBody BusinessApplication application) {
        try {
            BusinessApplication savedApp = businessApplicationService.createBusinessApplication(application);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedApp);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid business application creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error creating business application", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to create business application"));
        }
    }

    /**
     * Update existing business application
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateBusinessApplication(@PathVariable Long id, @RequestBody BusinessApplication applicationDetails) {
        try {
            BusinessApplication updatedApp = businessApplicationService.updateBusinessApplication(id, applicationDetails);
            return ResponseEntity.ok(updatedApp);
            
        } catch (NoSuchElementException e) {
            log.warn("Business application not found for update: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid business application update request for {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
            
        } catch (Exception e) {
            log.error("Error updating business application: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to update business application"));
        }
    }

    /**
     * Soft delete business application (deactivate)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusinessApplication(@PathVariable Long id) {
        try {
            businessApplicationService.deleteBusinessApplication(id);
            return ResponseEntity.noContent().build();
            
        } catch (NoSuchElementException e) {
            log.warn("Business application not found for deletion: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deleting business application: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Activate business application
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Object> activateBusinessApplication(@PathVariable Long id) {
        try {
            BusinessApplication activatedApp = businessApplicationService.activateBusinessApplication(id);
            return ResponseEntity.ok(activatedApp);
            
        } catch (NoSuchElementException e) {
            log.warn("Business application not found for activation: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error activating business application: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to activate business application"));
        }
    }

    /**
     * Deactivate business application
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Object> deactivateBusinessApplication(@PathVariable Long id) {
        try {
            BusinessApplication deactivatedApp = businessApplicationService.deactivateBusinessApplication(id);
            return ResponseEntity.ok(deactivatedApp);
            
        } catch (NoSuchElementException e) {
            log.warn("Business application not found for deactivation: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Error deactivating business application: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to deactivate business application"));
        }
    }

    /**
     * Check if business application exists
     */
    @GetMapping("/{id}/exists")
    public ResponseEntity<Object> checkBusinessApplicationExists(@PathVariable Long id) {
        try {
            boolean exists = businessApplicationService.existsById(id);
            return ResponseEntity.ok(java.util.Map.of("exists", exists));
        } catch (Exception e) {
            log.error("Error checking if business application exists: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to check business application existence"));
        }
    }

    /**
     * Check if business app name is available
     */
    @GetMapping("/name/{businessAppName}/available")
    public ResponseEntity<Object> checkBusinessAppNameAvailable(@PathVariable String businessAppName) {
        try {
            Optional<BusinessApplication> existingApp = businessApplicationService.findByBusinessAppName(businessAppName);
            boolean available = existingApp.isEmpty();
            return ResponseEntity.ok(java.util.Map.of("available", available));
        } catch (Exception e) {
            log.error("Error checking business app name availability: {}", businessAppName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", "Failed to check business app name availability"));
        }
    }
}