package com.citi.onecms.repository;

import com.citi.onecms.entity.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
    
    Optional<DataSource> findBySourceCode(String sourceCode);
    
    @Query("SELECT ds FROM DataSource ds WHERE ds.isActive = true ORDER BY ds.sourceName")
    List<DataSource> findAllActive();
    
    List<DataSource> findByIsActiveTrueOrderBySourceName();
}