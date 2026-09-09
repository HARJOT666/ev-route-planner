package com.example.evrouteplanner.service;

import com.example.evrouteplanner.dto.PlannedStopDto;
import com.example.evrouteplanner.dto.TripPlanResponse;
import com.example.evrouteplanner.dto.TripRequest;
import com.example.evrouteplanner.dto.TripSummaryDto;
import com.example.evrouteplanner.exception.ResourceNotFoundException;
import com.example.evrouteplanner.model.*;
import com.example.evrouteplanner.optimizer.TripOptimizer;
import com.example.evrouteplanner.repository.ChargingStationRepository;
import com.example.evrouteplanner.repository.TripRepository;
import com.example.evrouteplanner.repository.VehicleRepository;
import com.example.evrouteplanner.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans trips and stores trip history.
 *
 * Planning is fully synchronous:
 *   request -> load vehicle + stations -> TripOptimizer -> save -> response.
 */
@Service
public class TripService {

    private final VehicleRepository vehicleRepository;
    private final ChargingStationRepository stationRepository;
    private final TripRepository tripRepository;
    private final CurrentUserService currentUserService;

    public TripService(VehicleRepository vehicleRepository,
                       ChargingStationRepository stationRepository,
                       TripRepository tripRepository,
                       CurrentUserService currentUserService) {
        this.vehicleRepository = vehicleRepository;
        this.stationRepository = stationRepository;
        this.tripRepository = tripRepository;
        this.currentUserService = currentUserService;
    }

    public TripPlanResponse planTrip(TripRequest request) {
        User user = currentUserService.getCurrentUser();

        // Load the vehicle and make sure it belongs to the current user.
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Vehicle not found");
        }

        // Stations come straight from PostgreSQL (the source of truth) so the
        // plan always uses the freshest availability.
        List<ChargingStation> stations = stationRepository.findAll();

        // Run the deterministic optimizer.
        TripOptimizer optimizer = new TripOptimizer(vehicle);
        TripPlanResponse plan = optimizer.plan(request, stations);

        // Save the trip so it appears in history, then attach the new id.
        Trip savedTrip = saveTrip(user, request, vehicle, plan);
        plan.setTripId(savedTrip.getId());

        return plan;
    }

    private Trip saveTrip(User user, TripRequest request, Vehicle vehicle, TripPlanResponse plan) {
        Trip trip = new Trip();
        trip.setUser(user);
        trip.setStartName(request.getStartName());
        trip.setStartLat(request.getStartLat());
        trip.setStartLon(request.getStartLon());
        trip.setDestinationName(request.getDestinationName());
        trip.setDestinationLat(request.getDestinationLat());
        trip.setDestinationLon(request.getDestinationLon());
        trip.setMode(request.getMode());
        trip.setVehicleName(vehicle.getName());
        trip.setFeasible(plan.isFeasible());
        trip.setStartBatteryPercent(plan.getStartBatteryPercent());
        trip.setTotalDistanceKm(plan.getTotalDistanceKm());
        trip.setDrivingTimeMinutes(plan.getDrivingTimeMinutes());
        trip.setChargingTimeMinutes(plan.getChargingTimeMinutes());
        trip.setTotalCost(plan.getTotalCost());

        int order = 1;
        for (PlannedStopDto dto : plan.getStops()) {
            TripStop stop = new TripStop();
            stop.setStopOrder(order);
            stop.setStationId(dto.getStationId());
            stop.setStationName(dto.getStationName());
            stop.setLatitude(dto.getLatitude());
            stop.setLongitude(dto.getLongitude());
            stop.setDistanceFromPreviousKm(dto.getDistanceFromPreviousKm());
            stop.setBatteryArrivalPercent(dto.getBatteryArrivalPercent());
            stop.setBatteryDeparturePercent(dto.getBatteryDeparturePercent());
            stop.setEnergyAddedKwh(dto.getEnergyAddedKwh());
            stop.setChargingTimeMinutes(dto.getChargingTimeMinutes());
            stop.setChargingCost(dto.getChargingCost());
            trip.addStop(stop);
            order++;
        }

        return tripRepository.save(trip);
    }

    // Trip history list for the logged-in user (newest first).
    public List<TripSummaryDto> getMyTrips() {
        User user = currentUserService.getCurrentUser();
        List<TripSummaryDto> result = new ArrayList<>();
        for (Trip trip : tripRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            result.add(new TripSummaryDto(trip));
        }
        return result;
    }

    // Full detail of one saved trip (rebuilt into the same response shape).
    public TripPlanResponse getTripDetail(Long tripId) {
        Trip trip = loadOwnedTrip(tripId);
        return toPlanResponse(trip);
    }

    // Loads a trip and checks it belongs to the current user.
    public Trip loadOwnedTrip(Long tripId) {
        User user = currentUserService.getCurrentUser();
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));
        if (!trip.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Trip not found: " + tripId);
        }
        return trip;
    }

    public void saveAiExplanation(Trip trip, String explanation) {
        trip.setAiExplanation(explanation);
        tripRepository.save(trip);
    }

    // Convert a stored Trip back into the response the frontend understands.
    public TripPlanResponse toPlanResponse(Trip trip) {
        TripPlanResponse response = new TripPlanResponse();
        response.setTripId(trip.getId());
        response.setFeasible(trip.isFeasible());
        response.setDirectReach(trip.getStops().isEmpty() && trip.isFeasible());
        response.setStartName(trip.getStartName());
        response.setStartLat(trip.getStartLat());
        response.setStartLon(trip.getStartLon());
        response.setDestinationName(trip.getDestinationName());
        response.setDestinationLat(trip.getDestinationLat());
        response.setDestinationLon(trip.getDestinationLon());
        response.setMode(trip.getMode());
        response.setVehicleName(trip.getVehicleName());
        response.setStartBatteryPercent(trip.getStartBatteryPercent());
        response.setTotalDistanceKm(trip.getTotalDistanceKm());
        response.setDrivingTimeMinutes(trip.getDrivingTimeMinutes());
        response.setChargingTimeMinutes(trip.getChargingTimeMinutes());
        response.setTotalCost(trip.getTotalCost());
        response.setAiExplanation(trip.getAiExplanation());

        for (TripStop stop : trip.getStops()) {
            response.getStops().add(new PlannedStopDto(stop));
        }
        return response;
    }
}
