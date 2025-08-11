package com.citi.onecms.controller;

import com.citi.onecms.dto.CaseEntityRequest;
import com.citi.onecms.dto.CaseEntityResponse;
import com.citi.onecms.entity.CaseEntity;
import com.citi.onecms.service.CaseEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/case-entities")
@Tag(name = "Case Entities", description = "Case entity management endpoints")
public class CaseEntityController {

    @Autowired
    private CaseEntityService caseEntityService;

    @GetMapping("/case/{caseId}")
    @Operation(summary = "Get entities by case ID", description = "Retrieve all entities associated with a case")
    public ResponseEntity<List<CaseEntityResponse>> getEntitiesByCaseId(@PathVariable String caseId) {
        List<CaseEntityResponse> entities = caseEntityService.getEntitiesByCaseId(caseId);
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/case/{caseId}/type/{entityType}")
    @Operation(summary = "Get entities by case ID and type", description = "Retrieve entities by case ID filtered by entity type")
    public ResponseEntity<List<CaseEntityResponse>> getEntitiesByCaseIdAndType(
            @PathVariable String caseId,
            @PathVariable CaseEntity.EntityType entityType) {
        List<CaseEntityResponse> entities = caseEntityService.getEntitiesByCaseIdAndType(caseId, entityType);
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/{entityId}")
    @Operation(summary = "Get entity by ID", description = "Retrieve a specific entity by its ID")
    public ResponseEntity<CaseEntityResponse> getEntityById(@PathVariable String entityId) {
        Optional<CaseEntityResponse> entity = caseEntityService.getEntityById(entityId);
        return entity.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new entity", description = "Create a new case entity (person or organization)")
    public ResponseEntity<CaseEntityResponse> createEntity(@Valid @RequestBody CaseEntityRequest request) {
        CaseEntityResponse entity = caseEntityService.createEntity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(entity);
    }

    @PutMapping("/{entityId}")
    @Operation(summary = "Update entity", description = "Update an existing case entity")
    public ResponseEntity<CaseEntityResponse> updateEntity(
            @PathVariable String entityId,
            @Valid @RequestBody CaseEntityRequest request) {
        try {
            CaseEntityResponse entity = caseEntityService.updateEntity(entityId, request);
            return ResponseEntity.ok(entity);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{entityId}")
    @Operation(summary = "Delete entity", description = "Delete a case entity")
    public ResponseEntity<Void> deleteEntity(@PathVariable String entityId) {
        try {
            caseEntityService.deleteEntity(entityId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search/soeid/{soeid}")
    @Operation(summary = "Search entities by SOEID", description = "Search for entities by SOEID")
    public ResponseEntity<List<CaseEntityResponse>> searchEntitiesBySoeid(@PathVariable String soeid) {
        List<CaseEntityResponse> entities = caseEntityService.searchEntitiesBySoeid(soeid);
        return ResponseEntity.ok(entities);
    }

    @GetMapping("/search/email/{email}")
    @Operation(summary = "Search entities by email", description = "Search for entities by email address")
    public ResponseEntity<List<CaseEntityResponse>> searchEntitiesByEmail(@PathVariable String email) {
        List<CaseEntityResponse> entities = caseEntityService.searchEntitiesByEmail(email);
        return ResponseEntity.ok(entities);
    }
}