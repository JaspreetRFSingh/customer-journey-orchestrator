package com.journey.orchestrator.service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects and aggregates processing metrics for observability.
 */
public class MetricsCollector {
    
    private final AtomicLong eventsSubmitted = new AtomicLong(0);
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventsDuplicate = new AtomicLong(0);
    private final AtomicLong eventsError = new AtomicLong(0);
    
    private final Map<String, LongAdder> eventsByType = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> errorsByType = new ConcurrentHashMap<>();
    private final Map<String, DurationStats> processingTimeByType = new ConcurrentHashMap<>();
    
    private volatile long startTime = System.currentTimeMillis();
    
    public void recordEventSubmitted(String eventType) {
        eventsSubmitted.incrementAndGet();
        eventsByType.computeIfAbsent(eventType, k -> new LongAdder()).increment();
    }
    
    public void recordEventProcessed(String eventType, Duration duration) {
        eventsProcessed.incrementAndGet();
        eventsByType.computeIfAbsent(eventType, k -> new LongAdder()).increment();
        
        synchronized (processingTimeByType) {
            processingTimeByType.computeIfAbsent(eventType, k -> new DurationStats())
                .record(duration.toMillis());
        }
    }
    
    public void recordDuplicate() {
        eventsDuplicate.incrementAndGet();
    }
    
    public void recordError(String eventType, Throwable error) {
        eventsError.incrementAndGet();
        errorsByType.computeIfAbsent(eventType, k -> new LongAdder()).increment();
    }
    
    public ProcessingMetrics getMetrics() {
        Map<String, Long> typeCounts = new ConcurrentHashMap<>();
        eventsByType.forEach((k, v) -> typeCounts.put(k, v.sum()));
        
        Map<String, Long> errorCounts = new ConcurrentHashMap<>();
        errorsByType.forEach((k, v) -> errorCounts.put(k, v.sum()));
        
        Map<String, DurationStats> timeStats = new ConcurrentHashMap<>(processingTimeByType);
        
        return new ProcessingMetrics(
            eventsSubmitted.get(),
            eventsProcessed.get(),
            eventsDuplicate.get(),
            eventsError.get(),
            typeCounts,
            errorCounts,
            timeStats,
            Duration.ofMillis(System.currentTimeMillis() - startTime)
        );
    }
    
    public void reset() {
        eventsSubmitted.set(0);
        eventsProcessed.set(0);
        eventsDuplicate.set(0);
        eventsError.set(0);
        eventsByType.clear();
        errorsByType.clear();
        processingTimeByType.clear();
        startTime = System.currentTimeMillis();
    }
    
    /**
     * Duration statistics for tracking processing times.
     */
    public static class DurationStats {
        private long count = 0;
        private long sum = 0;
        private long min = Long.MAX_VALUE;
        private long max = 0;
        
        public synchronized void record(long durationMs) {
            count++;
            sum += durationMs;
            min = Math.min(min, durationMs);
            max = Math.max(max, durationMs);
        }
        
        public double getAverage() {
            return count > 0 ? (double) sum / count : 0;
        }
        
        public long getCount() {
            return count;
        }
        
        public long getMin() {
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        public long getMax() {
            return max;
        }
    }
    
    /**
     * Processing metrics snapshot.
     */
    public record ProcessingMetrics(
        long eventsSubmitted,
        long eventsProcessed,
        long duplicates,
        long errors,
        Map<String, Long> eventsByType,
        Map<String, Long> errorsByType,
        Map<String, DurationStats> processingTimeByType,
        Duration uptime
    ) {
        public double getSuccessRate() {
            if (eventsProcessed + errors == 0) return 0;
            return (double) eventsProcessed / (eventsProcessed + errors) * 100;
        }
        
        public double getThroughput() {
            long uptimeSeconds = uptime.getSeconds();
            if (uptimeSeconds == 0) return 0;
            return (double) eventsProcessed / uptimeSeconds;
        }
    }
}
