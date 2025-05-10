package com.journey.orchestrator.model;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Represents a single step within a customer journey.
 * Each step can have actions, conditions, and transitions.
 */
@Data
@Builder
public class JourneyStep {
    
    private final String stepId;
    private final String name;
    private final StepType type;
    private final int order;
    private final Expression entryCondition;
    private final List<Action> actions;
    private final List<Transition> transitions;
    private final Duration waitDuration;
    private final Map<String, Object> config;
    
    public enum StepType {
        START,              // Entry point
        ACTION,             // Execute an action (send message, etc.)
        DECISION,           // Branch based on condition
        WAIT,               // Wait for time or event
        SPLIT,              // A/B test or multi-way split
        JOIN,               // Merge branches
        GOAL,               // Check if goal achieved
        END                 // Exit point
    }
    
    /**
     * Action to be executed at this step.
     */
    @Data
    @Builder
    public static class Action {
        private final String actionId;
        private final ActionType type;
        private final Channel channel;
        private final Map<String, Object> parameters;
        private final Expression condition;
        private final PersonalizationConfig personalization;
    }
    
    public enum ActionType {
        SEND_MESSAGE,
        UPDATE_PROFILE,
        TRIGGER_EVENT,
        CALL_API,
        ASSIGN_SCORE,
        ADD_TO_SEGMENT,
        REMOVE_FROM_SEGMENT
    }
    
    /**
     * Transition to next step based on condition.
     */
    @Data
    @Builder
    public static class Transition {
        private final String transitionId;
        private final String targetStepId;
        private final Expression condition;
        private final int priority;
    }
    
    /**
     * Personalization configuration for dynamic content.
     */
    @Data
    @Builder
    public static class PersonalizationConfig {
        private final Map<String, String> contentVariants;
        private final Expression variantSelector;
        private final Map<String, Object> fallbackValues;
    }
}
