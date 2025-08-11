package com.flowable.wrapper.repository;

import com.flowable.wrapper.entity.WorkflowMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowMetadataRepository extends JpaRepository<WorkflowMetadata, Long> {
    
    Optional<WorkflowMetadata> findByProcessDefinitionKey(String processDefinitionKey);
    
    Optional<WorkflowMetadata> findByProcessDefinitionKeyAndActiveTrue(String processDefinitionKey);
    
    boolean existsByProcessDefinitionKey(String processDefinitionKey);
    
    List<WorkflowMetadata> findByBusinessAppNameAndActiveTrue(String businessAppName);
}