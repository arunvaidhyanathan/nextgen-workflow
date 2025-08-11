package com.citi.onecms.service;

import com.citi.onecms.dto.CaseWithAllegationsResponse;
import com.citi.onecms.entity.CaseStatus;
import com.citi.onecms.entity.Priority;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple case service that provides basic dashboard data without complex entity relationships.
 * This allows the dashboard to function while entity mappings are being fixed.
 */
@Service
@Slf4j
public class SimpleCaseService {
    
    public List<CaseWithAllegationsResponse> getDashboardCases(int page, int size, String status) {
        log.info("Fetching dashboard cases - page: {}, size: {}, status: {}", page, size, status);
        
        List<CaseWithAllegationsResponse> cases = new ArrayList<>();
        
        // Create sample cases for alice.intake (intake officer)
        for (int i = 1; i <= Math.min(size, 5); i++) {
            CaseWithAllegationsResponse caseResponse = new CaseWithAllegationsResponse();
            caseResponse.setCaseId("case-" + i);
            caseResponse.setCaseNumber("CMS-2025-" + String.format("%03d", i));
            caseResponse.setTitle("Case " + i + " - Investigation Required");
            caseResponse.setDescription("Sample case for dashboard testing");
            caseResponse.setPriority(i <= 2 ? Priority.HIGH : Priority.MEDIUM);
            caseResponse.setStatus(i == 1 ? CaseStatus.OPEN : CaseStatus.IN_PROGRESS);
            caseResponse.setComplainantName("Complainant " + i);
            caseResponse.setComplainantEmail("complainant" + i + "@example.com");
            caseResponse.setCreatedAt(LocalDateTime.now().minusDays(i));
            caseResponse.setUpdatedAt(LocalDateTime.now().minusHours(i * 2));
            caseResponse.setCreatedBy("alice.intake");
            
            // Add to list if status filter matches or no filter
            if (status == null || status.isEmpty() || 
                caseResponse.getStatus().toString().equalsIgnoreCase(status)) {
                cases.add(caseResponse);
            }
        }
        
        log.info("Returning {} dashboard cases", cases.size());
        return cases;
    }
    
    public Map<String, Object> getDashboardStats() {
        log.info("Fetching dashboard stats");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Basic stats for intake officer dashboard
        stats.put("allOpenCases", 5L);
        stats.put("openInvestigations", 2L);
        stats.put("totalCases", 10L);
        stats.put("myAssignedCases", 3L);
        stats.put("pendingReview", 1L);
        
        log.info("Dashboard stats: {}", stats);
        return stats;
    }
    
    public CaseWithAllegationsResponse getCaseByNumber(String caseNumber) {
        log.info("Fetching case by number: {}", caseNumber);
        
        // Return a sample case for now
        CaseWithAllegationsResponse caseResponse = new CaseWithAllegationsResponse();
        caseResponse.setCaseId("case-1");
        caseResponse.setCaseNumber(caseNumber);
        caseResponse.setTitle("Sample Case - " + caseNumber);
        caseResponse.setDescription("This is a sample case for testing purposes");
        caseResponse.setPriority(Priority.MEDIUM);
        caseResponse.setStatus(CaseStatus.OPEN);
        caseResponse.setComplainantName("Sample Complainant");
        caseResponse.setComplainantEmail("complainant@example.com");
        caseResponse.setCreatedAt(LocalDateTime.now().minusDays(1));
        caseResponse.setUpdatedAt(LocalDateTime.now().minusHours(2));
        caseResponse.setCreatedBy("alice.intake");
        
        return caseResponse;
    }
}