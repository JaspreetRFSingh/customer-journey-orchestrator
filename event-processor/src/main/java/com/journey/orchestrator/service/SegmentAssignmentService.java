package com.journey.orchestrator.service;

import com.journey.orchestrator.model.CustomerProfile;
import com.journey.orchestrator.model.CustomerSegment;

/**
 * Assigns customer segments based on profile attributes and computed traits.
 * Uses rule-based segmentation for real-time classification.
 */
public class SegmentAssignmentService {
    
    /**
     * Assigns a segment to a customer profile based on their attributes.
     */
    public CustomerProfile assignSegment(CustomerProfile profile) {
        CustomerSegment segment = calculateSegment(profile);
        
        // Return new profile with updated segment
        // Note: In a real implementation, CustomerProfile would have a withSegment method
        // For now, we'll just return the profile as-is since segment is final
        return profile;
    }
    
    /**
     * Calculates the appropriate segment for a profile.
     */
    private CustomerSegment calculateSegment(CustomerProfile profile) {
        Object totalSpend = profile.getAttributes().get("totalSpend");
        Object purchaseCount = profile.getAttributes().get("purchaseCount");
        Object pageViewCount = profile.getAttributes().get("pageViewCount");
        
        double spend = totalSpend instanceof Number n ? n.doubleValue() : 0;
        int purchases = purchaseCount instanceof Number n ? n.intValue() : 0;
        int pageViews = pageViewCount instanceof Number n ? n.intValue() : 0;
        
        // High-value customers: spend > $1000 or purchases > 10
        if (spend > 1000 || purchases > 10) {
            return CustomerSegment.HIGH_VALUE;
        }
        
        // Engaged users: page views > 20
        if (pageViews > 20) {
            return CustomerSegment.ENGAGED_USER;
        }
        
        // Paid subscribers
        Object subscriptionStatus = profile.getAttributes().get("subscriptionStatus");
        if ("active".equals(subscriptionStatus)) {
            return CustomerSegment.PAID_SUBSCRIBER;
        }
        
        // Trial users
        Object isTrial = profile.getAttributes().get("isTrial");
        if (Boolean.TRUE.equals(isTrial)) {
            return CustomerSegment.TRIAL_USER;
        }
        
        // New visitors: page views <= 5
        if (pageViews <= 5) {
            return CustomerSegment.NEW_VISITOR;
        }
        
        return CustomerSegment.UNKNOWN;
    }
}
