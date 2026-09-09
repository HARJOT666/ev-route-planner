package com.example.evrouteplanner.event;

import com.example.evrouteplanner.service.StationCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens to the "charging-station-events" topic. When a station status
 * changes, the cached station list is now stale, so we clear it. The next
 * station lookup will rebuild the cache from PostgreSQL (cache-aside).
 *
 * Flow: producer -> topic -> this consumer -> invalidate Redis cache.
 */
@Component
public class StationEventConsumer {

    private final StationCache stationCache;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StationEventConsumer(StationCache stationCache) {
        this.stationCache = stationCache;
    }

    @KafkaListener(topics = "charging-station-events", groupId = "ev-route-planner")
    public void onStationEvent(String message) {
        try {
            StationStatusEvent event = objectMapper.readValue(message, StationStatusEvent.class);
            System.out.println("[Kafka] Received station event for '" + event.getStationName()
                    + "': " + event.getOldStatus() + " -> " + event.getNewStatus());

            // The station list changed, so the cached copy is no longer correct.
            stationCache.invalidate();
            System.out.println("[Kafka] Station cache invalidated after status change.");
        } catch (Exception e) {
            System.out.println("[Kafka] Failed to process station event: " + e.getMessage());
        }
    }
}
