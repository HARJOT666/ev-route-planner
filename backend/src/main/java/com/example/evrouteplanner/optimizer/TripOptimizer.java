package com.example.evrouteplanner.optimizer;

import com.example.evrouteplanner.dto.PlannedStopDto;
import com.example.evrouteplanner.dto.RejectedStationDto;
import com.example.evrouteplanner.dto.TripPlanResponse;
import com.example.evrouteplanner.dto.TripRequest;
import com.example.evrouteplanner.model.ChargingStation;
import com.example.evrouteplanner.model.OptimizationMode;
import com.example.evrouteplanner.model.StationStatus;
import com.example.evrouteplanner.model.Vehicle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The heart of the project: a deterministic (no machine learning) EV trip
 * planner. Given a vehicle, its current battery and a list of charging
 * stations, it decides whether the destination is reachable and, if charging is
 * needed, which stations to stop at.
 *
 * The approach is a simple, explainable GREEDY algorithm:
 *   1. If we can reach the destination directly, do that.
 *   2. Otherwise, repeatedly pick the "best" reachable station (the definition
 *      of "best" depends on the optimization mode), charge to 80%, and continue
 *      until the destination is finally within range.
 *
 * At every step we only consider stations that are:
 *   - AVAILABLE,
 *   - reachable with the current battery, and
 *   - closer to the destination than we are now (so we always make progress).
 *
 * Choosing the winner happens in two simple steps:
 *   Step A - drive as far as we safely can. We look at how much progress the
 *            best candidate makes and keep only stations that make GOOD progress
 *            (at least 60% of that best). This avoids pointless tiny hops and
 *            naturally keeps the number of charging stops low.
 *   Step B - among those "good progress" stations, the mode decides:
 *            - FASTEST  : the station with the SHORTEST charging time (fast charger).
 *            - CHEAPEST : the station with the LOWEST charging cost (cheap electricity).
 *            - BALANCED : a 50/50 blend of charging time and cost.
 *
 * Everything is plain loops and if-statements so the flow can be read top to
 * bottom and explained in an interview.
 *
 * A new TripOptimizer is created per trip (it holds the vehicle being planned),
 * so it is a plain class, not a Spring bean.
 */
public class TripOptimizer {

    // --- Tunable assumptions (kept as named constants for clarity) ---
    private static final double AVG_SPEED_KMH = 60.0;     // assumed average driving speed
    private static final double RESERVE_PERCENT = 10.0;   // never plan to drop below this battery %
    private static final double CHARGE_TO_PERCENT = 80.0; // charging stops fill the battery to 80%
    private static final int MAX_STOPS = 8;               // safety guard against endless loops

    private final Vehicle vehicle;

