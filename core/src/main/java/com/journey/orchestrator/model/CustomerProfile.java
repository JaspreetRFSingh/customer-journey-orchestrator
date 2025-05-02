package com.journey.orchestrator.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a unified customer profile with real-time attributes.
 * Similar to enterprise-grade platforms's Real-Time Customer Profile.
 */
@Data
@Builder
@EqualsAndHashCode(of = "profileId")
public class CustomerProfile {
    
    private final String profileId;
    private final String customerId;
    private final Identity identity;
    private final Map<String, Object> attributes;
    private final Map<String, String> preferences;
    private final CustomerSegment segment;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Map<String, Object> computedTraits;
    
    @Data
    @Builder
    public static class Identity {
        private final String email;
        private final String phone;
        private final String crmId;
        private final String deviceId;
        private final Map<String, String> additionalIds;
    }
    
    /**
     * Updates a specific attribute and returns a new immutable instance.
     * This supports real-time profile updates from streaming events.
     */
    public CustomerProfile updateAttribute(String key, Object value) {
        Map<String, Object> newAttributes = new HashMap<>(this.attributes);
        newAttributes.put(key, value);
        return CustomerProfile.builder()
            .profileId(this.profileId)
            .customerId(this.customerId)
            .identity(this.identity)
            .attributes(newAttributes)
            .preferences(this.preferences)
            .segment(this.segment)
            .createdAt(this.createdAt)
            .updatedAt(Instant.now())
            .computedTraits(this.computedTraits)
            .build();
    }
    
    /**
     * Computes and updates traits based on current attributes.
     * Used for real-time personalization scoring.
     */
    public CustomerProfile computeTrait(String traitName, Object value) {
        Map<String, Object> newTraits = new HashMap<>(this.computedTraits);
        newTraits.put(traitName, value);
        return CustomerProfile.builder()
            .profileId(this.profileId)
            .customerId(this.customerId)
            .identity(this.identity)
            .attributes(this.attributes)
            .preferences(this.preferences)
            .segment(this.segment)
            .createdAt(this.createdAt)
            .updatedAt(Instant.now())
            .computedTraits(newTraits)
            .build();
    }
    
    /**
     * Factory method to create a new profile with default values.
     */
    public static CustomerProfile create(String customerId, Identity identity) {
        return CustomerProfile.builder()
            .profileId(UUID.randomUUID().toString())
            .customerId(customerId)
            .identity(identity)
            .attributes(new HashMap<>())
            .preferences(new HashMap<>())
            .segment(CustomerSegment.UNKNOWN)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .computedTraits(new HashMap<>())
            .build();
    }
}
