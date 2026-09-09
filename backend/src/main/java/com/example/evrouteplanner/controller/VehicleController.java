package com.example.evrouteplanner.controller;

import com.example.evrouteplanner.dto.VehicleRequest;
import com.example.evrouteplanner.dto.VehicleResponse;
import com.example.evrouteplanner.service.VehicleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Manage the logged-in user's vehicles. Requires a valid JWT. */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public VehicleResponse addVehicle(@Valid @RequestBody VehicleRequest request) {
        return vehicleService.addVehicle(request);
    }

    @GetMapping
    public List<VehicleResponse> getMyVehicles() {
        return vehicleService.getMyVehicles();
    }

    @DeleteMapping("/{id}")
    public void deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
    }
}
