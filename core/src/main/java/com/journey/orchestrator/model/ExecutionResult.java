package com.journey.orchestrator.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Result of executing an action or step in a journey.
 */
@Data
@Builder
public class ExecutionResult {
    
    private final String executionId;
    private final String journeyId;
    private final String profileId;
    private final String stepId;
    private final String actionId;
    private final ExecutionStatus status;
    private final Channel channel;
    private final String message;
    private final Map<String, Object> responseData;
    private final Instant executedAt;
    private final long durationMs;
    private final Exception error;
    
    public enum ExecutionStatus {
        PENDING,
        IN_PROGRESS,
        SUCCESS,
        FAILED,
        SKIPPED,
        TIMEOUT,
        CANCELLED
    }
    
    /**
     * Creates a successful execution result.
     */
    public static ExecutionResult success(String executionId, String journeyId, String profileId,
                                           String stepId, Channel channel, Map<String, Object> responseData) {
        return ExecutionResult.builder()
            .executionId(executionId)
            .journeyId(journeyId)
            .profileId(profileId)
            .stepId(stepId)
            .channel(channel)
            .status(ExecutionStatus.SUCCESS)
            .message("Action executed successfully")
            .responseData(responseData)
            .executedAt(Instant.now())
            .durationMs(0)
            .build();
    }
    
    /**
     * Creates a failed execution result.
     */
    public static ExecutionResult failure(String executionId, String journeyId, String profileId,
                                           String stepId, Channel channel, String message, Exception error) {
        return ExecutionResult.builder()
            .executionId(executionId)
            .journeyId(journeyId)
            .profileId(profileId)
            .stepId(stepId)
            .channel(channel)
            .status(ExecutionStatus.FAILED)
            .message(message)
            .error(error)
            .executedAt(Instant.now())
            .build();
    }
    
    /**
     * Creates a skipped execution result.
     */
    public static ExecutionResult skipped(String executionId, String journeyId, String profileId,
                                           String stepId, String reason) {
        return ExecutionResult.builder()
            .executionId(executionId)
            .journeyId(journeyId)
            .profileId(profileId)
            .stepId(stepId)
            .status(ExecutionStatus.SKIPPED)
            .message(reason)
            .executedAt(Instant.now())
            .build();
    }
}
