package com.citi.onecms.service;

import com.citi.onecms.dto.CaseSummaryResponse;
import com.citi.onecms.dto.EmailCaseUploadResponse;
import com.citi.onecms.entity.CaseCreationEmail;
import com.citi.onecms.entity.CaseSummary;
import com.citi.onecms.repository.CaseCreationEmailRepository;
import com.citi.onecms.repository.CaseSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.Properties;

@Service
@Transactional
public class EmailCaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailCaseService.class);
    
    @Autowired
    private CaseCreationEmailRepository caseCreationEmailRepository;
    
    @Autowired
    private CaseSummaryRepository caseSummaryRepository;
    
    /**
     * Process uploaded email file and store for LLM processing
     * @param file The uploaded .eml or .msg file
     * @return Response with call ID and processing status
     */
    public EmailCaseUploadResponse processEmailUpload(MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File is empty");
            }
            
            if (!isValidEmailFile(file)) {
                throw new IllegalArgumentException("Invalid file type. Only .eml and .msg files are supported");
            }
            
            // Extract email metadata
            EmailMetadata metadata = extractEmailMetadata(file);
            
            // Create case creation email record
            CaseCreationEmail caseCreationEmail = new CaseCreationEmail(
                CaseCreationEmail.STATUS_PENDING,
                metadata.getSenderEmail(),
                metadata.getSenderName(),
                metadata.getSubject(),
                metadata.getBodyText(),
                file.getBytes()
            );
            
            // Save to database
            CaseCreationEmail saved = caseCreationEmailRepository.save(caseCreationEmail);
            
            // TODO: Trigger asynchronous LLM processing
            // This would typically involve publishing to a message queue or scheduling a background task
            logger.info("Email upload processed. Call ID: {}, Status: PENDING", saved.getCallId());
            
            return new EmailCaseUploadResponse(
                saved.getCallId(),
                "PROCESSING",
                "Email intake started asynchronously."
            );
            
        } catch (Exception e) {
            logger.error("Error processing email upload", e);
            throw new RuntimeException("Failed to process email upload: " + e.getMessage(), e);
        }
    }
    
    /**
     * Retrieve case summary by case ID and workflow step
     * @param caseId The case ID
     * @param stepName The workflow step name
     * @return Case summary response
     */
    public CaseSummaryResponse getCaseSummary(Long caseId, String stepName) {
        Optional<CaseSummary> caseSummary = caseSummaryRepository.findByCaseIdAndStatusId(caseId, stepName);
        
        if (caseSummary.isEmpty()) {
            return null; // Controller will handle 404
        }
        
        CaseSummary summary = caseSummary.get();
        return new CaseSummaryResponse(
            summary.getCaseSummariesId(),
            summary.getCaseId(),
            summary.getStatusId(),
            summary.getSummaryText(),
            summary.getCreatedAt()
        );
    }
    
    /**
     * Validate if the uploaded file is a valid email file
     */
    private boolean isValidEmailFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return false;
        }
        
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".eml") || lowerFilename.endsWith(".msg");
    }
    
    /**
     * Extract email metadata from the uploaded file
     */
    private EmailMetadata extractEmailMetadata(MultipartFile file) {
        try {
            EmailMetadata metadata = new EmailMetadata();
            
            // For .eml files, use JavaMail to parse
            if (file.getOriginalFilename().toLowerCase().endsWith(".eml")) {
                Properties props = System.getProperties();
                Session session = Session.getDefaultInstance(props);
                
                MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(file.getBytes()));
                
                // Extract sender information
                InternetAddress[] fromAddresses = (InternetAddress[]) message.getFrom();
                if (fromAddresses != null && fromAddresses.length > 0) {
                    metadata.setSenderEmail(fromAddresses[0].getAddress());
                    metadata.setSenderName(fromAddresses[0].getPersonal());
                }
                
                // Extract subject
                metadata.setSubject(message.getSubject());
                
                // Extract body text (simplified - real implementation would handle multipart)
                Object content = message.getContent();
                if (content instanceof String) {
                    metadata.setBodyText((String) content);
                } else {
                    metadata.setBodyText("Complex email content - see raw attachment");
                }
                
            } else {
                // For .msg files, implement basic parsing or use a library like poi-scratchpad
                // For now, set minimal metadata
                metadata.setSenderEmail("unknown@example.com");
                metadata.setSenderName("Unknown Sender");
                metadata.setSubject("MSG File - Subject extraction not implemented");
                metadata.setBodyText("MSG file content - requires specialized parsing");
            }
            
            return metadata;
            
        } catch (Exception e) {
            logger.warn("Error extracting email metadata, using defaults", e);
            
            // Return default metadata if parsing fails
            EmailMetadata metadata = new EmailMetadata();
            metadata.setSenderEmail("unknown@example.com");
            metadata.setSenderName("Unknown");
            metadata.setSubject("Email parsing failed");
            metadata.setBodyText("Failed to extract email content");
            return metadata;
        }
    }
    
    /**
     * Inner class to hold email metadata
     */
    private static class EmailMetadata {
        private String senderEmail;
        private String senderName;
        private String subject;
        private String bodyText;
        
        // Getters and setters
        public String getSenderEmail() { return senderEmail; }
        public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
        
        public String getSenderName() { return senderName; }
        public void setSenderName(String senderName) { this.senderName = senderName; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getBodyText() { return bodyText; }
        public void setBodyText(String bodyText) { this.bodyText = bodyText; }
    }
}