    public TripOptimizer(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    /** A candidate charging station considered during one step of the greedy loop. */
    private static class Candidate {
        ChargingStation station;
        double distanceToStation;   // km from the current position
        double stationToDest;       // km from the station to the destination
        double progress;            // how much closer to the destination this stop gets us (km)
        double chargeTimeMinutes;   // time to charge back up to 80% here
        double chargeCost;          // cost to charge back up to 80% here

        Candidate(ChargingStation station, double distanceToStation, double stationToDest) {
            this.station = station;
            this.distanceToStation = distanceToStation;
            this.stationToDest = stationToDest;
        }
    }

    // ---------- small physics helpers ----------

    // How far (km) the car can drive using battery down to the reserve level.
    private double reachableKm(double batteryPercent) {
        double usablePercent = batteryPercent - RESERVE_PERCENT;
        if (usablePercent <= 0) {
            return 0;
        }
        return vehicle.getBatteryCapacityKwh() * (usablePercent / 100.0) * vehicle.getEfficiencyKmPerKwh();
    }

    // Battery percentage consumed to drive a given distance.
    private double percentForDistance(double km) {
        double energyKwh = km / vehicle.getEfficiencyKmPerKwh();
        return energyKwh / vehicle.getBatteryCapacityKwh() * 100.0;
    }

    private double drivingMinutes(double km) {
        return km / AVG_SPEED_KMH * 60.0;
    }

    // ---------- main entry point ----------

    public TripPlanResponse plan(TripRequest request, List<ChargingStation> allStations) {
        TripPlanResponse plan = new TripPlanResponse();
        plan.setStartName(request.getStartName());
        plan.setStartLat(request.getStartLat());
        plan.setStartLon(request.getStartLon());
        plan.setDestinationName(request.getDestinationName());
        plan.setDestinationLat(request.getDestinationLat());
        plan.setDestinationLon(request.getDestinationLon());
        plan.setMode(request.getMode());
        plan.setVehicleName(vehicle.getName());
        plan.setStartBatteryPercent(request.getBatteryPercent());

        double totalDistanceToDest = GeoUtil.distanceKm(
                request.getStartLat(), request.getStartLon(),
                request.getDestinationLat(), request.getDestinationLon());

        // ----- Case 1: can we reach the destination directly? -----
        if (reachableKm(request.getBatteryPercent()) >= totalDistanceToDest) {
            fillDirectTrip(plan, request, totalDistanceToDest);
            return plan;
        }

        // ----- Case 2: charging is required. Run the greedy loop. -----
        planWithCharging(plan, request, allStations);
        return plan;
    }

    // Simple case: no charging stop needed.
    private void fillDirectTrip(TripPlanResponse plan, TripRequest request, double distance) {
        double arrivalPercent = request.getBatteryPercent() - percentForDistance(distance);

        plan.setFeasible(true);
        plan.setDirectReach(true);
        plan.setTotalDistanceKm(round(distance));
        plan.setDrivingTimeMinutes(round(drivingMinutes(distance)));
        plan.setChargingTimeMinutes(0);
        plan.setTotalCost(0);
        plan.setArrivalBatteryPercent(round(arrivalPercent));
        plan.setMessage("Destination is reachable directly without charging. "
                + "Estimated battery on arrival: " + round(arrivalPercent) + "%.");
    }

    // Harder case: greedily add charging stops until the destination is reachable.
    private void planWithCharging(TripPlanResponse plan, TripRequest request,
                                  List<ChargingStation> allStations) {

        // Current position starts at the trip start.
        double currentLat = request.getStartLat();
        double currentLon = request.getStartLon();
        double currentPercent = request.getBatteryPercent();

        double totalDistance = 0;
        double drivingTime = 0;
        double chargingTime = 0;
        double totalCost = 0;

        List<Long> visited = new ArrayList<>();
        // Reason each station was not chosen; a station later picked is removed from here.
        Map<Long, String> rejected = new LinkedHashMap<>();

        for (int stopCount = 0; stopCount < MAX_STOPS; stopCount++) {

            double remainingToDest = GeoUtil.distanceKm(
                    currentLat, currentLon, request.getDestinationLat(), request.getDestinationLon());

            // Can we finish from here? Then stop adding charging stops.
            if (reachableKm(currentPercent) >= remainingToDest) {
                break;
            }

            // Build the list of valid candidate stations for this step.
            List<Candidate> candidates = new ArrayList<>();
            for (ChargingStation station : allStations) {
                if (visited.contains(station.getId())) {
                    continue;
                }

                // Rule 1: only usable (AVAILABLE) stations can be used.
                if (station.getStatus() != StationStatus.AVAILABLE) {
                    rejected.put(station.getId(), "station is " + station.getStatus());
                    continue;
                }

                double distToStation = GeoUtil.distanceKm(
                        currentLat, currentLon, station.getLatitude(), station.getLongitude());

                // Rule 2: must be reachable with the current battery.
                if (distToStation > reachableKm(currentPercent)) {
                    rejected.put(station.getId(),
                            "too far to reach with current battery (" + round(distToStation) + " km away)");
                    continue;
                }

                // Rule 3: must move us closer to the destination (no back-tracking).
                double stationToDest = GeoUtil.distanceKm(
                        station.getLatitude(), station.getLongitude(),
                        request.getDestinationLat(), request.getDestinationLon());
                if (stationToDest >= remainingToDest) {
                    rejected.put(station.getId(), "does not move the trip closer to the destination");
                    continue;
                }

                candidates.add(new Candidate(station, distToStation, stationToDest));
            }

            // No reachable station that makes progress -> the trip is not possible.
            if (candidates.isEmpty()) {
                plan.setFeasible(false);
                plan.setDirectReach(false);
                plan.setMessage("Destination cannot be reached: no reachable charging station "
                        + "makes progress toward the destination from the current point.");
                finalizeTotals(plan, totalDistance, drivingTime, chargingTime, totalCost, currentPercent);
                plan.setRejectedStations(toRejectedList(allStations, rejected));
                return;
            }

            // Pick the winner for this mode and record why the others lost.
            Candidate best = chooseBest(request.getMode(), candidates, currentPercent, remainingToDest);
            String loseReason = rejectionReason(request.getMode());
            for (Candidate c : candidates) {
                if (c.station.getId().equals(best.station.getId())) {
                    rejected.remove(c.station.getId());   // the winner is not a rejection
                } else {
                    rejected.put(c.station.getId(), loseReason);
                }
            }

            // --- Drive to the chosen station and charge there ---
            double arrivalPercent = currentPercent - percentForDistance(best.distanceToStation);
            double energyAdded = vehicle.getBatteryCapacityKwh()
                    * (CHARGE_TO_PERCENT - arrivalPercent) / 100.0;
            double chargingPowerKw = Math.min(vehicle.getMaxChargingPowerKw(), best.station.getPowerKw());
            double chargeMinutes = energyAdded / chargingPowerKw * 60.0;
            double cost = energyAdded * best.station.getPricePerKwh();

            PlannedStopDto stop = new PlannedStopDto();
            stop.setStationId(best.station.getId());
            stop.setStationName(best.station.getName());
            stop.setLatitude(best.station.getLatitude());
            stop.setLongitude(best.station.getLongitude());
            stop.setDistanceFromPreviousKm(round(best.distanceToStation));
            stop.setBatteryArrivalPercent(round(arrivalPercent));
            stop.setBatteryDeparturePercent(CHARGE_TO_PERCENT);
            stop.setEnergyAddedKwh(round(energyAdded));
            stop.setChargingTimeMinutes(round(chargeMinutes));
            stop.setChargingCost(round(cost));
            plan.getStops().add(stop);

            // Update running totals and move our position to the station.
            totalDistance += best.distanceToStation;
            drivingTime += drivingMinutes(best.distanceToStation);
            chargingTime += chargeMinutes;
            totalCost += cost;

            currentLat = best.station.getLatitude();
            currentLon = best.station.getLongitude();
            currentPercent = CHARGE_TO_PERCENT;
            visited.add(best.station.getId());
        }

        // --- Final leg: drive from the last point to the destination ---
        double finalLeg = GeoUtil.distanceKm(
                currentLat, currentLon, request.getDestinationLat(), request.getDestinationLon());

        // If even after MAX_STOPS we still cannot finish, report infeasible.
        if (reachableKm(currentPercent) < finalLeg) {
            plan.setFeasible(false);
            plan.setDirectReach(false);
            plan.setMessage("Destination cannot be reached within a reasonable number of charging stops.");
            finalizeTotals(plan, totalDistance, drivingTime, chargingTime, totalCost, currentPercent);
            plan.setRejectedStations(toRejectedList(allStations, rejected));
            return;
        }

        totalDistance += finalLeg;
        drivingTime += drivingMinutes(finalLeg);
        double arrivalPercent = currentPercent - percentForDistance(finalLeg);

        plan.setFeasible(true);
        plan.setDirectReach(false);
        finalizeTotals(plan, totalDistance, drivingTime, chargingTime, totalCost, arrivalPercent);
        plan.setMessage("Destination reachable with " + plan.getStops().size()
                + " charging stop(s). Estimated battery on arrival: " + round(arrivalPercent) + "%.");
        plan.setRejectedStations(toRejectedList(allStations, rejected));
    }

    // ---------- choosing the best candidate for the mode ----------

    private Candidate chooseBest(OptimizationMode mode, List<Candidate> candidates,
                                 double currentPercent, double remainingToDest) {

        // For every candidate, work out its progress, charging time and cost.
        double bestProgress = 0;
        for (Candidate c : candidates) {
            c.progress = remainingToDest - c.stationToDest;   // km closer to the goal

            double arrivalPercent = currentPercent - percentForDistance(c.distanceToStation);
            double energyAdded = vehicle.getBatteryCapacityKwh() * (CHARGE_TO_PERCENT - arrivalPercent) / 100.0;
            double chargingPowerKw = Math.min(vehicle.getMaxChargingPowerKw(), c.station.getPowerKw());

            c.chargeTimeMinutes = energyAdded / chargingPowerKw * 60.0;
            c.chargeCost = energyAdded * c.station.getPricePerKwh();

            if (c.progress > bestProgress) {
                bestProgress = c.progress;
            }
        }

        // Step A: keep only stations that make good progress (>= 60% of the best).
        // This is what stops us from making lots of tiny, pointless hops.
        double progressThreshold = 0.6 * bestProgress;
        List<Candidate> goodProgress = new ArrayList<>();
        for (Candidate c : candidates) {
            if (c.progress >= progressThreshold) {
                goodProgress.add(c);
            }
        }

        // Step B: among those, let the mode pick.
        if (mode == OptimizationMode.FASTEST) {
            Candidate best = goodProgress.get(0);
            for (Candidate c : goodProgress) {
                if (c.chargeTimeMinutes < best.chargeTimeMinutes) {
                    best = c;
                }
            }
            return best;

        } else if (mode == OptimizationMode.CHEAPEST) {
            Candidate best = goodProgress.get(0);
            for (Candidate c : goodProgress) {
                if (c.chargeCost < best.chargeCost) {
                    best = c;
                }
            }
            return best;

        } else {
            // BALANCED: scale charging time and cost to 0..1 across the good-progress
            // stations, then pick the lowest 50/50 blend.
            double minTime = Double.MAX_VALUE, maxTime = -Double.MAX_VALUE;
            double minCost = Double.MAX_VALUE, maxCost = -Double.MAX_VALUE;
            for (Candidate c : goodProgress) {
                minTime = Math.min(minTime, c.chargeTimeMinutes);
                maxTime = Math.max(maxTime, c.chargeTimeMinutes);
                minCost = Math.min(minCost, c.chargeCost);
                maxCost = Math.max(maxCost, c.chargeCost);
            }

            Candidate best = null;
            double bestScore = Double.MAX_VALUE;
            for (Candidate c : goodProgress) {
                double blended = 0.5 * normalize(c.chargeTimeMinutes, minTime, maxTime)
                        + 0.5 * normalize(c.chargeCost, minCost, maxCost);
                if (blended < bestScore) {
                    bestScore = blended;
                    best = c;
                }
            }
            return best;
        }
    }

    // Scale a value to 0..1 between min and max (0 when all values are equal).
    private double normalize(double value, double min, double max) {
        if (max == min) {
            return 0;
        }
        return (value - min) / (max - min);
    }

    private String rejectionReason(OptimizationMode mode) {
        if (mode == OptimizationMode.FASTEST) {
            return "another reachable station was faster (better time per km toward the destination)";
        } else if (mode == OptimizationMode.CHEAPEST) {
            return "another reachable station was cheaper (better cost per km toward the destination)";
        } else {
            return "another reachable station had a better balance of time and cost";
        }
    }

    // ---------- small formatting/helper methods ----------

    private void finalizeTotals(TripPlanResponse plan, double totalDistance, double drivingTime,
                                double chargingTime, double totalCost, double arrivalPercent) {
        plan.setTotalDistanceKm(round(totalDistance));
        plan.setDrivingTimeMinutes(round(drivingTime));
        plan.setChargingTimeMinutes(round(chargingTime));
        plan.setTotalCost(round(totalCost));
        plan.setArrivalBatteryPercent(round(arrivalPercent));
    }

    private List<RejectedStationDto> toRejectedList(List<ChargingStation> allStations,
                                                    Map<Long, String> rejected) {
        List<RejectedStationDto> list = new ArrayList<>();
        for (ChargingStation station : allStations) {
            String reason = rejected.get(station.getId());
            if (reason != null) {
                list.add(new RejectedStationDto(station.getId(), station.getName(), reason));
            }
        }
        return list;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;   // one decimal place
    }
}
