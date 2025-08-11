package com.citi.onecms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceResponse {
    private Long id;
    private String sourceCode;
    private String sourceName;
    private String description;
    private Boolean isActive;
}