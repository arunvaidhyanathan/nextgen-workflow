package com.citi.onecms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EscalationMethodResponse {
    private Long id;
    private String methodCode;
    private String methodName;
    private String description;
    private Boolean isActive;
}