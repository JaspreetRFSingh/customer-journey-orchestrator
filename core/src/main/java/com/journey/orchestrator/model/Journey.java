package com.journey.orchestrator.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a customer journey with multiple steps and conditions.
 * A journey defines the path a customer can take through various touchpoints.
 */
@Data
@Builder
public class Journey {
    
    private final String journeyId;
    private final String name;
    private final String description;
    private final JourneyStatus status;
    private final List<JourneyStep> steps;
    private final JourneyTrigger trigger;
    private final JourneyConfig config;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Map<String, Object> metadata;
    
    /**
     * Entry criteria for starting a journey.
     */
    @Data
    @Builder
    public static class JourneyTrigger {
        private final TriggerType type;
        private final String eventType;
        private final Expression condition;
        private final Map<String, Object> parameters;
    }
    
    public enum TriggerType {
        EVENT_BASED,      // Triggered by a specific event
        SCHEDULED,        // Triggered at a specific time
        AUDIENCE_BASED,   // Triggered when profile matches segment
        API_INITIATED     // Triggered via API call
    }
    
    /**
     * Configuration for journey execution.
     */
    @Data
    @Builder
    public static class JourneyConfig {
        private final int maxStepsPerProfile;
        private final long journeyDurationHours;
        private final boolean allowReEntry;
        private final GoalType goalType;
        private final Expression goalCondition;
        private final List<String> suppressionSegments;
        private final Priority priority;
    }
    
    public enum GoalType {
        NONE,
        CONVERSION,
        ENGAGEMENT,
        RETENTION,
        CUSTOM
    }
    
    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    public enum JourneyStatus {
        DRAFT,
        ACTIVE,
        PAUSED,
        COMPLETED,
        ARCHIVED
    }
    
    /**
     * Factory method to create a new journey.
     */
    public static Journey create(String name, String description, JourneyTrigger trigger, 
                                  List<JourneyStep> steps, JourneyConfig config) {
        return Journey.builder()
            .journeyId(UUID.randomUUID().toString())
            .name(name)
            .description(description)
            .status(JourneyStatus.DRAFT)
            .steps(steps)
            .trigger(trigger)
            .config(config)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .metadata(Map.of())
            .build();
    }
}
