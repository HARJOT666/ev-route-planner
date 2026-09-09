package com.example.evrouteplanner.controller;

import com.example.evrouteplanner.dto.TripPlanResponse;
import com.example.evrouteplanner.dto.TripRequest;
import com.example.evrouteplanner.dto.TripSummaryDto;
import com.example.evrouteplanner.service.TripService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Plan trips and read trip history. Requires a valid JWT. */
@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    // Synchronous: the plan is computed and returned immediately.
    @PostMapping("/plan")
    public TripPlanResponse planTrip(@Valid @RequestBody TripRequest request) {
        return tripService.planTrip(request);
    }

    @GetMapping
    public List<TripSummaryDto> getMyTrips() {
        return tripService.getMyTrips();
    }

    @GetMapping("/{id}")
    public TripPlanResponse getTripDetail(@PathVariable Long id) {
        return tripService.getTripDetail(id);
    }
}
