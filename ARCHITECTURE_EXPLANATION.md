# Architecture Explanation (for understanding & presenting)

This document explains the project in plain English so you can confidently walk
an interviewer through it. Read it top to bottom; each section maps to one file
or one flow.

---

## 0. The one-sentence pitch

> "It's a full-stack EV trip planner. A deterministic algorithm decides whether
> you can reach your destination and where to charge, optimizing for time, cost
> or a balance — PostgreSQL stores the data, Redis caches station lookups, Kafka
> handles station status changes asynchronously, and Gemini explains the result
> in plain English."

---

## 1. Complete request flow (React → Spring Boot → back)

Example: the user clicks **Plan Trip**.

```
React (TripPlanner.jsx + TripForm.jsx)
  │  axios POST /api/trips/plan  { start, dest, vehicleId, battery%, mode }
  │  Authorization: Bearer <JWT>        (added by axios interceptor)
  ▼
JwtAuthenticationFilter            reads token → validates → sets SecurityContext
  ▼
TripController.planTrip()          @RestController, maps JSON → TripRequest
  ▼
TripService.planTrip()
  │  - CurrentUserService.getCurrentUser()   (who is logged in?)
  │  - load Vehicle from PostgreSQL (must belong to the user)
  │  - load all ChargingStations from PostgreSQL (source of truth)
  │  - new TripOptimizer(vehicle).plan(request, stations)   ← the algorithm
  │  - save the Trip + TripStops to PostgreSQL
  ▼
TripPlanResponse  (JSON)  → back through the controller → axios → React state
  ▼
React renders the result: RouteMap, TripSummary, ChargingTimeline,
              StationAlternatives and AiAssistant components
```

Key idea: **controller is thin, service holds the logic, optimizer is pure
calculation.** You can read each layer top to bottom.

---

## 2. JWT authentication flow

Files: `security/JwtService.java`, `security/JwtAuthenticationFilter.java`,
`security/SecurityConfig.java`, `service/AuthService.java`.

```
REGISTER / LOGIN
  React → POST /api/auth/register or /login   (public endpoints)
  AuthService:
    register → hash password with BCrypt → save user → generate JWT
    login    → AuthenticationManager checks email+password → generate JWT
  Response: { token, name, email }
  React stores the token in localStorage (AuthContext).

EVERY PROTECTED REQUEST
  Request has header:  Authorization: Bearer <JWT>
        │
        ▼
  JwtAuthenticationFilter (runs once per request)
     1. read the "Authorization" header
     2. is it "Bearer ..."? extract the token
     3. JwtService.isValid(token)  → signature + expiry check
     4. JwtService.extractEmail(token)
     5. load the user, build an Authentication, put it in SecurityContext
        │
        ▼
  SecurityConfig says: /api/auth/** is public, everything else needs auth
        │
        ▼
  Controller runs — it now "knows" who the user is
```

Why it's simple: the token only stores the user's **email** as the subject.
No refresh tokens, no OAuth, no roles. Sessions are **stateless** — we
re-authenticate from the token on every request.

---

## 3. Redis cache hit vs cache miss

Files: `service/StationCache.java`, `config/RedisConfig.java`.

```
GET /api/stations
      │
      ▼
StationCache.getStations()
      │
      ├─ redisTemplate.get("stations:all")
      │
      ├─ value found?  ── YES ──►  CACHE HIT   → return cached list  (fast)
      │
      └─ NO ─►  CACHE MISS
                 → read all stations from PostgreSQL
                 → store list in Redis with 5-minute TTL
                 → return the list
```

**When is the cache cleared?** Only when a station's status changes — and that
happens via Kafka (next section), not directly. This is a clean **cache-aside**
pattern: the code reads through the cache, and the cache is invalidated on write.

You can see it live in the backend logs:
```
[Redis] Cache MISS for stations:all -> reading from PostgreSQL
[Redis] Cache HIT for stations:all
```

---

## 4. Kafka producer → topic → consumer flow

