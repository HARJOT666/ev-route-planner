# ⚡ EV Route Planner with Charging-Stop Optimization

Plan an electric-vehicle road trip: enter a start, a destination and your current
battery level, and the app tells you **whether you can make it**, **where to
charge**, and **why those stops were chosen** — with an optional AI explanation.

This is an individual full-stack project built to be **simple and easy to read**:
plain Spring Boot services, a straightforward greedy optimization algorithm, and
a clean React dashboard.

---

## 1. The problem

EV drivers can't just "fill up anywhere". Battery range is limited, chargers are
spread out, they differ in **speed** (kW) and **price** (₹/kWh), and some are
**out of service**. Planning a long trip by hand is annoying and error-prone.

## 2. The solution

A web app that:
1. Checks if the destination is reachable on the current battery.
2. If not, finds reachable charging stations and builds a charging plan.
3. Optimizes the plan for **Fastest**, **Cheapest**, or **Balanced**.
4. Shows the route + stops on a map and saves the trip to history.
5. Uses **Gemini** to explain the plan in plain English (explanation only — it
   never makes the decisions).

## 3. Features

- Register / log in (JWT authentication)
- Add and select EV vehicles (battery size, efficiency, charging power)
- Plan a trip with a start, destination, battery %, and optimization mode
- Deterministic charging-stop optimizer (no machine learning)
- Interactive map (React Leaflet + OpenStreetMap)
- Trip summary: distance, driving time, charging time, cost, battery on arrival
- "Why other stations were not chosen" list (explainability)
- Per-user trip history with detail view
- AI explanation and a "ask a question about this trip" box (Gemini)
- Redis caching of station lookups
- Kafka events for charging-station status changes

## 4. Tech stack

**Backend:** Java 17, Spring Boot 3, Spring Web, Spring Data JPA, Spring
Security, JWT (jjwt), PostgreSQL, Redis, Apache Kafka, Gemini API.

**Frontend:** React 18, Vite, JavaScript, Axios, React Leaflet, OpenStreetMap.

**Infrastructure:** Docker Compose (PostgreSQL, Redis, Kafka in KRaft mode).

## 5. Architecture (modular monolith)

One Spring Boot application, organised into clear packages. No microservices.

```
              ┌─────────────────────────── React (Vite) ───────────────────────────┐
              │  Login • Trip Planner (map, form) • History • AI panel               │
              └───────────────▲───────────────────────────────────┬─────────────────┘
                              │ REST + JWT (Axios)                 │
              ┌───────────────┴───────────────────────────────────▼─────────────────┐
              │                       Spring Boot backend                            │
              │  controller → service → (optimizer | repository | gemini)            │
              │                                                                      │
              │   JWT filter ──► SecurityContext                                     │
              │   TripOptimizer (deterministic greedy algorithm)                     │
              │   StationCache (Redis cache-aside)                                   │
              │   Kafka producer/consumer (station status events)                    │
              └───────┬───────────────┬──────────────────┬───────────────┬──────────┘
                      │               │                  │               │
                 PostgreSQL         Redis              Kafka          Gemini API
              (source of truth)   (cache)         (async events)   (explanations)
```

Backend package layout:

```
src/main/java/com/example/evrouteplanner/
├── config/       RedisConfig, KafkaTopicConfig, DataSeeder
├── controller/   AuthController, VehicleController, TripController, StationController, AiController
├── dto/          request/response objects
├── model/        User, Vehicle, ChargingStation, Trip, TripStop, enums
├── repository/   Spring Data JPA repositories
├── service/      AuthService, VehicleService, TripService, ChargingStationService,
│                 StationCache, GeminiService
├── optimizer/    TripOptimizer, GeoUtil   ← the core algorithm
├── security/     JwtService, JwtAuthenticationFilter, SecurityConfig, CurrentUserService, ...
├── event/        StationStatusEvent, StationEventProducer, StationEventConsumer
└── exception/    GlobalExceptionHandler + custom exceptions
```

## 6. Why PostgreSQL

PostgreSQL is the **persistent source of truth**. Users, vehicles, trips, trip
stops and charging stations are stored here. Relationships are simple:

```
User ──< Vehicle        (a user owns many vehicles)
User ──< Trip ──< TripStop
ChargingStation         (shared system data, not owned by a user)
```

Trip planning always reads the **freshest** station data straight from
PostgreSQL, so a plan is never based on stale cached availability.

## 7. Why Redis

Redis caches the **charging-station list** that the frontend map requests
frequently. It uses a simple **cache-aside** pattern:

- **Key:** `stations:all`
- **Value:** the full list of stations as JSON
- **TTL:** 5 minutes (configurable)
- **Invalidation:** when a station's status changes, a Kafka consumer deletes the key

```
GET /api/stations → StationCache.getStations()
   → Redis has "stations:all"?  HIT  → return cached list
   → else                        MISS → read PostgreSQL → store in Redis → return
```

## 8. Why Kafka

Some things should happen **asynchronously**. When a charging station changes
status (e.g. `AVAILABLE → OUT_OF_SERVICE`), we don't want that to block anything.
So we publish an event and let a consumer react.

