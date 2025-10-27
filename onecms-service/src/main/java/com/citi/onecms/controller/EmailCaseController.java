package com.citi.onecms.controller;

import com.citi.onecms.dto.CaseSummaryResponse;
import com.citi.onecms.dto.EmailCaseUploadResponse;
import com.citi.onecms.dto.ErrorResponse;
import com.citi.onecms.service.AuthorizationService;
import com.citi.onecms.service.EmailCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/emails")
@Tag(name = "Email Case Intake", description = "APIs for LLM-driven case creation from email files and case summary retrieval")
public class EmailCaseController {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailCaseController.class);
    
    @Autowired
    private EmailCaseService emailCaseService;
    
    @Autowired
    private AuthorizationService authorizationService;
    
    /**
     * Upload email file and asynchronously create case via LLM extraction
     */
    @PostMapping(value = "/createcaseupload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Upload email file and asynchronously create case via LLM extraction",
        description = "Accepts .eml or .msg files, stores raw email data, and triggers asynchronous LLM processing for case creation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202", 
            description = "Accepted - Email file saved and LLM processing job triggered",
            content = @Content(schema = @Schema(implementation = EmailCaseUploadResponse.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Bad Request - Missing file or invalid file type",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - Invalid or missing session",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal Server Error - Database or storage error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> createCaseFromEmail(
        @Parameter(description = "The .eml or .msg file to be processed", required = true)
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        try {
            // Extract user ID from request headers (set by API Gateway)
            String userId = request.getHeader("X-User-Id");
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User authentication required"));
            }
            
            // TODO: Add authorization check for email case creation
            // For now, allow all authenticated users to upload emails
            
            // Validate file parameter
            if (file == null || file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "File parameter is required"));
            }
            
            // Process email upload
            EmailCaseUploadResponse response = emailCaseService.processEmailUpload(file);
            
            logger.info("Email case upload processed successfully. Call ID: {}, User: {}", 
                       response.getCallId(), userId);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid email upload request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Error processing email upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Failed to process email upload"));
        }
    }
    
    /**
     * Retrieve a historical case summary snapshot for a specific workflow step
     */
    @GetMapping("/casesummary")
    @Operation(
        summary = "Retrieve a historical case summary snapshot for a specific workflow step",
        description = "Returns the case summary text for a given case ID and workflow step"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "OK - Returns the case summary record",
            content = @Content(schema = @Schema(implementation = CaseSummaryResponse.class))
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Not Found - No summary exists for the given step",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - Invalid or missing session",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Forbidden - Access denied to case summary",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> getCaseSummaryByStep(
        @Parameter(description = "The unique identifier of the case", required = true)
        @RequestParam Long caseID,
        @Parameter(description = "The workflow step whose summary snapshot is requested", required = true)
        @RequestParam String stepName,
        HttpServletRequest request
    ) {
        try {
            // Extract user ID from request headers (set by API Gateway)
            String userId = request.getHeader("X-User-Id");
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("UNAUTHORIZED", "User authentication required"));
            }
            
            // TODO: Add authorization check for case summary access
            // Check if user has permission to view this case summary
            // For now, allow all authenticated users to view summaries
            
            // Validate step name
            if (!isValidStepName(stepName)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("BAD_REQUEST", "Invalid step name: " + stepName));
            }
            
            // Retrieve case summary
            CaseSummaryResponse summary = emailCaseService.getCaseSummary(caseID, stepName);
            
            if (summary == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", 
                           "No summary exists for case " + caseID + " and step " + stepName));
            }
            
            logger.info("Case summary retrieved. Case ID: {}, Step: {}, User: {}", 
                       caseID, stepName, userId);
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            logger.error("Error retrieving case summary for case {} and step {}", caseID, stepName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Failed to retrieve case summary"));
        }
    }
    
    /**
     * Validate workflow step name against allowed values
     */
    private boolean isValidStepName(String stepName) {
        return stepName != null && (
            stepName.equals("LLM_INITIAL") ||
            stepName.equals("EO_INTAKE_ANALYST") ||
            stepName.equals("EO_INTAKE_DIRECTOR") ||
            stepName.equals("INVESTIGATION_COMPLETE")
        );
    }
}