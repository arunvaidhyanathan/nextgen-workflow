package com.citi.onecms.controller;

import com.citi.onecms.dto.CaseNarrativeRequest;
import com.citi.onecms.dto.CaseNarrativeResponse;
import com.citi.onecms.service.CaseNarrativeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/v1/case-narratives")
@Tag(name = "Case Narratives", description = "Case narrative management endpoints")
public class CaseNarrativeController {

    @Autowired
    private CaseNarrativeService caseNarrativeService;

    @GetMapping("/case/{caseId}")
    @Operation(summary = "Get narratives by case ID", description = "Retrieve all narratives associated with a case")
    public ResponseEntity<List<CaseNarrativeResponse>> getNarrativesByCaseId(@PathVariable String caseId) {
        List<CaseNarrativeResponse> narratives = caseNarrativeService.getNarrativesByCaseId(caseId);
        return ResponseEntity.ok(narratives);
    }

    @GetMapping("/case/{caseId}/active")
    @Operation(summary = "Get active narratives by case ID", description = "Retrieve only active (non-recalled) narratives for a case")
    public ResponseEntity<List<CaseNarrativeResponse>> getActiveNarrativesByCaseId(@PathVariable String caseId) {
        List<CaseNarrativeResponse> narratives = caseNarrativeService.getActiveNarrativesByCaseId(caseId);
        return ResponseEntity.ok(narratives);
    }

    @GetMapping("/{narrativeId}")
    @Operation(summary = "Get narrative by ID", description = "Retrieve a specific narrative by its ID")
    public ResponseEntity<CaseNarrativeResponse> getNarrativeById(@PathVariable String narrativeId) {
        Optional<CaseNarrativeResponse> narrative = caseNarrativeService.getNarrativeById(narrativeId);
        return narrative.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new narrative", description = "Create a new case narrative")
    public ResponseEntity<CaseNarrativeResponse> createNarrative(@Valid @RequestBody CaseNarrativeRequest request) {
        CaseNarrativeResponse narrative = caseNarrativeService.createNarrative(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(narrative);
    }

    @PutMapping("/{narrativeId}")
    @Operation(summary = "Update narrative", description = "Update an existing case narrative")
    public ResponseEntity<CaseNarrativeResponse> updateNarrative(
            @PathVariable String narrativeId,
            @Valid @RequestBody CaseNarrativeRequest request) {
        try {
            CaseNarrativeResponse narrative = caseNarrativeService.updateNarrative(narrativeId, request);
            return ResponseEntity.ok(narrative);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{narrativeId}/recall")
    @Operation(summary = "Recall narrative", description = "Mark a narrative as recalled")
    public ResponseEntity<Void> recallNarrative(@PathVariable String narrativeId) {
        try {
            caseNarrativeService.recallNarrative(narrativeId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{narrativeId}")
    @Operation(summary = "Delete narrative", description = "Delete a case narrative")
    public ResponseEntity<Void> deleteNarrative(@PathVariable String narrativeId) {
        try {
            caseNarrativeService.deleteNarrative(narrativeId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/case/{caseId}/type/{narrativeType}")
    @Operation(summary = "Get narratives by type", description = "Retrieve narratives filtered by type")
    public ResponseEntity<List<CaseNarrativeResponse>> getNarrativesByType(
            @PathVariable String caseId,
            @PathVariable String narrativeType) {
        List<CaseNarrativeResponse> narratives = caseNarrativeService.getNarrativesByType(caseId, narrativeType);
        return ResponseEntity.ok(narratives);
    }

    @GetMapping("/case/{caseId}/counts")
    @Operation(summary = "Get narrative counts", description = "Get counts of active and recalled narratives for a case")
    public ResponseEntity<Map<String, Long>> getNarrativeCounts(@PathVariable String caseId) {
        long activeCount = caseNarrativeService.getActiveNarrativesCount(caseId);
        long recalledCount = caseNarrativeService.getRecalledNarrativesCount(caseId);
        
        Map<String, Long> counts = Map.of(
                "active", activeCount,
                "recalled", recalledCount,
                "total", activeCount + recalledCount
        );
        
        return ResponseEntity.ok(counts);
    }
}