```
PUT /api/stations/{id}/status
   → update PostgreSQL
   → publish StationStatusEvent to topic "charging-station-events"
        → consumer receives it
        → invalidates the Redis station cache
```

Normal trip planning is **NOT** sent through Kafka — it stays synchronous (see §12).

## 9. Why Gemini

Gemini is an **explanation layer only**. The deterministic `TripOptimizer` makes
every decision (route feasibility, which stations, how long to charge, cost).
Gemini receives the **already-computed facts** and turns them into friendly text,
or answers a user's question about the plan. It can never invent or change a plan.

```
TripOptimizer → structured trip facts → GeminiService (prompt) → Gemini → readable explanation
```

## 10. JWT authentication flow

```
Register / Login (email + password)
   → AuthService verifies credentials (BCrypt)
   → JwtService.generateToken(email)  → returns JWT to the client

Every protected request:
   Authorization: Bearer <JWT>
   → JwtAuthenticationFilter extracts the token
   → JwtService validates it (signature + expiry)
   → loads the user, puts it in the SecurityContext
   → controller runs as the authenticated user
```

## 11. Core optimization algorithm (the important part)

Deterministic and greedy — plain loops and if-statements. See
`optimizer/TripOptimizer.java`.

**Physics:**
```
range(km)        = batteryCapacityKwh × (usable% / 100) × efficiencyKmPerKwh
usable%          = batteryPercent − 10   (keep a 10% reserve)
% used for D km  = (D / efficiencyKmPerKwh) / batteryCapacityKwh × 100
```

**Steps:**
1. If `range(startBattery) ≥ distance(start, destination)` → **direct trip**, done.
2. Otherwise run a greedy loop. From the current point:
   - If we can now reach the destination → stop looping.
   - Collect **candidate** stations that are: `AVAILABLE`, **reachable** with the
     current battery, and **closer to the destination** (always make progress).
   - **Step A** – keep only candidates that make *good* progress (≥ 60% of the
     best). This avoids tiny pointless hops and keeps the number of stops low.
   - **Step B** – pick among them by mode:
     - **FASTEST**  → shortest charging time (fast charger)
     - **CHEAPEST** → lowest charging cost (cheap electricity)
     - **BALANCED** → 50/50 blend of charging time and cost
   - "Drive" to the winner, charge to 80%, update battery/time/cost, repeat.
3. Add the final leg to the destination.

Every rejected station is recorded with a reason, so we can always explain
**why station B was chosen instead of A**.

**Costs computed:**
```
FASTEST  total time = driving time + charging time
CHEAPEST total cost = Σ charging cost per stop
BALANCED chooses stations that balance the two
```

## 12. Synchronous vs asynchronous flows

| Flow | Type | Why |
|------|------|-----|
| Trip planning | **Synchronous** | The user is waiting for the answer right now |
| Station lookup | Synchronous (with Redis cache) | Fast, read-heavy |
| Station status change | **Asynchronous** (Kafka) | Side-effects (cache invalidation) shouldn't block the request |
| AI explanation | Synchronous request to Gemini | Triggered by the user on demand |

## 13. Folder structure

```
ev-route-planner/
├── backend/            Spring Boot app (Maven)
├── frontend/           React app (Vite)
├── docker-compose.yml  PostgreSQL + Redis + Kafka
├── README.md
└── ARCHITECTURE_EXPLANATION.md   ← detailed walkthrough for presenting the project
```

## 14. How to run the project

### Prerequisites
- Java 17+ and Maven
- Node.js 18+ and npm
- Docker + Docker Compose

### Step 1 — Start the backing services
```bash
cd ev-route-planner
docker compose up -d
# starts PostgreSQL (5432), Redis (6379), Kafka (9092)
```

### Step 2 — Run the backend
```bash
cd backend
mvn spring-boot:run
# starts on http://localhost:8080
# On first start it auto-creates tables and seeds charging stations.
```

### Step 3 — Run the frontend
```bash
cd frontend
npm install
npm run dev
# open http://localhost:5173
```

### Step 4 — Use it
1. Register an account.
2. Add a vehicle (try **60 kWh, 6 km/kWh, 150 kW** to see the modes differ clearly).
3. Plan **Delhi → Mumbai** at ~90% battery and switch between Fastest / Cheapest / Balanced.

### Environment variables

Nothing is hard-coded; everything has a local-dev default. Override as needed:

| Variable | Default | Used for |
|----------|---------|----------|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `evrouteplanner` | PostgreSQL |
| `DB_USER` / `DB_PASSWORD` | `evuser` / `evpass` | PostgreSQL |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka |
| `APP_JWT_SECRET` | dev default (change in prod) | Signing JWTs (≥ 32 bytes) |
| `GEMINI_API_KEY` | *(empty)* | Enables AI explanations |

To enable the AI explanations, get a key from Google AI Studio and run the
backend with it set:
```bash
GEMINI_API_KEY=your_key_here mvn spring-boot:run
```
Without a key the app still works fully — the AI panel just shows a friendly
"AI is not configured" message.

---

See **ARCHITECTURE_EXPLANATION.md** for a deeper, presentation-focused walkthrough.
