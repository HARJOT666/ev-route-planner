package com.example.evrouteplanner.config;

import com.example.evrouteplanner.model.ChargingStation;
import com.example.evrouteplanner.model.StationStatus;
import com.example.evrouteplanner.repository.ChargingStationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds mock charging stations on first startup so the planner has data to work
 * with. Stations are spread roughly along the Delhi -> Mumbai corridor plus a
 * couple of off-route ones (useful for demonstrating why a station is rejected).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ChargingStationRepository stationRepository;

    public DataSeeder(ChargingStationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public void run(String... args) {
        // Only seed once.
        if (stationRepository.count() > 0) {
            return;
        }

        // The corridor waypoints (Jaipur, Ajmer, Udaipur, Ahmedabad, Surat) each
        // have THREE stations close together to create a real trade-off:
        //   - a FAST charger  : high power (quick) but expensive
        //   - a MID charger   : medium power and medium price
        //   - a VALUE charger : low power (slow) but cheap
        // This is why FASTEST, CHEAPEST and BALANCED pick different stations at the
        // same stop. (Use a vehicle with a high max charging power to feel the
        // difference between fast and mid chargers.)
        // name, latitude, longitude, powerKw, pricePerKwh
        saveTrio("Jaipur",    26.91, 75.79);
        saveTrio("Ajmer",     26.45, 74.64);
        saveTrio("Udaipur",   24.58, 73.68);
        saveTrio("Ahmedabad", 23.03, 72.58);
        saveTrio("Surat",     21.17, 72.83);

        // Off-route stations (far from the Delhi->Mumbai line) - always rejected.
        save("Lucknow East Charge",   26.85, 80.95, 50, 9.0);
        save("Bhopal Central Charge", 23.26, 77.41, 50, 9.0);

        System.out.println("[Seed] Inserted " + stationRepository.count() + " charging stations.");
    }

    // Creates the three station tiers for one corridor waypoint, slightly offset
    // so they show as separate markers on the map.
    private void saveTrio(String city, double lat, double lon) {
        save(city + " FastCharge",  lat,        lon,        150, 22.0);
        save(city + " MidCharge",   lat + 0.04, lon + 0.04, 60, 16.0);
        save(city + " ValueCharge", lat - 0.04, lon - 0.04, 30, 10.0);
    }

    private void save(String name, double lat, double lon, double powerKw, double pricePerKwh) {
        ChargingStation station = new ChargingStation();
        station.setName(name);
        station.setLatitude(lat);
        station.setLongitude(lon);
        station.setPowerKw(powerKw);
        station.setPricePerKwh(pricePerKwh);
        station.setStatus(StationStatus.AVAILABLE);
        stationRepository.save(station);
    }
}
