package com.journey.orchestrator.api;

import com.journey.orchestrator.model.Event;
import com.journey.orchestrator.service.RealTimeEventProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for event ingestion.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final RealTimeEventProcessor eventProcessor;

    public EventController(RealTimeEventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @PostMapping
    public ResponseEntity<Event> ingestEvent(@RequestBody Event event) {
        eventProcessor.submitEvent(event);
        return ResponseEntity.accepted().body(event);
    }

    @PostMapping("/batch")
    public ResponseEntity<List<Event>> ingestBatch(@RequestBody List<Event> events) {
        events.forEach(eventProcessor::submitEvent);
        return ResponseEntity.accepted().body(events);
    }

    @PostMapping("/{eventId}/process")
    public ResponseEntity<Void> processEventSync(@PathVariable String eventId) {
        // In production, fetch event and process synchronously
        return ResponseEntity.ok().build();
    }
}
