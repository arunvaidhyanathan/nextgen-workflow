package com.workflow.entitlements.controller;

import com.workflow.entitlements.entity.BusinessApplication;
import com.workflow.entitlements.repository.BusinessApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/entitlements/business-applications")
@CrossOrigin(origins = "*")
public class BusinessApplicationController {

    @Autowired
    private BusinessApplicationRepository businessApplicationRepository;

    @GetMapping
    public ResponseEntity<List<BusinessApplication>> getAllBusinessApplications() {
        List<BusinessApplication> apps = businessApplicationRepository.findAll();
        return ResponseEntity.ok(apps);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessApplication> getBusinessApplicationById(@PathVariable Long id) {
        Optional<BusinessApplication> app = businessApplicationRepository.findById(id);
        return app.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{businessAppName}")
    public ResponseEntity<BusinessApplication> getBusinessApplicationByName(@PathVariable String businessAppName) {
        Optional<BusinessApplication> app = businessApplicationRepository.findByBusinessAppName(businessAppName);
        return app.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public ResponseEntity<List<BusinessApplication>> getActiveBusinessApplications() {
        List<BusinessApplication> activeApps = businessApplicationRepository.findByIsActiveTrue();
        return ResponseEntity.ok(activeApps);
    }

    @PostMapping
    public ResponseEntity<BusinessApplication> createBusinessApplication(@RequestBody BusinessApplication businessApplication) {
        try {
            BusinessApplication savedApp = businessApplicationRepository.save(businessApplication);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedApp);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<BusinessApplication> updateBusinessApplication(@PathVariable Long id, @RequestBody BusinessApplication appDetails) {
        Optional<BusinessApplication> optionalApp = businessApplicationRepository.findById(id);
        
        if (optionalApp.isPresent()) {
            BusinessApplication app = optionalApp.get();
            app.setBusinessAppName(appDetails.getBusinessAppName());
            app.setDescription(appDetails.getDescription());
            app.setIsActive(appDetails.getIsActive());
            app.setMetadata(appDetails.getMetadata());
            
            BusinessApplication updatedApp = businessApplicationRepository.save(app);
            return ResponseEntity.ok(updatedApp);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBusinessApplication(@PathVariable Long id) {
        if (businessApplicationRepository.existsById(id)) {
            businessApplicationRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<BusinessApplication> activateBusinessApplication(@PathVariable Long id) {
        Optional<BusinessApplication> optionalApp = businessApplicationRepository.findById(id);
        
        if (optionalApp.isPresent()) {
            BusinessApplication app = optionalApp.get();
            app.setIsActive(true);
            BusinessApplication updatedApp = businessApplicationRepository.save(app);
            return ResponseEntity.ok(updatedApp);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<BusinessApplication> deactivateBusinessApplication(@PathVariable Long id) {
        Optional<BusinessApplication> optionalApp = businessApplicationRepository.findById(id);
        
        if (optionalApp.isPresent()) {
            BusinessApplication app = optionalApp.get();
            app.setIsActive(false);
            BusinessApplication updatedApp = businessApplicationRepository.save(app);
            return ResponseEntity.ok(updatedApp);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}