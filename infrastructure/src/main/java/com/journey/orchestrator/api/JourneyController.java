package com.journey.orchestrator.api;

import com.journey.orchestrator.model.*;
import com.journey.orchestrator.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API for journey management.
 */
@RestController
@RequestMapping("/api/v1/journeys")
public class JourneyController {

    private final JourneyOrchestrator orchestrator;
    private final EventRouter eventRouter;

    public JourneyController(JourneyOrchestrator orchestrator, EventRouter eventRouter) {
        this.orchestrator = orchestrator;
        this.eventRouter = eventRouter;
    }

    @PostMapping
    public ResponseEntity<Journey> createJourney(@RequestBody Journey journey) {
        orchestrator.registerJourney(journey);
        eventRouter.registerJourney(journey);
        return ResponseEntity.ok(journey);
    }

    @GetMapping
    public ResponseEntity<List<Journey>> listJourneys() {
        return ResponseEntity.ok(eventRouter.getAllJourneys());
    }

    @GetMapping("/{journeyId}")
    public ResponseEntity<Journey> getJourney(@PathVariable String journeyId) {
        return eventRouter.getAllJourneys().stream()
            .filter(j -> j.getJourneyId().equals(journeyId))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{journeyId}/activate")
    public ResponseEntity<Void> activateJourney(@PathVariable String journeyId) {
        // In production, update journey status to ACTIVE
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{journeyId}")
    public ResponseEntity<Void> deleteJourney(@PathVariable String journeyId) {
        eventRouter.unregisterJourney(journeyId);
        return ResponseEntity.ok().build();
    }
}
