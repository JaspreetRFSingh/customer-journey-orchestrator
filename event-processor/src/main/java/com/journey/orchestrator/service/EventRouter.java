package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Event;
import com.journey.orchestrator.model.Journey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Routes events to matching journeys based on trigger conditions.
 * This is a core component that determines which journeys should be
 * entered when an event occurs.
 */
public class EventRouter {
    
    private static final Logger LOG = LoggerFactory.getLogger(EventRouter.class);
    
    private final Map<String, Journey> journeys = new ConcurrentHashMap<>();
    private final Map<String, List<Journey>> eventTypeIndex = new ConcurrentHashMap<>();
    private final JourneyExecutionService executionService;
    private final ExpressionEvaluator expressionEvaluator;
    
    public EventRouter(JourneyExecutionService executionService) {
        this.executionService = executionService;
        this.expressionEvaluator = new ExpressionEvaluator();
    }
    
    /**
     * Registers a journey for event routing.
     */
    public void registerJourney(Journey journey) {
        journeys.put(journey.getJourneyId(), journey);
        
        // Index by event type for fast lookup
        if (journey.getTrigger() != null && 
            journey.getTrigger().getEventType() != null) {
            
            String eventType = journey.getTrigger().getEventType();
            eventTypeIndex.computeIfAbsent(eventType, k -> new ArrayList<>())
                .add(journey);
            
            LOG.info("Registered journey '{}' for event type: {}", 
                journey.getName(), eventType);
        }
    }
    
    /**
     * Unregisters a journey from event routing.
     */
    public void unregisterJourney(String journeyId) {
        Journey journey = journeys.remove(journeyId);
        if (journey != null && journey.getTrigger() != null) {
            String eventType = journey.getTrigger().getEventType();
            if (eventType != null && eventTypeIndex.containsKey(eventType)) {
                eventTypeIndex.get(eventType).remove(journey);
            }
        }
    }
    
    /**
     * Routes an event to all matching journeys.
     * Returns the list of journey executions started.
     */
    public List<String> routeEvent(Event event) {
        List<String> startedJourneys = new ArrayList<>();
        
        // Find journeys triggered by this event type
        List<Journey> candidateJourneys = eventTypeIndex.get(event.getEventType());
        if (candidateJourneys == null || candidateJourneys.isEmpty()) {
            LOG.debug("No journeys registered for event type: {}", event.getEventType());
            return startedJourneys;
        }
        
        // Build context for expression evaluation
        Map<String, Object> context = buildEvaluationContext(event);
        
        // Evaluate conditions and start matching journeys
        for (Journey journey : candidateJourneys) {
            try {
                if (matchesTrigger(journey, event, context)) {
                    executionService.startJourney(journey, event.getProfileId(), event);
                    startedJourneys.add(journey.getJourneyId());
                    LOG.info("Started journey '{}' for profile {}", 
                        journey.getName(), event.getProfileId());
                }
            } catch (Exception e) {
                LOG.error("Error routing event to journey: {}", journey.getJourneyId(), e);
            }
        }
        
        return startedJourneys;
    }
    
    /**
     * Checks if an event matches a journey's trigger conditions.
     */
    private boolean matchesTrigger(Journey journey, Event event, Map<String, Object> context) {
        if (journey.getTrigger() == null) {
            return false;
        }
        
        Journey.JourneyTrigger trigger = journey.getTrigger();
        
        // Check condition if present
        if (trigger.getCondition() != null) {
            boolean matches = expressionEvaluator.evaluateBoolean(trigger.getCondition(), context);
            if (!matches) {
                LOG.debug("Journey '{}' condition not matched for event {}", 
                    journey.getName(), event.getEventId());
                return false;
            }
        }
        
        // Check additional parameters if present
        if (trigger.getParameters() != null && !trigger.getParameters().isEmpty()) {
            for (Map.Entry<String, Object> param : trigger.getParameters().entrySet()) {
                Object eventValue = event.getPayload().get(param.getKey());
                if (!param.getValue().equals(eventValue)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Builds the evaluation context from event and profile data.
     */
    private Map<String, Object> buildEvaluationContext(Event event) {
        return Map.of(
            "event", event,
            "eventType", event.getEventType(),
            "payload", event.getPayload(),
            "profileId", event.getProfileId(),
            "customerId", event.getCustomerId(),
            "timestamp", event.getTimestamp(),
            "source", event.getSource()
        );
    }
    
    /**
     * Returns all registered journeys.
     */
    public List<Journey> getAllJourneys() {
        return new ArrayList<>(journeys.values());
    }
    
    /**
     * Returns journeys for a specific event type.
     */
    public List<Journey> getJourneysForEventType(String eventType) {
        return eventTypeIndex.getOrDefault(eventType, new ArrayList<>());
    }
}
