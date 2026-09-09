package com.example.evrouteplanner.controller;

import com.example.evrouteplanner.dto.StationResponse;
import com.example.evrouteplanner.dto.StationStatusUpdateRequest;
import com.example.evrouteplanner.service.ChargingStationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read charging stations and update their status. Requires a valid JWT. */
@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final ChargingStationService stationService;

    public StationController(ChargingStationService stationService) {
        this.stationService = stationService;
    }

    // Served from the Redis cache (cache-aside).
    @GetMapping
    public List<StationResponse> getAllStations() {
        return stationService.getAllStations();
    }

    // Updates PostgreSQL and publishes a Kafka event (which invalidates the cache).
    @PutMapping("/{id}/status")
    public StationResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody StationStatusUpdateRequest request) {
        return stationService.updateStatus(id, request.getStatus());
    }
}
