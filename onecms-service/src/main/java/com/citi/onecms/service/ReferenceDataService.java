package com.citi.onecms.service;

import com.citi.onecms.dto.CountryClusterResponse;
import com.citi.onecms.dto.DataSourceResponse;
import com.citi.onecms.dto.EscalationMethodResponse;
import com.citi.onecms.entity.CountryCluster;
import com.citi.onecms.entity.DataSource;
import com.citi.onecms.entity.EscalationMethod;
import com.citi.onecms.repository.CountryClusterRepository;
import com.citi.onecms.repository.DataSourceRepository;
import com.citi.onecms.repository.EscalationMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReferenceDataService {

    @Autowired
    private EscalationMethodRepository escalationMethodRepository;

    @Autowired
    private DataSourceRepository dataSourceRepository;

    @Autowired
    private CountryClusterRepository countryClusterRepository;

    public List<EscalationMethodResponse> getAllEscalationMethods() {
        return escalationMethodRepository.findAllActive()
                .stream()
                .map(this::convertToEscalationMethodResponse)
                .collect(Collectors.toList());
    }

    public List<DataSourceResponse> getAllDataSources() {
        return dataSourceRepository.findAllActive()
                .stream()
                .map(this::convertToDataSourceResponse)
                .collect(Collectors.toList());
    }

    public List<CountryClusterResponse> getAllCountryClusters() {
        return countryClusterRepository.findAllActive()
                .stream()
                .map(this::convertToCountryClusterResponse)
                .collect(Collectors.toList());
    }

    public List<CountryClusterResponse> getCountriesByRegion(String region) {
        return countryClusterRepository.findByRegionAndActive(region)
                .stream()
                .map(this::convertToCountryClusterResponse)
                .collect(Collectors.toList());
    }

    public List<CountryClusterResponse> getCountriesByCluster(String clusterName) {
        return countryClusterRepository.findByClusterNameOrderByCountryName(clusterName)
                .stream()
                .map(this::convertToCountryClusterResponse)
                .collect(Collectors.toList());
    }

    // Conversion methods
    private EscalationMethodResponse convertToEscalationMethodResponse(EscalationMethod entity) {
        return new EscalationMethodResponse(
                entity.getId(),
                entity.getMethodCode(),
                entity.getMethodName(),
                entity.getDescription(),
                entity.getIsActive()
        );
    }

    private DataSourceResponse convertToDataSourceResponse(DataSource entity) {
        return new DataSourceResponse(
                entity.getId(),
                entity.getSourceCode(),
                entity.getSourceName(),
                entity.getDescription(),
                entity.getIsActive()
        );
    }

    private CountryClusterResponse convertToCountryClusterResponse(CountryCluster entity) {
        return new CountryClusterResponse(
                entity.getId(),
                entity.getCountryCode(),
                entity.getCountryName(),
                entity.getClusterName(),
                entity.getRegion(),
                entity.getIsActive()
        );
    }
}