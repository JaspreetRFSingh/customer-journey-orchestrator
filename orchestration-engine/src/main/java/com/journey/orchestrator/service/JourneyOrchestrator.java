package com.journey.orchestrator.service;

import com.journey.orchestrator.model.*;
import com.journey.orchestrator.model.JourneyExecutionState.ExecutionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Core journey orchestration engine.
 * 
 * Responsibilities:
 * - Execute journey steps in order
 * - Handle decision splits and transitions
 * - Manage wait states and timeouts
 * - Coordinate with channel executors for message delivery
 * - Track execution state and handle failures
 * 
 * This is the heart of the journey orchestration system, similar to
 * journey orchestration platforms's journey execution engine.
 */
public class JourneyOrchestrator {
    
    private static final Logger LOG = LoggerFactory.getLogger(JourneyOrchestrator.class);
    
    private final ChannelExecutorService channelExecutor;
    private final ExpressionEvaluator expressionEvaluator;
    private final ScheduledExecutorService scheduler;
    private final Map<String, Journey> journeyRegistry;
    
    public JourneyOrchestrator(ChannelExecutorService channelExecutor) {
        this.channelExecutor = channelExecutor;
        this.expressionEvaluator = new ExpressionEvaluator();
        this.scheduler = Executors.newScheduledThreadPool(4);
        this.journeyRegistry = new ConcurrentHashMap<>();
    }
    
    /**
     * Registers a journey for execution.
     */
    public void registerJourney(Journey journey) {
        journeyRegistry.put(journey.getJourneyId(), journey);
        LOG.info("Registered journey: {}", journey.getName());
    }
    
    /**
     * Executes a journey for a profile starting from the beginning.
     */
    public void executeJourney(Journey journey, JourneyExecutionState state, Event triggerEvent) {
        try {
            LOG.info("Executing journey '{}' for profile {}", 
                journey.getName(), state.getProfileId());
            
            // Find the start step
            JourneyStep startStep = findStartStep(journey.getSteps());
            if (startStep == null) {
                LOG.error("No start step found for journey: {}", journey.getJourneyId());
                return;
            }
            
            // Begin execution
            executeStep(journey, state, startStep, triggerEvent);
            
        } catch (Exception e) {
            LOG.error("Error executing journey: {}", journey.getJourneyId(), e);
            handleExecutionError(state, e);
        }
    }
    
    /**
     * Resumes a journey from its current state.
     */
    public void resumeJourney(Journey journey, JourneyExecutionState state) {
        if (state.getCurrentStepId() == null) {
            LOG.warn("Cannot resume journey without current step");
            return;
        }
        
        JourneyStep currentStep = findStepById(journey.getSteps(), state.getCurrentStepId());
        if (currentStep == null) {
            LOG.error("Current step not found: {}", state.getCurrentStepId());
            return;
        }
        
        executeStep(journey, state, currentStep, null);
    }
    
    /**
     * Executes a single step in the journey.
     */
    private void executeStep(Journey journey, JourneyExecutionState state, 
                             JourneyStep step, Event triggerEvent) {
        LOG.debug("Executing step '{}' ({}) for journey {}", 
            step.getName(), step.getStepId(), journey.getJourneyId());
        
        // Update state to current step
        state = state.advanceToStep(step.getStepId());
        
        try {
            // Check entry condition
            if (step.getEntryCondition() != null) {
                Map<String, Object> context = buildContext(state, triggerEvent);
                if (!expressionEvaluator.evaluateBoolean(step.getEntryCondition(), context)) {
                    LOG.debug("Step {} entry condition not met, skipping", step.getStepId());
                    handleStepSkipped(journey, state, step, "Entry condition not met");
                    return;
                }
            }
            
            // Execute based on step type
            switch (step.getType()) {
                case START -> executeStartStep(journey, state, step, triggerEvent);
                case ACTION -> executeActionStep(journey, state, step, triggerEvent);
                case DECISION -> executeDecisionStep(journey, state, step, triggerEvent);
                case WAIT -> executeWaitStep(journey, state, step, triggerEvent);
                case SPLIT -> executeSplitStep(journey, state, step, triggerEvent);
                case JOIN -> executeJoinStep(journey, state, step, triggerEvent);
                case GOAL -> executeGoalStep(journey, state, step, triggerEvent);
                case END -> executeEndStep(journey, state, step, triggerEvent);
            }
            
        } catch (Exception e) {
            LOG.error("Error executing step: {}", step.getStepId(), e);
            handleStepError(journey, state, step, e);
        }
    }
    
