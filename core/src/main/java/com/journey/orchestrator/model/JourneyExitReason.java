package com.journey.orchestrator.model;

/**
 * Reasons for journey exit.
 */
public enum JourneyExitReason {
    COMPLETED,              // Journey completed normally
    GOAL_ACHIEVED,          // Goal was achieved before completion
    SUPPRESSED,             // Profile was suppressed
    EXPIRED,                // Journey expired
    MANUAL_EXIT,            // Manually exited
    ERROR,                  // Error occurred
    RE_ENTRY,               // Exited for re-entry
    SEGMENT_CHANGE          // Profile no longer matches segment
}
