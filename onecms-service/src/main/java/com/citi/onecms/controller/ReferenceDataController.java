package com.citi.onecms.controller;

import com.citi.onecms.dto.CountryClusterResponse;
import com.citi.onecms.dto.DataSourceResponse;
import com.citi.onecms.dto.EscalationMethodResponse;
import com.citi.onecms.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/reference-data")
@Tag(name = "Reference Data", description = "Reference data management endpoints")
public class ReferenceDataController {

    @Autowired
    private ReferenceDataService referenceDataService;

    @GetMapping("/escalation-methods")
    @Operation(summary = "Get all escalation methods", description = "Retrieve all active escalation methods")
    public ResponseEntity<List<EscalationMethodResponse>> getAllEscalationMethods() {
        List<EscalationMethodResponse> methods = referenceDataService.getAllEscalationMethods();
        return ResponseEntity.ok(methods);
    }

    @GetMapping("/data-sources")
    @Operation(summary = "Get all data sources", description = "Retrieve all active data sources")
    public ResponseEntity<List<DataSourceResponse>> getAllDataSources() {
        List<DataSourceResponse> dataSources = referenceDataService.getAllDataSources();
        return ResponseEntity.ok(dataSources);
    }

    @GetMapping("/countries-clusters")
    @Operation(summary = "Get all countries and clusters", description = "Retrieve all active countries and clusters")
    public ResponseEntity<List<CountryClusterResponse>> getAllCountryClusters() {
        List<CountryClusterResponse> countries = referenceDataService.getAllCountryClusters();
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/countries-clusters/region/{region}")
    @Operation(summary = "Get countries by region", description = "Retrieve countries filtered by region")
    public ResponseEntity<List<CountryClusterResponse>> getCountriesByRegion(@PathVariable String region) {
        List<CountryClusterResponse> countries = referenceDataService.getCountriesByRegion(region);
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/countries-clusters/cluster/{clusterName}")
    @Operation(summary = "Get countries by cluster", description = "Retrieve countries filtered by cluster name")
    public ResponseEntity<List<CountryClusterResponse>> getCountriesByCluster(@PathVariable String clusterName) {
        List<CountryClusterResponse> countries = referenceDataService.getCountriesByCluster(clusterName);
        return ResponseEntity.ok(countries);
    }
}