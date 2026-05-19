# Transaction Aggregation API

A Spring Boot API that fetches financial transaction data from mock bank sources, categorizes each transaction, persists the results, and exposes a REST API to query them.

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Build | Gradle (Kotlin DSL) |
| HTTP Client | Spring `RestClient` |
| Resilience | Resilience4j (Circuit Breaker + Retry) |
| Concurrency | Java 21 Virtual Threads + `CompletableFuture` |
| Persistence | PostgreSQL + Spring Data JPA |
| Migrations | Liquibase |
| Caching | Redis (Spring Cache) |
| Mapping | MapStruct (compile-time) |
| Boilerplate | Lombok |
| Testing | JUnit 5 + Mockito |

---

## Running with Docker
This starts the app, PostgreSQL, and Redis together:

```bash
docker compose up --build
```

Wait until you see:
```
transaction-api  | Started TransactionAggregationApiApplication in X seconds
```

The API is now available at `http://localhost:8080`.

Other useful commands:
```bash
docker compose up --build -d   # run in background
docker compose down            # stop all containers
docker compose down -v         # stop and delete all data
```

---

## API Endpoints

Base URL: `http://localhost:8080/api/v1/transactions`

**Important:** Always call `POST /aggregate/{accountId}` first — this fetches, categorizes, and saves the data. All other endpoints read from the database. The mock data uses account ID `ACC-123456`.

---

### 1. Aggregate Transactions
**`POST /api/v1/transactions/aggregate/{accountId}`**

Fetches transactions from all mock bank sources in parallel, categorizes each one, saves them to the database, and returns the full list.

```bash
curl -X POST http://localhost:8080/api/v1/transactions/aggregate/ACC-123456
```

**Returns:** Array of categorized transactions
```json
[
  {
    "id": "a1b2c3d4-...",
    "accountId": "ACC-123456",
    "amount": -849.24,
    "timestamp": "2026-04-12T09:15:00",
    "description": "Nandos restaurant Rosebank",
    "category": "DINING"
  },
  {
    "id": "e5f6g7h8-...",
    "accountId": "ACC-123456",
    "amount": -2430.03,
    "timestamp": "2026-04-10T14:22:00",
    "description": "Pick n Pay Supermarket",
    "category": "GROCERIES"
  }
]
```

---

### 2. Get All Transactions for an Account
**`GET /api/v1/transactions/{accountId}`**

Returns all stored, categorized transactions for the account. Served from Redis cache after the first call.

```bash
curl http://localhost:8080/api/v1/transactions/ACC-123456
```

**Returns:** Same array format as above.

---

### 3. Filter by Category
**`GET /api/v1/transactions/{accountId}/category/{category}`**

Returns only transactions matching a specific category.

Valid categories: `GROCERIES`, `DINING`, `FUEL`, `ENTERTAINMENT`, `TRANSPORT`, `UTILITIES`, `HEALTH`, `SHOPPING`, `TRAVEL`, `OTHER`

```bash
curl http://localhost:8080/api/v1/transactions/ACC-123456/category/GROCERIES
curl http://localhost:8080/api/v1/transactions/ACC-123456/category/DINING
curl http://localhost:8080/api/v1/transactions/ACC-123456/category/ENTERTAINMENT
```

**Returns:** Array of transactions in that category only.

---

### 4. Spend Summary by Category
**`GET /api/v1/transactions/{accountId}/summary`**

Returns the total spend per category for the account. Negative values = money spent, positive = money received.

```bash
curl http://localhost:8080/api/v1/transactions/ACC-123456/summary
```

**Returns:**
```json
{
  "accountId": "ACC-123456",
  "totalTransactions": 1000,
  "spendByCategory": {
    "GROCERIES": -12430.55,
    "DINING": -4821.30,
    "ENTERTAINMENT": -2299.49,
    "FUEL": -3104.18,
    "UTILITIES": -1850.00,
    "OTHER": -5430.00
  }
}
```

---

### 5. Top Transactions by Amount
**`GET /api/v1/transactions/{accountId}/top?limit=N`**

Returns the top N transactions sorted by highest absolute amount. Defaults to 10 if `limit` is not specified.

```bash
curl "http://localhost:8080/api/v1/transactions/ACC-123456/top?limit=5"
curl "http://localhost:8080/api/v1/transactions/ACC-123456/top?limit=10"
```

**Returns:** Array of the N highest-value transactions (e.g. salary, large purchases).

---

### 6. All Transactions (All Accounts)
**`GET /api/v1/transactions`**

Returns every transaction stored across all accounts. Useful for admin or reporting views.

```bash
curl http://localhost:8080/api/v1/transactions
```

**Returns:** Array of all transactions in the database.

---

## Full Test Sequence

Run these in order after `docker compose up --build`:

```bash
# Step 1 — Populate the database
curl -X POST http://localhost:8080/api/v1/transactions/aggregate/ACC-123456

# Step 2 — View all transactions
curl http://localhost:8080/api/v1/transactions/ACC-123456 | python3 -m json.tool | head -40

# Step 3 — Filter groceries only
curl http://localhost:8080/api/v1/transactions/ACC-123456/category/GROCERIES | python3 -m json.tool

# Step 4 — View spend summary
curl http://localhost:8080/api/v1/transactions/ACC-123456/summary | python3 -m json.tool

# Step 5 — Top 5 highest transactions
curl "http://localhost:8080/api/v1/transactions/ACC-123456/top?limit=5" | python3 -m json.tool

# Step 6 — All transactions across all accounts
curl http://localhost:8080/api/v1/transactions | python3 -m json.tool | head -20

# Step 7 — Health check
curl http://localhost:8080/actuator/health
```

---

## Categorization

`Category.classify(String description)` matches keywords in the transaction description:

| Keywords | Category |
|---|---|
| woolworths, checkers, pick n pay, spar | `GROCERIES` |
| netflix, spotify, dstv, cinema | `ENTERTAINMENT` |
| eskom, electricity, municipality, telkom | `UTILITIES` |
| uber, bolt, taxi, gautrain | `TRANSPORT` |
| engen, shell, petrol, caltex | `FUEL` |
| kfc, nandos, restaurant, coffee | `DINING` |
| dischem, clicks, pharmacy, doctor | `HEALTH` |
| takealot, amazon, h&m, clothing | `SHOPPING` |
| flight, hotel, airbnb, airport | `TRAVEL` |
| (no match) | `OTHER` |

---

## Running Tests

```bash
./gradlew test
```

Tests include:
- `CategoryTest` — parameterized tests for every category keyword
- `TransactionServiceTest` — unit tests with mocked ports (no Spring context, no DB)