Files: `event/StationStatusEvent.java`, `event/StationEventProducer.java`,
`event/StationEventConsumer.java`, `config/KafkaTopicConfig.java`.

```
PUT /api/stations/{id}/status   { "status": "OUT_OF_SERVICE" }
      │
      ▼
ChargingStationService.updateStatus()
      1. load station, change status, SAVE to PostgreSQL   (source of truth first)
      2. StationEventProducer.publish(event)
             │  serialise event → JSON string
             ▼
        Kafka topic: "charging-station-events"
             │
             ▼
        StationEventConsumer.onStationEvent()   @KafkaListener
             → StationCache.invalidate()   (delete "stations:all" from Redis)
```

Backend logs show the whole chain:
```
[Kafka] Published station event: {"stationId":1,...,"newStatus":"OUT_OF_SERVICE"}
[Kafka] Received station event for 'Jaipur FastCharge': AVAILABLE -> OUT_OF_SERVICE
[Kafka] Station cache invalidated after status change.
```

**Why Kafka here and not for trip planning?** Trip planning is something the user
is actively waiting for — it must be synchronous and immediate. A status change,
on the other hand, has side effects (invalidate cache, and later you could
recompute affected trips, notify users, etc.) that are perfect for a fire-and-
forget event. Kafka decouples "the status changed" from "everything that should
react to it".

---

## 5. PostgreSQL as the source of truth

- Every important fact lives in PostgreSQL: users, vehicles, trips, trip stops,
  charging stations.
- Redis is only a **copy** for speed. If Redis is wiped, nothing is lost — the
  next read repopulates it from PostgreSQL.
- Trip planning reads stations **directly from PostgreSQL**, never from the
  cache, so a plan is always based on the true, current availability.

Entities and relationships (`model/`):
```
User (id, email, password-hash, name)
   ├──< Vehicle (batteryCapacityKwh, efficiencyKmPerKwh, maxChargingPowerKw)
   └──< Trip (start, dest, mode, distances, times, cost, feasible)
             └──< TripStop (station, battery in/out, energy, time, cost, order)
ChargingStation (name, lat, lon, powerKw, pricePerKwh, status)  ← shared data
```

---

## 6. Gemini explanation flow

Files: `service/GeminiService.java`, `controller/AiController.java`.

```
User clicks "Explain this plan"
      │
      ▼
POST /api/ai/trips/{tripId}/explain
      │
      ▼
AiController → load the saved Trip (must belong to the user)
            → TripService.toPlanResponse(trip)   (the real, computed facts)
            → GeminiService.explainPlan(plan)
                  builds a plain-text "facts" block:
                     From/To, vehicle, mode, distances, times, cost,
                     each charging stop with battery in/out
                  sends it to Gemini with the instruction
                     "explain using ONLY these facts, do not invent numbers"
            → save the explanation on the Trip
            → return the text
```

**The critical rule:** Gemini is given the finished plan and only writes prose
about it. It does **not** choose stations or compute anything. This keeps the
system trustworthy and deterministic — you can prove the numbers came from the
algorithm, not from an LLM guess.

If no `GEMINI_API_KEY` is set, the service returns a friendly "not configured"
message instead of failing.

---

## 7. The EV optimization algorithm, step by step

File: `optimizer/TripOptimizer.java` (+ `optimizer/GeoUtil.java` for distance).

### The numbers we work with
```
range(km)   = batteryCapacityKwh × (usable% / 100) × efficiencyKmPerKwh
usable%     = batteryPercent − 10        (always keep a 10% safety reserve)
distance    = Haversine straight-line distance between two lat/lon points
driving time = distance / 60 km/h
```

### Case 1 — can we go direct?
```
if range(startBattery) ≥ distance(start, destination):
    return a direct plan (no charging)
```

### Case 2 — we need to charge (greedy loop)
Repeat until we can reach the destination (max 8 stops as a safety guard):

