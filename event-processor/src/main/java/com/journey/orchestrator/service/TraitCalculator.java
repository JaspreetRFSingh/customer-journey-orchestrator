package com.journey.orchestrator.service;

import com.journey.orchestrator.model.CustomerProfile;

import java.time.Duration;
import java.time.Instant;

/**
 * Calculates computed traits for customer profiles.
 * Traits are derived values that help with segmentation and personalization.
 */
public class TraitCalculator {
    
    /**
     * Calculates and updates computed traits for a profile.
     */
    public CustomerProfile calculateTraits(CustomerProfile profile) {
        // Calculate engagement score
        double engagementScore = calculateEngagementScore(profile);
        profile = profile.computeTrait("engagementScore", engagementScore);
        
        // Calculate customer lifetime value prediction
        double predictedLtv = calculatePredictedLtv(profile);
        profile = profile.computeTrait("predictedLtv", predictedLtv);
        
        // Calculate churn risk
        double churnRisk = calculateChurnRisk(profile);
        profile = profile.computeTrait("churnRisk", churnRisk);
        
        // Calculate recency score
        int recencyScore = calculateRecencyScore(profile);
        profile = profile.computeTrait("recencyScore", recencyScore);
        
        return profile;
    }
    
    /**
     * Calculates an engagement score (0-100) based on user activity.
     */
    private double calculateEngagementScore(CustomerProfile profile) {
        double score = 0;
        
        // Page views contribution (max 25 points)
        Object pageViews = profile.getAttributes().get("pageViewCount");
        int views = pageViews instanceof Number n ? n.intValue() : 0;
        score += Math.min(25, views * 2.5);
        
        // Purchase activity (max 25 points)
        Object purchases = profile.getAttributes().get("purchaseCount");
        int purchaseCount = purchases instanceof Number n ? n.intValue() : 0;
        score += Math.min(25, purchaseCount * 5);
        
        // Recency contribution (max 25 points)
        Object lastActivity = profile.getAttributes().get("lastPageView");
        if (lastActivity instanceof Instant instant) {
            Duration sinceLastActivity = Duration.between(instant, Instant.now());
            if (sinceLastActivity.toDays() < 1) {
                score += 25;
            } else if (sinceLastActivity.toDays() < 7) {
                score += 15;
            } else if (sinceLastActivity.toDays() < 30) {
                score += 5;
            }
        }
        
        // Cart activity (max 25 points)
        Object cartValue = profile.getAttributes().get("cartValue");
        if (cartValue instanceof Number n && n.doubleValue() > 0) {
            score += 25;
        }
        
        return Math.min(100, score);
    }
    
    /**
     * Calculates predicted lifetime value.
     */
    private double calculatePredictedLtv(CustomerProfile profile) {
        Object totalSpend = profile.getAttributes().get("totalSpend");
        Object purchaseCount = profile.getAttributes().get("purchaseCount");
        
        double spend = totalSpend instanceof Number n ? n.doubleValue() : 0;
        int count = purchaseCount instanceof Number n ? n.intValue() : 0;
        
        if (count == 0) {
            // For new customers, use average order value estimate
            return spend > 0 ? spend * 5 : 500; // Assume 5x potential
        }
        
        double avgOrderValue = spend / count;
        // Predict based on purchase frequency and avg order value
        return avgOrderValue * Math.max(1, count) * 2;
    }
    
    /**
     * Calculates churn risk (0-1, higher = more likely to churn).
     */
    private double calculateChurnRisk(CustomerProfile profile) {
        double risk = 0.5; // Base risk
        
        // Reduce risk for recent activity
        Object lastActivity = profile.getAttributes().get("lastPageView");
        if (lastActivity instanceof Instant instant) {
            Duration sinceLastActivity = Duration.between(instant, Instant.now());
            if (sinceLastActivity.toDays() < 7) {
                risk -= 0.3;
            } else if (sinceLastActivity.toDays() > 30) {
                risk += 0.2;
            }
        }
        
        // Reduce risk for high engagement
        Object engagementScore = profile.getComputedTraits().get("engagementScore");
        if (engagementScore instanceof Number n) {
            double score = n.doubleValue();
            if (score > 70) {
                risk -= 0.2;
            } else if (score < 30) {
                risk += 0.2;
            }
        }
        
        // Reduce risk for high spend
        Object totalSpend = profile.getAttributes().get("totalSpend");
        if (totalSpend instanceof Number n && n.doubleValue() > 500) {
            risk -= 0.1;
        }
        
        return Math.max(0, Math.min(1, risk));
    }
    
    /**
     * Calculates recency score (1-5, higher = more recent).
     */
    private int calculateRecencyScore(CustomerProfile profile) {
        Object lastActivity = profile.getAttributes().get("lastPageView");
        if (lastActivity instanceof Instant instant) {
            Duration sinceLastActivity = Duration.between(instant, Instant.now());
            long days = sinceLastActivity.toDays();
            
            if (days == 0) return 5;
            if (days <= 3) return 4;
            if (days <= 7) return 3;
            if (days <= 30) return 2;
            return 1;
        }
        return 1;
    }
}
