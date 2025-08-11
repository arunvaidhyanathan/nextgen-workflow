package com.citi.onecms.service;

import com.citi.onecms.entity.WorkItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WorkItemService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Generates a new work item ID in format
     * @return Generated work item ID
     */
    public String generateWorkItemId() {
        try {
            // Call database function to generate work item ID
            String workItemId = jdbcTemplate.queryForObject(
                "SELECT cms_flowable_workflow.generate_work_item_id()", 
                String.class
            );
            return workItemId;
        } catch (Exception e) {
            // Fallback to Java-based generation if database function fails
            return generateFallbackWorkItemId();
        }
    }

    /**
     * Fallback method to generate work item ID if database function is not available
     * @return Generated work item ID
     */
    private String generateFallbackWorkItemId() {
        // Get current year
        int currentYear = java.time.Year.now().getValue();
        
        // Get next sequence number (this is a simple implementation)
        // In production, you should use a database sequence or atomic counter
        long count = jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(work_item_id FROM 9 FOR 3) AS INTEGER)), 0) + 1 " +
            "FROM cms_flowable_workflow.work_items " +
            "WHERE work_item_id LIKE 'WI-" + currentYear + "-%'",
            Long.class
        );
        return String.format("WI-%d-%03d", currentYear, count);
    }

    /**
     * Sets work item ID for a new work item before saving
     * @param workItem The work item to set ID for
     */
    public void setWorkItemId(WorkItem workItem) {
        if (workItem.getWorkItemId() == null || workItem.getWorkItemId().isEmpty()) {
            workItem.setWorkItemId(generateWorkItemId());
        }
    }
}