package com.citi.onecms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CountryClusterResponse {
    private Long id;
    private String countryCode;
    private String countryName;
    private String clusterName;
    private String region;
    private Boolean isActive;
}