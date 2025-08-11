package com.flowable.wrapper.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null fields from JSON output
@Schema(description = "Response after task completion, handles success and validation failure")
public class TaskCompletionResponse {

    @Schema(description = "Status of the completion attempt", example = "VALIDATION_FAILED")
    private String status;

    @Schema(description = "A message describing the outcome", example = "Please correct the errors and resubmit")
    private String message;

    @Schema(description = "Completed task ID", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String taskId;

    @Schema(description = "Task name", example = "Submit Expense Report")
    private String taskName;

    @Schema(description = "Process instance ID", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String processInstanceId;

    @Schema(description = "Completion time")
    private Instant completedAt;

    @Schema(description = "User who completed the task", example = "john.doe")
    private String completedBy;

    @Schema(description = "Is the process instance still active", example = "true")
    private boolean processActive;

    // Fields for successful completion with a next step
    @Schema(description = "Next task ID if process is still active and validation passed", example = "5d7e9c3a-1234-5678-9abc-def012345678")
    private String nextTaskId;

    @Schema(description = "Next task name if process is still active and validation passed", example = "Manager Approval")
    private String nextTaskName;

    @Schema(description = "Next task queue if process is still active and validation passed", example = "manager-queue")
    private String nextTaskQueue;

    // Fields for validation failure
    @Schema(description = "List of validation errors if the task looped back")
    private Object validationErrors;

    @Schema(description = "The attempt number for the task", example = "2")
    private Integer attemptNumber;

    @Schema(description = "The ID of the new task instance that was created after validation failure", example = "5d7e9c3a-9876-5432-1cba-def012345678")
    private String retryTaskId;

}
