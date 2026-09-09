package com.example.evrouteplanner.service;

import com.example.evrouteplanner.dto.StationResponse;
import com.example.evrouteplanner.model.ChargingStation;
import com.example.evrouteplanner.repository.ChargingStationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Cache-aside caching for the charging station list.
 *
 *   getStations():
 *     1. look in Redis (key "stations:all")
 *     2. HIT  -> return cached list
 *     3. MISS -> read from PostgreSQL, store in Redis with a TTL, return
 *
 *   invalidate():
 *     delete the key so the next read rebuilds it (called after a status change).
 */
@Service
public class StationCache {

    private static final String CACHE_KEY = "stations:all";

    private final ChargingStationRepository stationRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final long ttlSeconds;

    public StationCache(ChargingStationRepository stationRepository,
                        RedisTemplate<String, Object> redisTemplate,
                        @Value("${app.station-cache-ttl-seconds}") long ttlSeconds) {
        this.stationRepository = stationRepository;
        this.redisTemplate = redisTemplate;
        this.ttlSeconds = ttlSeconds;
    }

    @SuppressWarnings("unchecked")
    public List<StationResponse> getStations() {
        // 1. Try the cache first.
        Object cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            System.out.println("[Redis] Cache HIT for " + CACHE_KEY);
            return (List<StationResponse>) cached;
        }

        // 2. Cache miss: read the source of truth (PostgreSQL).
        System.out.println("[Redis] Cache MISS for " + CACHE_KEY + " -> reading from PostgreSQL");
        List<StationResponse> stations = new ArrayList<>();
        for (ChargingStation station : stationRepository.findAll()) {
            stations.add(new StationResponse(station));
        }

        // 3. Store in Redis with a TTL so it refreshes periodically.
        redisTemplate.opsForValue().set(CACHE_KEY, stations, Duration.ofSeconds(ttlSeconds));
        return stations;
    }

    public void invalidate() {
        redisTemplate.delete(CACHE_KEY);
    }
}
