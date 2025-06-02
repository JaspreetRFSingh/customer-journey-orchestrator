package com.journey.orchestrator.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the execution state of a profile through a journey.
 * This is crucial for resuming journeys and maintaining state across sessions.
 */
@Data
@Builder
public class JourneyExecutionState {
    
    private final String executionStateId;
    private final String journeyId;
    private final String profileId;
    private final ExecutionState status;
    private final String currentStepId;
    private final List<String> completedSteps;
    private final List<ExecutionResult> executionHistory;
    private final Map<String, Object> variables;
    private final int stepCount;
    private final Instant startedAt;
    private final Instant lastUpdatedAt;
    private final Instant expiresAt;
    private final JourneyExitReason exitReason;
    
    public enum ExecutionState {
        NOT_STARTED,
        ACTIVE,
        WAITING,
        COMPLETED,
        EXITED,
        FAILED,
        EXPIRED
    }
    
    /**
     * Factory method to create a new execution state.
     */
    public static JourneyExecutionState create(String journeyId, String profileId) {
        return JourneyExecutionState.builder()
            .executionStateId(UUID.randomUUID().toString())
            .journeyId(journeyId)
            .profileId(profileId)
            .status(ExecutionState.NOT_STARTED)
            .currentStepId(null)
            .completedSteps(new ArrayList<>())
            .executionHistory(new ArrayList<>())
            .variables(Map.of())
            .stepCount(0)
            .startedAt(Instant.now())
            .lastUpdatedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(30L * 24 * 60 * 60)) // 30 days
            .exitReason(null)
            .build();
    }
    
    /**
     * Advances to the next step and returns a new state.
     */
    public JourneyExecutionState advanceToStep(String stepId) {
        List<String> newCompletedSteps = new ArrayList<>(this.completedSteps);
        if (this.currentStepId != null) {
            newCompletedSteps.add(this.currentStepId);
        }
        
        return JourneyExecutionState.builder()
            .executionStateId(this.executionStateId)
            .journeyId(this.journeyId)
            .profileId(this.profileId)
            .status(ExecutionState.ACTIVE)
            .currentStepId(stepId)
            .completedSteps(newCompletedSteps)
            .executionHistory(this.executionHistory)
            .variables(this.variables)
            .stepCount(this.stepCount + 1)
            .startedAt(this.startedAt)
            .lastUpdatedAt(Instant.now())
            .expiresAt(this.expiresAt)
            .exitReason(this.exitReason)
            .build();
    }
    
    /**
     * Adds an execution result to the history.
     */
    public JourneyExecutionState addExecutionResult(ExecutionResult result) {
        List<ExecutionResult> newHistory = new ArrayList<>(this.executionHistory);
        newHistory.add(result);
        
        return JourneyExecutionState.builder()
            .executionStateId(this.executionStateId)
            .journeyId(this.journeyId)
            .profileId(this.profileId)
            .status(this.status)
            .currentStepId(this.currentStepId)
            .completedSteps(this.completedSteps)
            .executionHistory(newHistory)
            .variables(this.variables)
            .stepCount(this.stepCount)
            .startedAt(this.startedAt)
            .lastUpdatedAt(Instant.now())
            .expiresAt(this.expiresAt)
            .exitReason(this.exitReason)
            .build();
    }
    
    /**
     * Sets the state to waiting.
     */
    public JourneyExecutionState waiting() {
        return JourneyExecutionState.builder()
            .executionStateId(this.executionStateId)
            .journeyId(this.journeyId)
            .profileId(this.profileId)
            .status(ExecutionState.WAITING)
            .currentStepId(this.currentStepId)
            .completedSteps(this.completedSteps)
            .executionHistory(this.executionHistory)
            .variables(this.variables)
            .stepCount(this.stepCount)
            .startedAt(this.startedAt)
            .lastUpdatedAt(Instant.now())
            .expiresAt(this.expiresAt)
            .exitReason(this.exitReason)
            .build();
    }
    
    /**
     * Marks the journey as completed.
     */
    public JourneyExecutionState completed() {
        return JourneyExecutionState.builder()
            .executionStateId(this.executionStateId)
            .journeyId(this.journeyId)
            .profileId(this.profileId)
            .status(ExecutionState.COMPLETED)
            .currentStepId(null)
            .completedSteps(this.completedSteps)
            .executionHistory(this.executionHistory)
            .variables(this.variables)
            .stepCount(this.stepCount)
            .startedAt(this.startedAt)
            .lastUpdatedAt(Instant.now())
            .expiresAt(this.expiresAt)
            .exitReason(JourneyExitReason.COMPLETED)
            .build();
    }
    
    /**
     * Checks if a specific step has been completed.
     */
    public boolean hasCompletedStep(String stepId) {
        return completedSteps.contains(stepId);
    }
}
