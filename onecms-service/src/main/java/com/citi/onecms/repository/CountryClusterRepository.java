package com.citi.onecms.repository;

import com.citi.onecms.entity.CountryCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryClusterRepository extends JpaRepository<CountryCluster, Long> {
    
    Optional<CountryCluster> findByCountryCode(String countryCode);
    
    @Query("SELECT cc FROM CountryCluster cc WHERE cc.isActive = true ORDER BY cc.countryName")
    List<CountryCluster> findAllActive();
    
    List<CountryCluster> findByIsActiveTrueOrderByCountryName();
    
    List<CountryCluster> findByClusterNameOrderByCountryName(String clusterName);
    
    @Query("SELECT cc FROM CountryCluster cc WHERE cc.region = ?1 AND cc.isActive = true ORDER BY cc.countryName")
    List<CountryCluster> findByRegionAndActive(String region);
}