package com.journey.orchestrator.service;

import com.journey.orchestrator.model.Event;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

/**
 * Deduplicates events within a time window to prevent duplicate processing.
 * Uses a time-based cache for efficient deduplication.
 */
public class EventDeduplicator {
    
    private final Cache<String, Boolean> seenEvents;
    
    public EventDeduplicator(Duration timeWindow) {
        this.seenEvents = Caffeine.newBuilder()
            .maximumSize(1_000_000)
            .expireAfterWrite(timeWindow)
            .build();
    }
    
    /**
     * Checks if an event is a duplicate.
     * Returns true if the event was already seen, false otherwise.
     * Also marks the event as seen.
     */
    public boolean isDuplicate(Event event) {
        String key = event.getEventId();
        Boolean alreadySeen = seenEvents.getIfPresent(key);
        
        if (alreadySeen != null) {
            return true;
        }
        
        // Mark as seen
        seenEvents.put(key, true);
        return false;
    }
    
    /**
     * Checks if an event is a duplicate without marking it.
     */
    public boolean checkDuplicate(Event event) {
        return seenEvents.getIfPresent(event.getEventId()) != null;
    }
    
    /**
     * Returns the number of events currently tracked.
     */
    public long getTrackedEventCount() {
        return seenEvents.estimatedSize();
    }
}
