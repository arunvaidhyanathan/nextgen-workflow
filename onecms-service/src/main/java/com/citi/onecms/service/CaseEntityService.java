package com.citi.onecms.service;

import com.citi.onecms.dto.CaseEntityRequest;
import com.citi.onecms.dto.CaseEntityResponse;
import com.citi.onecms.entity.CaseEntity;
import com.citi.onecms.repository.CaseEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CaseEntityService {

    @Autowired
    private CaseEntityRepository caseEntityRepository;

    public List<CaseEntityResponse> getEntitiesByCaseId(String caseId) {
        return caseEntityRepository.findByCaseIdOrderByCreatedAt(caseId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CaseEntityResponse> getEntitiesByCaseIdAndType(String caseId, CaseEntity.EntityType entityType) {
        return caseEntityRepository.findByCaseIdAndEntityType(caseId, entityType)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Optional<CaseEntityResponse> getEntityById(String entityId) {
        return caseEntityRepository.findByEntityId(entityId)
                .map(this::convertToResponse);
    }

    public CaseEntityResponse createEntity(CaseEntityRequest request) {
        CaseEntity entity = new CaseEntity();
        entity.setEntityId("ENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        entity.setCaseId(request.getCaseId());
        entity.setEntityType(request.getEntityType());
        entity.setInvestigationFunction(request.getInvestigationFunction());
        entity.setRelationshipType(request.getRelationshipType());

        // Person fields
        if (request.getEntityType() == CaseEntity.EntityType.PERSON) {
            entity.setSoeid(request.getSoeid());
            entity.setGeid(request.getGeid());
            entity.setFirstName(request.getFirstName());
            entity.setMiddleName(request.getMiddleName());
            entity.setLastName(request.getLastName());
            entity.setEmailAddress(request.getEmailAddress());
            entity.setPhoneNumber(request.getPhoneNumber());
            entity.setAddress(request.getAddress());
            entity.setCity(request.getCity());
            entity.setState(request.getState());
            entity.setZipCode(request.getZipCode());
        }

        // Organization fields
        if (request.getEntityType() == CaseEntity.EntityType.ORGANIZATION) {
            entity.setOrganizationName(request.getOrganizationName());
            entity.setOrganizationType(request.getOrganizationType());
        }

        CaseEntity savedEntity = caseEntityRepository.save(entity);
        return convertToResponse(savedEntity);
    }

    public CaseEntityResponse updateEntity(String entityId, CaseEntityRequest request) {
        CaseEntity entity = caseEntityRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Entity not found with ID: " + entityId));

        entity.setInvestigationFunction(request.getInvestigationFunction());
        entity.setRelationshipType(request.getRelationshipType());

        // Person fields
        if (request.getEntityType() == CaseEntity.EntityType.PERSON) {
            entity.setSoeid(request.getSoeid());
            entity.setGeid(request.getGeid());
            entity.setFirstName(request.getFirstName());
            entity.setMiddleName(request.getMiddleName());
            entity.setLastName(request.getLastName());
            entity.setEmailAddress(request.getEmailAddress());
            entity.setPhoneNumber(request.getPhoneNumber());
            entity.setAddress(request.getAddress());
            entity.setCity(request.getCity());
            entity.setState(request.getState());
            entity.setZipCode(request.getZipCode());
        }

        // Organization fields
        if (request.getEntityType() == CaseEntity.EntityType.ORGANIZATION) {
            entity.setOrganizationName(request.getOrganizationName());
            entity.setOrganizationType(request.getOrganizationType());
        }

        CaseEntity savedEntity = caseEntityRepository.save(entity);
        return convertToResponse(savedEntity);
    }

    public void deleteEntity(String entityId) {
        CaseEntity entity = caseEntityRepository.findByEntityId(entityId)
                .orElseThrow(() -> new RuntimeException("Entity not found with ID: " + entityId));
        caseEntityRepository.delete(entity);
    }

    public List<CaseEntityResponse> searchEntitiesBySoeid(String soeid) {
        return caseEntityRepository.findBySoeid(soeid)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<CaseEntityResponse> searchEntitiesByEmail(String email) {
        return caseEntityRepository.findByEmailAddress(email)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private CaseEntityResponse convertToResponse(CaseEntity entity) {
        CaseEntityResponse response = new CaseEntityResponse();
        response.setId(entity.getId());
        response.setEntityId(entity.getEntityId());
        response.setCaseId(entity.getCaseId());
        response.setEntityType(entity.getEntityType());
        response.setInvestigationFunction(entity.getInvestigationFunction());
        response.setRelationshipType(entity.getRelationshipType());

        // Person fields
        response.setSoeid(entity.getSoeid());
        response.setGeid(entity.getGeid());
        response.setFirstName(entity.getFirstName());
        response.setMiddleName(entity.getMiddleName());
        response.setLastName(entity.getLastName());
        response.setEmailAddress(entity.getEmailAddress());
        response.setPhoneNumber(entity.getPhoneNumber());
        response.setAddress(entity.getAddress());
        response.setCity(entity.getCity());
        response.setState(entity.getState());
        response.setZipCode(entity.getZipCode());

        // Organization fields
        response.setOrganizationName(entity.getOrganizationName());
        response.setOrganizationType(entity.getOrganizationType());

        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setDisplayName(entity.getDisplayName());

        return response;
    }
}