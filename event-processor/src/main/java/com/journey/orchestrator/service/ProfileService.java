package com.journey.orchestrator.service;

import com.journey.orchestrator.model.CustomerProfile;
import com.journey.orchestrator.model.CustomerProfile.Identity;
import com.journey.orchestrator.model.CustomerSegment;
import com.journey.orchestrator.model.Event;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing customer profiles with real-time updates.
 * 
 * Features:
 * - In-memory caching with Caffeine for low-latency access
 * - Real-time profile updates from events
 * - Computed traits calculation
 * - Segment assignment based on rules
 * 
 * This mirrors enterprise-grade platforms's Real-Time Customer Profile.
 */
public class ProfileService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProfileService.class);
    
    private final Cache<String, CustomerProfile> profileCache;
    private final Map<String, CustomerProfile> profileStore;
    private final SegmentAssignmentService segmentService;
    private final TraitCalculator traitCalculator;
    
    public ProfileService() {
        this.profileCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterWrite(Duration.ofHours(24))
            .recordStats()
            .build();
        
        this.profileStore = new ConcurrentHashMap<>();
        this.segmentService = new SegmentAssignmentService();
        this.traitCalculator = new TraitCalculator();
    }
    
    /**
     * Gets a profile by ID.
     */
    public Optional<CustomerProfile> getProfile(String profileId) {
        CustomerProfile profile = profileCache.getIfPresent(profileId);
        if (profile != null) {
            return Optional.of(profile);
        }
        
        // Fall back to store
        profile = profileStore.get(profileId);
        if (profile != null) {
            profileCache.put(profileId, profile);
            return Optional.of(profile);
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets a profile by customer ID.
     */
    public Optional<CustomerProfile> getProfileByCustomerId(String customerId) {
        return profileStore.values().stream()
            .filter(p -> customerId.equals(p.getCustomerId()))
            .findFirst()
            .map(p -> {
                profileCache.put(p.getProfileId(), p);
                return p;
            });
    }
    
    /**
     * Creates a new customer profile.
     */
    public CustomerProfile createProfile(String customerId, Identity identity, 
                                          Map<String, Object> initialAttributes) {
        CustomerProfile profile = CustomerProfile.create(customerId, identity);
        
        if (initialAttributes != null && !initialAttributes.isEmpty()) {
            for (Map.Entry<String, Object> entry : initialAttributes.entrySet()) {
                profile = profile.updateAttribute(entry.getKey(), entry.getValue());
            }
        }
        
        // Calculate initial traits and segment
        profile = traitCalculator.calculateTraits(profile);
        profile = segmentService.assignSegment(profile);
        
        profileStore.put(profile.getProfileId(), profile);
        profileCache.put(profile.getProfileId(), profile);
        
        LOG.info("Created profile {} for customer {}", profile.getProfileId(), customerId);
        return profile;
    }
    
    /**
     * Updates a profile from an event.
     * This is the core real-time profile update mechanism.
     */
    public CustomerProfile updateFromEvent(Event event) {
        Optional<CustomerProfile> existingOpt = getProfile(event.getProfileId());
        
        CustomerProfile profile;
        if (existingOpt.isPresent()) {
            profile = existingOpt.get();
        } else {
            // Create profile if it doesn't exist
            Identity identity = Identity.builder()
                .email((String) event.getPayload().get("email"))
                .build();
            profile = CustomerProfile.create(event.getCustomerId(), identity);
        }
        
        // Update attributes from event payload
        profile = updateProfileFromEvent(profile, event);
        
        // Calculate traits
        profile = traitCalculator.calculateTraits(profile);
        
        // Assign segment
        profile = segmentService.assignSegment(profile);
        
        // Store updated profile
        profileStore.put(profile.getProfileId(), profile);
        profileCache.put(profile.getProfileId(), profile);
        
        LOG.debug("Updated profile {} from event {}", profile.getProfileId(), event.getEventId());
        return profile;
    }
    
    /**
     * Updates a profile attribute.
     */
    public CustomerProfile updateAttribute(String profileId, String key, Object value) {
        Optional<CustomerProfile> existingOpt = getProfile(profileId);
        if (existingOpt.isEmpty()) {
            throw new IllegalArgumentException("Profile not found: " + profileId);
        }
        
        CustomerProfile profile = existingOpt.get().updateAttribute(key, value);
        profile = traitCalculator.calculateTraits(profile);
        profile = segmentService.assignSegment(profile);
        
        profileStore.put(profileId, profile);
        profileCache.put(profileId, profile);
        
        return profile;
    }
    
    /**
     * Updates profile from event data.
     */
    private CustomerProfile updateProfileFromEvent(CustomerProfile profile, Event event) {
        Map<String, Object> payload = event.getPayload();
        
        // Update attributes based on event type
        switch (event.getEventType()) {
            case "profileCreated" -> {
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    profile = profile.updateAttribute(entry.getKey(), entry.getValue());
                }
            }
            case "purchase" -> {
                // Update purchase-related attributes
                Object amount = payload.get("amount");
                if (amount instanceof Number) {
                    // Update total spend
                    Object currentTotal = profile.getAttributes().get("totalSpend");
                    double total = currentTotal instanceof Number n ? n.doubleValue() : 0;
                    total += ((Number) amount).doubleValue();
                    profile = profile.updateAttribute("totalSpend", total);
                    
                    // Update purchase count
                    Object currentCount = profile.getAttributes().get("purchaseCount");
                    int count = currentCount instanceof Number n ? n.intValue() : 0;
                    profile = profile.updateAttribute("purchaseCount", count + 1);
                    
                    // Update last purchase date
                    profile = profile.updateAttribute("lastPurchaseDate", Instant.now());
                }
            }
            case "cartAdd", "cartRemove" -> {
                // Update cart value
                Object cartValue = payload.get("cartValue");
                if (cartValue != null) {
                    profile = profile.updateAttribute("cartValue", cartValue);
                }
            }
            case "pageView" -> {
                // Update page view count
                Object currentViews = profile.getAttributes().get("pageViewCount");
                int views = currentViews instanceof Number n ? n.intValue() : 0;
                profile = profile.updateAttribute("pageViewCount", views + 1);
                profile = profile.updateAttribute("lastPageView", Instant.now());
            }
            default -> {
                // Generic attribute update
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    profile = profile.updateAttribute("event_" + entry.getKey(), entry.getValue());
                }
            }
        }
        
        return profile;
    }
    
    /**
     * Returns cache statistics.
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            profileCache.estimatedSize(),
            profileCache.stats().hitCount(),
            profileCache.stats().missCount(),
            profileCache.stats().hitRate()
        );
    }
    
    /**
     * Cache statistics record.
     */
    public record CacheStats(long size, long hits, long misses, double hitRate) {}
}