    /**
     * Executes a START step.
     */
    private void executeStartStep(Journey journey, JourneyExecutionState state, 
                                   JourneyStep step, Event triggerEvent) {
        LOG.debug("Start step executed for journey {}", journey.getJourneyId());
        
        // Move to next step
        JourneyStep nextStep = getNextStep(journey.getSteps(), step, state, triggerEvent);
        if (nextStep != null) {
            executeStep(journey, state, nextStep, triggerEvent);
        }
    }
    
    /**
     * Executes an ACTION step - sends messages, updates profiles, etc.
     */
    private void executeActionStep(Journey journey, JourneyExecutionState state, 
                                    JourneyStep step, Event triggerEvent) {
        List<JourneyStep.Action> actions = step.getActions();
        if (actions == null || actions.isEmpty()) {
            moveToNextStep(journey, state, step, triggerEvent);
            return;
        }
        
        Map<String, Object> context = buildContext(state, triggerEvent);
        
        for (JourneyStep.Action action : actions) {
            // Check action condition
            if (action.getCondition() != null) {
                if (!expressionEvaluator.evaluateBoolean(action.getCondition(), context)) {
                    LOG.debug("Action condition not met, skipping: {}", action.getActionId());
                    continue;
                }
            }
            
            // Execute the action
            ExecutionResult result = channelExecutor.execute(action, state.getProfileId(), context);
            state = state.addExecutionResult(result);
            
            LOG.info("Action {} executed with status: {}", action.getActionId(), result.getStatus());
        }
        
        moveToNextStep(journey, state, step, triggerEvent);
    }
    
    /**
     * Executes a DECISION step - branches based on conditions.
     */
    private void executeDecisionStep(Journey journey, JourneyExecutionState state, 
                                      JourneyStep step, Event triggerEvent) {
        Map<String, Object> context = buildContext(state, triggerEvent);
        
        // Evaluate transitions to find the matching branch
        JourneyStep nextStep = evaluateTransitions(journey.getSteps(), step, context);
        
        if (nextStep != null) {
            LOG.debug("Decision step {} resolved to step {}", step.getStepId(), nextStep.getStepId());
            executeStep(journey, state, nextStep, triggerEvent);
        } else {
            LOG.debug("No matching transition found for decision step {}", step.getStepId());
            moveToNextStep(journey, state, step, triggerEvent);
        }
    }
    
