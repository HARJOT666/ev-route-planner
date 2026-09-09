package com.example.evrouteplanner.service;

import com.example.evrouteplanner.dto.StationResponse;
import com.example.evrouteplanner.event.StationEventProducer;
import com.example.evrouteplanner.event.StationStatusEvent;
import com.example.evrouteplanner.exception.ResourceNotFoundException;
import com.example.evrouteplanner.model.ChargingStation;
import com.example.evrouteplanner.model.StationStatus;
import com.example.evrouteplanner.repository.ChargingStationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Read charging stations and change their status.
 * Reads go through the Redis cache; writes update PostgreSQL and publish a
 * Kafka event so the cache gets invalidated asynchronously.
 */
@Service
public class ChargingStationService {

    private final ChargingStationRepository stationRepository;
    private final StationCache stationCache;
    private final StationEventProducer eventProducer;

    public ChargingStationService(ChargingStationRepository stationRepository,
                                  StationCache stationCache,
                                  StationEventProducer eventProducer) {
        this.stationRepository = stationRepository;
        this.stationCache = stationCache;
        this.eventProducer = eventProducer;
    }

    // Used by the frontend map. Served from Redis (cache-aside).
    public List<StationResponse> getAllStations() {
        return stationCache.getStations();
    }

    /**
     * Change a station's status.
     *  1. Update PostgreSQL (the source of truth).
     *  2. Publish a Kafka event describing the change.
     * The Kafka consumer then invalidates the Redis cache.
     */
    public StationResponse updateStatus(Long stationId, StationStatus newStatus) {
        ChargingStation station = stationRepository.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found: " + stationId));

        StationStatus oldStatus = station.getStatus();
        station.setStatus(newStatus);
        stationRepository.save(station);

        // Announce the change so other parts of the system can react.
        eventProducer.publish(new StationStatusEvent(
                station.getId(), station.getName(), oldStatus, newStatus));

        return new StationResponse(station);
    }
}