```
1. remaining = distance(currentPosition, destination)
   if range(currentBattery) ≥ remaining:  break   (we can finish)

2. Build CANDIDATE stations. A station qualifies only if it is:
     - AVAILABLE
     - reachable now (distance ≤ current range)
     - closer to the destination than we are  (guarantees progress)
   (stations failing a rule are recorded with the reason they were rejected)

3. Step A — "drive as far as we safely can":
     keep only candidates whose progress ≥ 60% of the best candidate's progress.
     → this removes tiny, wasteful hops and keeps the stop count low.

4. Step B — pick the winner by mode:
     FASTEST  → smallest charging time  (chooses the fast charger)
     CHEAPEST → smallest charging cost  (chooses cheap electricity)
     BALANCED → lowest 50/50 blend of (normalised time) and (normalised cost)

5. "Drive" to the winner:
     arrivalBattery = current − %usedFor(distanceToStation)
     charge to 80%
     add up driving time, charging time, cost, distance
     move current position to the station; repeat
```

### The final leg
```
drive from the last stop to the destination
arrivalBattery = current − %usedFor(finalLeg)
```

### Why this is easy to explain
- It's a **greedy** algorithm: at each step it makes the locally best choice.
- Every rule is a plain `if`. Every choice has a reason we store and display.
- You can literally point at the "rejected stations" list and say:
  *"Station A was rejected because it was out of service; Station B was chosen
  because, among the reachable stations that make good progress, it had the
  shortest charging time."*

### Worked demo (Delhi → Mumbai, 60 kWh / 6 km/kWh / 150 kW car, 90% battery)
The corridor has three station tiers at each waypoint (Fast / Mid / Value):

| Mode | Stops chosen | Charging time | Cost |
|------|--------------|---------------|------|
| FASTEST  | all **Fast** chargers  | ~64 min  | ~₹3,545 |
| CHEAPEST | all **Value** chargers | ~324 min | ~₹1,621 |
| BALANCED | all **Mid** chargers   | ~160 min | ~₹2,562 |

Same 5 stops, same distance — only the *tier of charger* changes with the mode.
That's the whole story in one table.

---

## 8. Why a modular monolith (and not microservices)

- It's **one deployable app** with clear internal packages (controller, service,
  optimizer, repository, ...). Easy to run, easy to read, easy to demo.
- Microservices would add network calls, service discovery, distributed
  transactions and a lot of operational overhead — for a single-person project
  with modest scope, that's pure cost and no benefit.
- The package boundaries already give the *organisational* benefits of
  separation without the *distributed-systems* pain. If it ever needed to scale,
  the optimizer or the station service could be split out later.

## 9. Why we intentionally skipped CQRS and Event Sourcing

- **CQRS** (separate read and write models) solves problems we don't have. Our
  reads and writes are simple CRUD plus one algorithm. Splitting them would
  double the code for no gain.
- **Event Sourcing** (storing every change as an event and rebuilding state)
  would make the data model far harder to reason about. We only need the
  *current* state of trips and stations, which a normal relational schema stores
  perfectly.
- We already use Kafka **where it genuinely fits** — asynchronous station status
  events — without pretending the whole system is event-sourced.

The guiding principle for the whole project: **use each technology only where it
earns its place, and keep everything else boringly simple and readable.**

---

## 10. Quick file map (where to look during a demo)

| You're asked about... | Open this file |
|-----------------------|----------------|
| The algorithm | `optimizer/TripOptimizer.java` |
| JWT | `security/JwtService.java`, `security/JwtAuthenticationFilter.java` |
| Redis | `service/StationCache.java` |
| Kafka | `event/StationEventProducer.java`, `event/StationEventConsumer.java` |
| Gemini | `service/GeminiService.java` |
| Trip persistence | `service/TripService.java` |
| Data model | `model/` package |
| The React planner page | `frontend/src/pages/TripPlanner.jsx`, `components/TripForm.jsx` |
| The map | `frontend/src/components/RouteMap.jsx` |
| Summary stats + itinerary | `components/TripSummary.jsx`, `components/ChargingTimeline.jsx` |
| Alternatives + AI assistant | `components/StationAlternatives.jsx`, `components/AiAssistant.jsx` |
| Trip history page | `frontend/src/pages/TripHistory.jsx` |