    /**
     * Executes a WAIT step - pauses execution for a duration or event.
     */
    private void executeWaitStep(Journey journey, JourneyExecutionState state, 
                                  JourneyStep step, Event triggerEvent) {
        Duration waitDuration = step.getWaitDuration();
        
        if (waitDuration == null || waitDuration.isZero()) {
            // No wait, move to next step
            moveToNextStep(journey, state, step, triggerEvent);
            return;
        }
        
        // Set state to waiting and schedule resume
        state = state.waiting();
        LOG.info("Journey {} waiting for {} for profile {}", 
            journey.getJourneyId(), waitDuration, state.getProfileId());
        
        scheduler.schedule(() -> {
            LOG.debug("Wait complete for journey {}", journey.getJourneyId());
            moveToNextStep(journey, state, step, triggerEvent);
        }, waitDuration.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * Executes a SPLIT step - A/B test or multi-way split.
     */
    private void executeSplitStep(Journey journey, JourneyExecutionState state, 
                                   JourneyStep step, Event triggerEvent) {
        Map<String, Object> context = buildContext(state, triggerEvent);
        
        // Use transition evaluation for split logic
        JourneyStep nextStep = evaluateTransitions(journey.getSteps(), step, context);
        
        if (nextStep != null) {
            executeStep(journey, state, nextStep, triggerEvent);
        } else {
            // Default to first transition if no condition matches
            if (step.getTransitions() != null && !step.getTransitions().isEmpty()) {
                String targetId = step.getTransitions().get(0).getTargetStepId();
                JourneyStep targetStep = findStepById(journey.getSteps(), targetId);
                if (targetStep != null) {
                    executeStep(journey, state, targetStep, triggerEvent);
                }
            }
        }
    }
    
    /**
     * Executes a JOIN step - merges branches.
     */
    private void executeJoinStep(Journey journey, JourneyExecutionState state, 
                                  JourneyStep step, Event triggerEvent) {
        moveToNextStep(journey, state, step, triggerEvent);
    }
    
    /**
     * Executes a GOAL step - checks if goal is achieved.
     */
    private void executeGoalStep(Journey journey, JourneyExecutionState state, 
                                  JourneyStep step, Event triggerEvent) {
        Map<String, Object> context = buildContext(state, triggerEvent);
        
        if (journey.getConfig() != null && journey.getConfig().getGoalCondition() != null) {
            boolean goalAchieved = expressionEvaluator.evaluateBoolean(
                journey.getConfig().getGoalCondition(), context);
            
            if (goalAchieved) {
                LOG.info("Goal achieved for journey {} profile {}", 
                    journey.getJourneyId(), state.getProfileId());
                state = state.completed();
                return;
            }
        }
        
        moveToNextStep(journey, state, step, triggerEvent);
    }
    
    /**
     * Executes an END step - completes the journey.
     */
    private void executeEndStep(Journey journey, JourneyExecutionState state, 
                                 JourneyStep step, Event triggerEvent) {
        LOG.info("Journey {} completed for profile {}", 
            journey.getJourneyId(), state.getProfileId());
        state = state.completed();
    }
    
    /**
     * Finds the next step based on transitions.
     */
    private JourneyStep getNextStep(List<JourneyStep> steps, JourneyStep currentStep, 
                                     JourneyExecutionState state, Event triggerEvent) {
        if (currentStep.getTransitions() == null || currentStep.getTransitions().isEmpty()) {
            return null;
        }
        
        Map<String, Object> context = buildContext(state, triggerEvent);
        return evaluateTransitions(steps, currentStep, context);
    }
    
    /**
     * Evaluates transitions to find the matching next step.
     */
    private JourneyStep evaluateTransitions(List<JourneyStep> steps, JourneyStep currentStep, 
                                             Map<String, Object> context) {
        if (currentStep.getTransitions() == null) {
            return null;
        }
        
        // Sort by priority and evaluate
        return currentStep.getTransitions().stream()
            .sorted((t1, t2) -> Integer.compare(t1.getPriority(), t2.getPriority()))
            .filter(t -> t.getCondition() == null || 
                        expressionEvaluator.evaluateBoolean(t.getCondition(), context))
            .findFirst()
            .flatMap(t -> findStepById(steps, t.getTargetStepId()))
            .orElse(null);
    }
    
    /**
     * Moves to the next step in the journey.
     */
    private void moveToNextStep(Journey journey, JourneyExecutionState state, 
                                 JourneyStep step, Event triggerEvent) {
        JourneyStep nextStep = getNextStep(journey.getSteps(), step, state, triggerEvent);
        if (nextStep != null) {
            executeStep(journey, state, nextStep, triggerEvent);
        } else if (step.getType() != JourneyStep.JourneyStepType.END) {
            // No next step and not an END step - complete the journey
            state = state.completed();
            LOG.info("Journey {} completed (no more steps) for profile {}", 
                journey.getJourneyId(), state.getProfileId());
        }
    }
    
    /**
     * Builds the evaluation context for expressions.
     */
    private Map<String, Object> buildContext(JourneyExecutionState state, Event triggerEvent) {
        return Map.of(
            "profileId", state.getProfileId(),
            "journeyId", state.getJourneyId(),
            "variables", state.getVariables(),
            "event", triggerEvent != null ? triggerEvent : Map.of(),
            "timestamp", Instant.now()
        );
    }
    
    /**
     * Finds the start step in a journey.
     */
    private JourneyStep findStartStep(List<JourneyStep> steps) {
        return steps.stream()
            .filter(s -> s.getType() == JourneyStep.JourneyStepType.START)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Finds a step by ID.
     */
    private JourneyStep findStepById(List<JourneyStep> steps, String stepId) {
        return steps.stream()
            .filter(s -> s.getStepId().equals(stepId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Handles step skip.
     */
    private void handleStepSkipped(Journey journey, JourneyExecutionState state, 
                                    JourneyStep step, String reason) {
        ExecutionResult result = ExecutionResult.skipped(
            java.util.UUID.randomUUID().toString(),
            journey.getJourneyId(),
            state.getProfileId(),
            step.getStepId(),
            reason
        );
        state = state.addExecutionResult(result);
        moveToNextStep(journey, state, step, null);
    }
    
    /**
     * Handles step error.
     */
    private void handleStepError(Journey journey, JourneyExecutionState state, 
                                  JourneyStep step, Exception error) {
        LOG.error("Step error in journey {}", journey.getJourneyId(), error);
        // Continue to next step or handle based on error policy
        moveToNextStep(journey, state, step, null);
    }
    
    /**
     * Handles execution error.
     */
    private void handleExecutionError(JourneyExecutionState state, Exception error) {
        // Mark state as failed
        LOG.error("Journey execution failed for profile {}", state.getProfileId(), error);
    }
    
    /**
     * Shuts down the orchestrator.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
