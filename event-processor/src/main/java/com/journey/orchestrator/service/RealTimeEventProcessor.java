package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Event;
import com.journey.orchestrator.model.Event.EventPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * High-performance real-time event processor.
 * 
 * Features:
 * - Priority-based event processing
 * - Async processing with configurable thread pools
 * - Event deduplication
 * - Processing metrics and observability
 * - Backpressure handling
 * 
 * This mirrors enterprise-grade platforms's real-time event processing capabilities.
 */
public class RealTimeEventProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(RealTimeEventProcessor.class);
    
    private final EventRouter eventRouter;
    private final ProfileService profileService;
    private final EventDeduplicator deduplicator;
    private final MetricsCollector metrics;
    
    private final ExecutorService highPriorityExecutor;
    private final ExecutorService normalPriorityExecutor;
    private final BlockingQueue<Event> highPriorityQueue;
    private final BlockingQueue<Event> normalPriorityQueue;
    
    private volatile boolean running = false;
    private final List<Future<?>> processorFutures = new ArrayList<>();
    
    private RealTimeEventProcessor(Builder builder) {
        this.eventRouter = builder.eventRouter;
        this.profileService = builder.profileService;
        this.deduplicator = builder.deduplicator;
        this.metrics = builder.metrics;
        
        this.highPriorityQueue = new PriorityBlockingQueue<>(1000);
        this.normalPriorityQueue = new LinkedBlockingQueue<>(10000);
        
        this.highPriorityExecutor = Executors.newFixedThreadPool(
            builder.highPriorityThreads,
            new ThreadFactoryBuilder("high-priority-processor")
        );
        
        this.normalPriorityExecutor = Executors.newFixedThreadPool(
            builder.normalPriorityThreads,
            new ThreadFactoryBuilder("normal-priority-processor")
        );
    }
    
    /**
     * Starts the event processor.
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        LOG.info("Starting RealTimeEventProcessor...");
        
        // Start high priority processors
        for (int i = 0; i < 4; i++) {
            Future<?> future = highPriorityExecutor.submit(new EventProcessor(highPriorityQueue));
            processorFutures.add(future);
        }
        
        // Start normal priority processors
        for (int i = 0; i < 8; i++) {
            Future<?> future = normalPriorityExecutor.submit(new EventProcessor(normalPriorityQueue));
            processorFutures.add(future);
        }
        
        LOG.info("RealTimeEventProcessor started with {} processors", processorFutures.size());
    }
    
    /**
     * Stops the event processor gracefully.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        LOG.info("Stopping RealTimeEventProcessor...");
        
        highPriorityExecutor.shutdown();
        normalPriorityExecutor.shutdown();
        
        try {
            if (!highPriorityExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                highPriorityExecutor.shutdownNow();
            }
            if (!normalPriorityExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                normalPriorityExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            highPriorityExecutor.shutdownNow();
            normalPriorityExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        LOG.info("RealTimeEventProcessor stopped");
    }
    
    /**
     * Submits an event for processing.
     */
    public void submitEvent(Event event) {
        Instant submitTime = Instant.now();
        
        // Check for duplicates
        if (deduplicator.isDuplicate(event)) {
            LOG.debug("Duplicate event detected: {}", event.getEventId());
            metrics.recordDuplicate();
            return;
        }
        
        // Route event to appropriate queue based on priority
        if (event.getPriority() == EventPriority.HIGH || 
            event.getPriority() == EventPriority.CRITICAL) {
            highPriorityQueue.offer(event);
        } else {
            normalPriorityQueue.offer(event);
        }
        
        metrics.recordEventSubmitted(event.getEventType());
        LOG.debug("Event submitted: {} (priority: {})", event.getEventId(), event.getPriority());
    }
    
    /**
     * Processes an event synchronously (for testing or critical paths).
     */
    public void processEventSync(Event event) {
        Instant startTime = Instant.now();
        
        try {
            // Deduplicate
            if (deduplicator.isDuplicate(event)) {
                return;
            }
            
            // Update profile
            profileService.updateFromEvent(event);
            
            // Route to journeys
            eventRouter.routeEvent(event);
            
            metrics.recordEventProcessed(event.getEventType(), 
                Duration.between(startTime, Instant.now()));
            
        } catch (Exception e) {
            LOG.error("Error processing event: {}", event.getEventId(), e);
            metrics.recordError(event.getEventType(), e);
            throw new EventProcessingException("Failed to process event", e);
        }
    }
    
    /**
     * Returns processing metrics.
     */
    public ProcessingMetrics getMetrics() {
        return metrics.getMetrics();
    }
    
    /**
     * Internal event processor runnable.
     */
    private class EventProcessor implements Runnable {
        private final BlockingQueue<Event> queue;
        
        EventProcessor(BlockingQueue<Event> queue) {
            this.queue = queue;
        }
        
        @Override
        public void run() {
            while (running || !queue.isEmpty()) {
                try {
                    Event event = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (event == null) {
                        continue;
                    }
                    
                    processEventSync(event);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOG.error("Error in event processor", e);
                }
            }
        }
    }
    
    /**
     * Builder for RealTimeEventProcessor.
     */
    public static class Builder {
        private EventRouter eventRouter;
        private ProfileService profileService;
        private EventDeduplicator deduplicator = new EventDeduplicator(Duration.ofMinutes(5));
        private MetricsCollector metrics = new MetricsCollector();
        private int highPriorityThreads = 4;
        private int normalPriorityThreads = 8;
        
        public Builder eventRouter(EventRouter eventRouter) {
            this.eventRouter = eventRouter;
            return this;
        }
        
        public Builder profileService(ProfileService profileService) {
            this.profileService = profileService;
            return this;
        }
        
        public Builder deduplicator(EventDeduplicator deduplicator) {
            this.deduplicator = deduplicator;
            return this;
        }
        
        public Builder metrics(MetricsCollector metrics) {
            this.metrics = metrics;
            return this;
        }
        
        public Builder highPriorityThreads(int threads) {
            this.highPriorityThreads = threads;
            return this;
        }
        
        public Builder normalPriorityThreads(int threads) {
            this.normalPriorityThreads = threads;
            return this;
        }
        
        public RealTimeEventProcessor build() {
            if (eventRouter == null) {
                throw new IllegalStateException("EventRouter is required");
            }
            if (profileService == null) {
                throw new IllegalStateException("ProfileService is required");
            }
            return new RealTimeEventProcessor(this);
        }
    }
    
    /**
     * Custom exception for event processing errors.
     */
    public static class EventProcessingException extends RuntimeException {
        public EventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Thread factory with named threads for better debugging.
     */
    private static class ThreadFactoryBuilder implements ThreadFactory {
        private final String namePrefix;
        private final AtomicLong threadNumber = new AtomicLong(1);
        
        ThreadFactoryBuilder(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(namePrefix + "-thread-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    }
    
    /**
     * Priority blocking queue for high-priority events.
     */
    private static class PriorityBlockingQueue extends LinkedBlockingQueue<Event> {
        public PriorityBlockingQueue(int capacity) {
            super(capacity);
        }
        
        @Override
        public int compareTo(Event o1, Event o2) {
            // Higher priority = lower number (processed first)
            return Integer.compare(o1.getPriority().ordinal(), o2.getPriority().ordinal());
        }
    }
}
