package com.citi.onecms.service;

import com.citi.onecms.dto.CaseNarrativeRequest;
import com.citi.onecms.dto.CaseNarrativeResponse;
import com.citi.onecms.entity.CaseNarrative;
import com.citi.onecms.repository.CaseNarrativeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaseNarrativeService {

    @Autowired
    private CaseNarrativeRepository caseNarrativeRepository;


    public List<CaseNarrativeResponse> getNarrativesByCaseId(String caseId) {
        return caseNarrativeRepository.findByCaseIdOrderByCreatedAt(caseId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CaseNarrativeResponse> getActiveNarrativesByCaseId(String caseId) {
        return caseNarrativeRepository.findByCaseIdAndIsRecalledFalseOrderByCreatedAt(caseId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Optional<CaseNarrativeResponse> getNarrativeById(String narrativeId) {
        return caseNarrativeRepository.findByNarrativeId(narrativeId)
                .map(this::convertToResponse);
    }

    public CaseNarrativeResponse createNarrative(CaseNarrativeRequest request) {
        CaseNarrative narrative = new CaseNarrative();
        narrative.setNarrativeId("NAR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        narrative.setCaseId(request.getCaseId());
        narrative.setInvestigationFunction(request.getInvestigationFunction());
        narrative.setNarrativeType(request.getNarrativeType());
        narrative.setNarrativeTitle(request.getNarrativeTitle());
        narrative.setNarrativeText(request.getNarrativeText());
        narrative.setIsRecalled(request.getIsRecalled());

        // TODO: Set created by from user context passed in request or header
        // For now, leave as null since auth is handled by entitlement-service
        narrative.setCreatedBy(null);

        CaseNarrative savedNarrative = caseNarrativeRepository.save(narrative);
        return convertToResponse(savedNarrative);
    }

    public CaseNarrativeResponse updateNarrative(String narrativeId, CaseNarrativeRequest request) {
        CaseNarrative narrative = caseNarrativeRepository.findByNarrativeId(narrativeId)
                .orElseThrow(() -> new RuntimeException("Narrative not found with ID: " + narrativeId));

        narrative.setInvestigationFunction(request.getInvestigationFunction());
        narrative.setNarrativeType(request.getNarrativeType());
        narrative.setNarrativeTitle(request.getNarrativeTitle());
        narrative.setNarrativeText(request.getNarrativeText());

        CaseNarrative savedNarrative = caseNarrativeRepository.save(narrative);
        return convertToResponse(savedNarrative);
    }

    public void recallNarrative(String narrativeId) {
        CaseNarrative narrative = caseNarrativeRepository.findByNarrativeId(narrativeId)
                .orElseThrow(() -> new RuntimeException("Narrative not found with ID: " + narrativeId));
        
        narrative.setIsRecalled(true);
        caseNarrativeRepository.save(narrative);
    }

    public void deleteNarrative(String narrativeId) {
        CaseNarrative narrative = caseNarrativeRepository.findByNarrativeId(narrativeId)
                .orElseThrow(() -> new RuntimeException("Narrative not found with ID: " + narrativeId));
        caseNarrativeRepository.delete(narrative);
    }

    public List<CaseNarrativeResponse> getNarrativesByType(String caseId, String narrativeType) {
        return caseNarrativeRepository.findByCaseIdAndType(caseId, narrativeType)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public long getActiveNarrativesCount(String caseId) {
        return caseNarrativeRepository.countActiveByCaseId(caseId);
    }

    public long getRecalledNarrativesCount(String caseId) {
        return caseNarrativeRepository.countRecalledByCaseId(caseId);
    }

    private CaseNarrativeResponse convertToResponse(CaseNarrative narrative) {
        CaseNarrativeResponse response = new CaseNarrativeResponse();
        response.setId(narrative.getId());
        response.setNarrativeId(narrative.getNarrativeId());
        response.setCaseId(narrative.getCaseId());
        response.setInvestigationFunction(narrative.getInvestigationFunction());
        response.setNarrativeType(narrative.getNarrativeType());
        response.setNarrativeTitle(narrative.getNarrativeTitle());
        response.setNarrativeText(narrative.getNarrativeText());
        response.setIsRecalled(narrative.getIsRecalled());
        response.setCreatedBy(narrative.getCreatedBy());
        response.setCreatedAt(narrative.getCreatedAt());
        response.setUpdatedAt(narrative.getUpdatedAt());

        // TODO: Get user name from entitlement-service if needed
        // For now, just use the user ID
        if (narrative.getCreatedBy() != null) {
            response.setCreatedByName("User " + narrative.getCreatedBy());
        }

        return response;
    }
}