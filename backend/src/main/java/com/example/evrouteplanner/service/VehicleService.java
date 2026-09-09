package com.example.evrouteplanner.service;

import com.example.evrouteplanner.dto.VehicleRequest;
import com.example.evrouteplanner.dto.VehicleResponse;
import com.example.evrouteplanner.exception.ResourceNotFoundException;
import com.example.evrouteplanner.model.User;
import com.example.evrouteplanner.model.Vehicle;
import com.example.evrouteplanner.repository.VehicleRepository;
import com.example.evrouteplanner.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Manage the logged-in user's vehicles. Every method works only on vehicles
 * that belong to the current user.
 */
@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CurrentUserService currentUserService;

    public VehicleService(VehicleRepository vehicleRepository,
                          CurrentUserService currentUserService) {
        this.vehicleRepository = vehicleRepository;
        this.currentUserService = currentUserService;
    }

    public VehicleResponse addVehicle(VehicleRequest request) {
        User user = currentUserService.getCurrentUser();

        Vehicle vehicle = new Vehicle();
        vehicle.setName(request.getName());
        vehicle.setBatteryCapacityKwh(request.getBatteryCapacityKwh());
        vehicle.setEfficiencyKmPerKwh(request.getEfficiencyKmPerKwh());
        vehicle.setMaxChargingPowerKw(request.getMaxChargingPowerKw());
        vehicle.setUser(user);

        vehicleRepository.save(vehicle);
        return new VehicleResponse(vehicle);
    }

    public List<VehicleResponse> getMyVehicles() {
        User user = currentUserService.getCurrentUser();

        List<VehicleResponse> result = new ArrayList<>();
        for (Vehicle vehicle : vehicleRepository.findByUserId(user.getId())) {
            result.add(new VehicleResponse(vehicle));
        }
        return result;
    }

    public void deleteVehicle(Long vehicleId) {
        User user = currentUserService.getCurrentUser();
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));

        // Make sure a user can only delete their own vehicle.
        if (!vehicle.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Vehicle not found: " + vehicleId);
        }
        vehicleRepository.delete(vehicle);
    }
}
