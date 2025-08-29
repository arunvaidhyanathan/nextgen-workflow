package com.citi.onecms.repository;

import com.citi.onecms.entity.CaseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaseTypeRepository extends JpaRepository<CaseType, Long> {
    Optional<CaseType> findById(Long caseTypeId);
    Optional<CaseType> findByTypeName(String typeName);
    List<CaseType> findByActiveTrue();
    boolean existsByTypeName(String typeName);
}