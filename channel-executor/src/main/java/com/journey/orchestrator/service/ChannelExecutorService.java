package com.journey.orchestrator.service;

import com.journey.orchestrator.model.*;
import com.journey.orchestrator.model.JourneyStep.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Centralized service for executing actions across multiple channels.
 * 
 * Features:
 * - Pluggable channel executors
 * - Async execution with timeout handling
 * - Personalization support
 * - Execution result tracking
 * - Rate limiting and throttling
 * 
 * This simulates journey orchestration platforms's multi-channel execution
 * capabilities including email, SMS, push, in-app, and more.
 */
public class ChannelExecutorService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ChannelExecutorService.class);
    
    private final Map<Channel, ChannelExecutor> channelExecutors;
    private final PersonalizationEngine personalizationEngine;
    private final ExecutorService executor;
    private final Map<Channel, RateLimiter> rateLimiters;
    
    public ChannelExecutorService() {
        this.channelExecutors = new EnumMap<>(Channel.class);
        this.personalizationEngine = new PersonalizationEngine();
        this.executor = Executors.newFixedThreadPool(10);
        this.rateLimiters = new EnumMap<>(Channel.class);
        
        // Register default channel executors
        registerDefaultExecutors();
        initializeRateLimiters();
    }
    
    /**
     * Registers a channel executor.
     */
    public void registerExecutor(Channel channel, ChannelExecutor channelExecutor) {
        channelExecutors.put(channel, channelExecutor);
        LOG.info("Registered executor for channel: {}", channel);
    }
    
    /**
     * Executes an action for a profile.
     */
    public ExecutionResult execute(Action action, String profileId, Map<String, Object> context) {
        String executionId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        try {
            // Check rate limit
            if (action.getChannel() != null) {
                RateLimiter limiter = rateLimiters.get(action.getChannel());
                if (limiter != null && !limiter.tryAcquire()) {
                    LOG.warn("Rate limit exceeded for channel: {}", action.getChannel());
                    return ExecutionResult.failure(
                        executionId, "", profileId, action.getActionId(), 
                        action.getChannel(), "Rate limit exceeded", null);
                }
            }
            
            // Apply personalization
            Action personalizedAction = personalizeAction(action, context);
            
            // Get the appropriate executor
            ChannelExecutor channelExecutor = getExecutor(personalizedAction.getChannel());
            if (channelExecutor == null) {
                LOG.warn("No executor found for channel: {}", personalizedAction.getChannel());
                return ExecutionResult.skipped(
                    executionId, "", profileId, action.getActionId(),
                    "No executor available for channel: " + personalizedAction.getChannel());
            }
            
            // Execute the action
            ChannelExecutionResult channelResult = channelExecutor.execute(
                personalizedAction, profileId, context);
            
            long durationMs = System.currentTimeMillis() - startTime;
            
            if (channelResult.isSuccess()) {
                return ExecutionResult.success(
                    executionId, "", profileId, action.getActionId(),
                    action.getChannel(), channelResult.getResponseData());
            } else {
                return ExecutionResult.failure(
                    executionId, "", profileId, action.getActionId(),
                    action.getChannel(), channelResult.getErrorMessage(),
                    channelResult.getError());
            }
            
        } catch (Exception e) {
            LOG.error("Error executing action: {}", action.getActionId(), e);
            long durationMs = System.currentTimeMillis() - startTime;
            return ExecutionResult.failure(
                executionId, "", profileId, action.getActionId(),
                action.getChannel(), "Execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes an action asynchronously.
     */
    public CompletableFuture<ExecutionResult> executeAsync(Action action, String profileId, 
                                                            Map<String, Object> context) {
        return CompletableFuture.supplyAsync(
            () -> execute(action, profileId, context), 
            executor
        );
    }
    
    /**
     * Gets an executor for a channel.
     */
    private ChannelExecutor getExecutor(Channel channel) {
        if (channel == null) {
            return channelExecutors.get(Channel.API);
        }
        return channelExecutors.get(channel);
    }
    
    /**
     * Applies personalization to an action.
     */
    private Action personalizeAction(Action action, Map<String, Object> context) {
        if (action.getPersonalization() == null) {
            return action;
        }
        
        Map<String, Object> personalizedParams = personalizationEngine.applyPersonalization(
            action.getParameters(), 
            action.getPersonalization(), 
            context
        );
        
        return Action.builder()
            .actionId(action.getActionId())
            .type(action.getType())
            .channel(action.getChannel())
            .parameters(personalizedParams)
            .condition(action.getCondition())
            .personalization(action.getPersonalization())
            .build();
    }
    
    /**
     * Registers default channel executors.
     */
    private void registerDefaultExecutors() {
        registerExecutor(Channel.EMAIL, new EmailChannelExecutor());
        registerExecutor(Channel.SMS, new SmsChannelExecutor());
        registerExecutor(Channel.PUSH_NOTIFICATION, new PushNotificationExecutor());
        registerExecutor(Channel.IN_APP_MESSAGE, new InAppMessageExecutor());
        registerExecutor(Channel.WEB_PUSH, new WebPushExecutor());
        registerExecutor(Channel.WHATSAPP, new WhatsAppExecutor());
        registerExecutor(Channel.WEB_PERSONALIZATION, new WebPersonalizationExecutor());
        registerExecutor(Channel.API, new ApiChannelExecutor());
    }
    
    /**
     * Initializes rate limiters for channels.
     */
    private void initializeRateLimiters() {
        // Email: 100 per second
        rateLimiters.put(Channel.EMAIL, new RateLimiter(100, TimeUnit.SECONDS));
        // SMS: 50 per second
        rateLimiters.put(Channel.SMS, new RateLimiter(50, TimeUnit.SECONDS));
        // Push: 200 per second
        rateLimiters.put(Channel.PUSH_NOTIFICATION, new RateLimiter(200, TimeUnit.SECONDS));
        // In-app: 500 per second
        rateLimiters.put(Channel.IN_APP_MESSAGE, new RateLimiter(500, TimeUnit.SECONDS));
    }
    
    /**
     * Shuts down the service.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Interface for channel-specific executors.
     */
    public interface ChannelExecutor {
        ChannelExecutionResult execute(Action action, String profileId, 
                                        Map<String, Object> context);
        Channel getSupportedChannel();
    }
    
    /**
     * Result of a channel execution.
     */
    public record ChannelExecutionResult(
        boolean success,
        Map<String, Object> responseData,
        String errorMessage,
        Exception error
    ) {
        public static ChannelExecutionResult success(Map<String, Object> data) {
            return new ChannelExecutionResult(true, data, null, null);
        }
        
        public static ChannelExecutionResult failure(String message, Exception error) {
            return new ChannelExecutionResult(false, Map.of(), message, error);
        }
    }
    
    /**
     * Simple rate limiter implementation.
     */
    private static class RateLimiter {
        private final int permitsPerPeriod;
        private final long periodNanos;
        private final ConcurrentLinkedQueue<Long> timestamps;
        
        public RateLimiter(int permitsPerPeriod, TimeUnit periodUnit) {
            this.permitsPerPeriod = permitsPerPeriod;
            this.periodNanos = periodUnit.toNanos(1);
            this.timestamps = new ConcurrentLinkedQueue<>();
        }
        
        public synchronized boolean tryAcquire() {
            long now = System.nanoTime();
            long periodStart = now - periodNanos;
            
            // Remove old timestamps
            while (!timestamps.isEmpty() && timestamps.peek() < periodStart) {
                timestamps.poll();
            }
            
            // Check if we have permits available
            if (timestamps.size() < permitsPerPeriod) {
                timestamps.offer(now);
                return true;
            }
            
            return false;
        }
    }
}
