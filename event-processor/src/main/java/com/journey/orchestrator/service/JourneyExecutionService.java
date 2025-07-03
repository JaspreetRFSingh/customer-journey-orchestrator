package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Event;
import com.journey.orchestrator.model.Journey;
import com.journey.orchestrator.model.JourneyExecutionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing journey execution states.
 * Handles starting, resuming, and tracking journey progress for each profile.
 */
public class JourneyExecutionService {
    
    private static final Logger LOG = LoggerFactory.getLogger(JourneyExecutionService.class);
    
    private final Map<String, JourneyExecutionState> executionStates;
    private final Map<String, JourneyExecutionState> profileJourneyIndex;
    private final JourneyOrchestrator orchestrator;
    
    public JourneyExecutionService(JourneyOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.executionStates = new ConcurrentHashMap<>();
        this.profileJourneyIndex = new ConcurrentHashMap<>();
    }
    
    /**
     * Starts a journey for a profile.
     */
    public void startJourney(Journey journey, String profileId, Event triggerEvent) {
        String key = getExecutionKey(journey.getJourneyId(), profileId);
        
        // Check if already running (unless re-entry is allowed)
        JourneyExecutionState existing = executionStates.get(key);
        if (existing != null && existing.getStatus() == JourneyExecutionState.ExecutionState.ACTIVE) {
            if (journey.getConfig() != null && !journey.getConfig().isAllowReEntry()) {
                LOG.debug("Journey {} already active for profile {}, skipping", 
                    journey.getJourneyId(), profileId);
                return;
            }
        }
        
        // Create new execution state
        JourneyExecutionState newState = JourneyExecutionState.create(
            journey.getJourneyId(), profileId);
        
        executionStates.put(key, newState);
        profileJourneyIndex.put(key, newState);
        
        LOG.info("Started journey {} for profile {}", journey.getJourneyId(), profileId);
        
        // Trigger orchestration
        orchestrator.executeJourney(journey, newState, triggerEvent);
    }
    
    /**
     * Gets the execution state for a journey and profile.
     */
    public Optional<JourneyExecutionState> getExecutionState(String journeyId, String profileId) {
        return Optional.ofNullable(executionStates.get(getExecutionKey(journeyId, profileId)));
    }
    
    /**
     * Updates the execution state.
     */
    public void updateExecutionState(JourneyExecutionState state) {
        String key = getExecutionKey(state.getJourneyId(), state.getProfileId());
        executionStates.put(key, state);
    }
    
    /**
     * Gets all active executions for a profile.
     */
    public Map<String, JourneyExecutionState> getActiveExecutionsForProfile(String profileId) {
        return profileJourneyIndex.entrySet().stream()
            .filter(e -> e.getKey().contains(profileId))
            .filter(e -> e.getValue().getStatus() == JourneyExecutionState.ExecutionState.ACTIVE ||
                        e.getValue().getStatus() == JourneyExecutionState.ExecutionState.WAITING)
            .collect(Map.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Removes an execution state.
     */
    public void removeExecutionState(String journeyId, String profileId) {
        String key = getExecutionKey(journeyId, profileId);
        executionStates.remove(key);
        profileJourneyIndex.remove(key);
    }
    
    private String getExecutionKey(String journeyId, String profileId) {
        return journeyId + ":" + profileId;
    }
}
