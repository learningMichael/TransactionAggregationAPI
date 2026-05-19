# AWS Infrastructure Design — Transaction Aggregation API

> In this document I explore how the current Spring Boot application would evolve into a
> cloud-native system on AWS — capable of handling millions of transactions globally.
> I am considering availabiliby, scalability, security, and cost-efficiency in the design.
> What happens when one zone goes down? Obviously we do not want capitec app to be down :) so let's explore...
>
> The current app runs locally on a single machine. This design is the next evolutionary step.

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Traffic & Security Layer](#2-traffic--security-layer)
3. [Compute Layer](#3-compute-layer)
4. [Database & Cache](#4-database--cache)
5. [Async Processing](#5-async-processing)
6. [Reliability & Observability](#6-reliability--observability)
7. [AWS Services Summary](#7-aws-services-summary)

---

## 1. Architecture Overview

```
                        Global Users (Cape Town / London / New York)
                                         │
                              ┌──────────▼──────────┐
                              │     AWS Route 53     │
                              │  Latency-Based DNS   │
                              │  Cape Town → af-south-1
                              │  London    → eu-west-1
                              └──────────┬──────────┘
                                         │
                              ┌──────────▼──────────┐
                              │  WAF + CloudFront    │
                              │  DDoS & injection    │
                              │  protection at edge  │
                              └──────────┬──────────┘
                                         │
                              ┌──────────▼──────────┐
                              │   API Gateway        │
                              │  Auth (Cognito JWT)  │
                              │  Rate limiting       │
                              └──────┬──────┬────────┘
                                     │      │
                  ┌──────────────────▼┐    ┌▼─────────────────────┐
                  │  EKS — af-south-1  │    │  EKS — eu-west-1      │
                  │  Spring Boot Pods  │    │  Spring Boot Pods     │
                  │  (auto-scaled)     │    │  (auto-scaled)        │
                  └──────────┬─────────┘    └────────┬─────────────┘
                             │                       │
                  ┌──────────▼───────────────────────▼─────────────┐
                  │        Aurora Global Database (PostgreSQL)      │
                  │  PRIMARY (af-south-1) ── writes ──────────────► │
                  │  REPLICA  (eu-west-1) ◄── replication (<1s) ── │
                  └────────────────────────────────────────────────┘
                             │                       │
                  ┌──────────▼───────────────────────▼─────────────┐
                  │      ElastiCache Redis — Global Datastore       │
                  │  Cached transactions available in both regions  │
                  └────────────────────────────────────────────────┘
```

---

## 2. Traffic & Security Layer

### Route 53 — Latency-Based Routing
DNS measures real latency from the user's location and routes them to the nearest healthy region.
- Cape Town user → `af-south-1`
- London user → `eu-west-1`
- If `af-south-1` goes down → Route 53 health checks detect failure and reroute to `eu-west-1` automatically

### WAF + CloudFront
- **WAF** blocks SQL injection, XSS, and DDoS attacks before they reach any server
- **CloudFront** caches response headers at the edge — reducing latency for repeat requests

### API Gateway + Cognito
- Every request must carry a valid **OAuth2 JWT token** issued by Amazon Cognito
- API Gateway validates the token before the request reaches the Spring Boot pods
- Rate limiting is enforced per client to prevent abuse

---

## 3. Compute Layer

The existing `Dockerfile` is deployed as-is onto **Amazon EKS (Kubernetes)**.

```
EKS Cluster (af-south-1)
├── Deployment: transaction-aggregation-api
│   ├── Pod 1 (Spring Boot container)
│   ├── Pod 2 (Spring Boot container)
│   └── Pod N (auto-scaled by HPA)
```

**Auto-Scaling:**

| Scaler | Trigger | Action |
|---|---|---|
| **HPA** (Horizontal Pod Autoscaler) | CPU > 70% | Add more Spring Boot pods |
| **Cluster Autoscaler** | No capacity for pending pods | Add new EC2 nodes |

> **Key point:** Java 21 Virtual Threads (already in the codebase) allow each pod to handle a very
> large number of concurrent bank API calls without OS thread starvation — fewer pods are needed
> compared to traditional thread-per-request models.

---

## 4. Database & Cache

### Aurora Global Database (PostgreSQL)
A managed PostgreSQL-compatible database with built-in cross-region replication.

```
af-south-1 (PRIMARY)                eu-west-1 (REPLICA)
┌───────────────────────┐           ┌───────────────────────┐
│  All WRITES go here   │ ─────────►│  All READS go here    │
│                       │  <1s lag  │  Low-latency, local   │
└───────────────────────┘           └───────────────────────┘
```

- Existing **Liquibase migrations** work unchanged — Aurora is fully PostgreSQL-compatible
- If `af-south-1` fails, Aurora promotes the replica to primary in **under 1 minute** automatically
- Storage auto-scales to 128TB — no manual intervention as transaction volume grows

### ElastiCache Redis — Global Datastore
Extends the existing Redis caching layer across regions.

- A categorized transaction cached in Cape Town is **asynchronously replicated** to London
- A user who travels between regions has their data already warm in the local cache
- TTL and eviction policies remain the same as in `application.yml`

---

## 5. Async Processing

### The Problem with Synchronous Aggregation at Scale
The current flow makes the user **wait** for all bank API calls to complete before returning a response.
At scale, a slow bank API blocks the user's request for seconds.

### The Solution — SQS Queue + Background Worker

```
User           API Gateway        SQS Queue        Worker Pod
 │                  │                 │                 │
 │── POST /fetch ──►│                 │                 │
 │                  │── enqueue ─────►│                 │
 │◄── 202 Accepted ─│                 │                 │
 │  (processing      │                │── consume ─────►│
 │   in background)  │                │                 │── call Bank APIs
 │                   │                │                 │── classify
 │                   │                │                 │── save to Aurora
 │◄── WebSocket notification (AppSync) ────────────────►│
 │  (your transactions are ready)     │                 │
```

- User gets an **immediate 202 response** — no timeout risk
- The bank API call runs in a background worker — independently scalable
- Notification sent via **AWS AppSync (WebSocket)** when processing completes

### Transactional Outbox Pattern
**The problem it solves:** If the app saves to the DB but crashes before publishing the SQS event,
the event is lost forever.

**The solution:**
```
Single database transaction:
  INSERT INTO transactions (...)
  INSERT INTO outbox_events (event_type, payload)   ← same transaction
  COMMIT  ← both succeed or both fail

Separate process (Debezium / Lambda):
  Reads new outbox_events rows
  Publishes to SQS
  Marks event as PUBLISHED
```
The database and event stream are always in sync — no lost events, no duplicates.

---

## 6. Reliability & Observability

### Fault Tolerance

| Layer | Pattern | Behaviour |
|---|---|---|
| App | Resilience4j Circuit Breaker | Stops calling failing bank API; returns fallback immediately |
| App | Resilience4j Retry (3 attempts) | Retries transient errors before opening circuit |
| Queue | SQS Dead Letter Queue (DLQ) | Failed messages after 3 retries parked for manual replay |
| DB | Aurora auto-failover | Replica promoted to primary in <1 min if primary fails |

**Dead Letter Queue — no data is ever lost:**
```
SQS Queue → Worker fails 3× → Message moved to DLQ
                                      │
                         Engineer fixes bug, replays DLQ
                                      │
                              Worker succeeds 
```

### Secrets Management
**AWS Secrets Manager** stores and auto-rotates:
- Database credentials (`DB_USERNAME`, `DB_PASSWORD`)
- Bank API keys
- Redis auth tokens

Rotation happens without redeploying the app — credentials fetched at runtime via AWS SDK.

### Distributed Tracing — X-Ray + OpenTelemetry
Every request is assigned a **Trace ID**. A developer can see exactly where time was spent:

```
POST /api/v1/transactions/aggregate/ACC-123456  — 91ms total
├── API Gateway auth                              3ms
├── TransactionService.aggregate()
│   ├── Virtual Thread → MockBankDataProvider    12ms
│   ├── Virtual Thread → SecondBankAdapter       45ms  ← bottleneck
│   └── allOf() join                             45ms
├── Category.classify() × 1000                   8ms
└── Aurora INSERT × 1000                        22ms
```

---

## 7. AWS Services Summary

| Component | AWS Service | Why |
|---|---|---|
| **DNS** | Route 53 | Latency-based routing — nearest healthy region |
| **Edge Protection** | WAF + CloudFront | DDoS and injection protection before compute |
| **API Entry Point** | API Gateway + Cognito | JWT auth and rate limiting at the edge |
| **Compute** | EKS (Kubernetes) | Docker deployment, HPA auto-scaling |
| **Primary DB** | Aurora Global (PostgreSQL) | Managed PostgreSQL, cross-region replication |
| **Cache** | ElastiCache Redis Global | Cross-region cache, warm data everywhere |
| **Async Messaging** | SQS + SNS | Decouple aggregation from user request, DLQ for failures |
| **Secrets** | AWS Secrets Manager | Auto-rotation of credentials without redeploy |
| **Auth** | Amazon Cognito | Managed JWT issuance and validation |
| **Tracing** | AWS X-Ray + OpenTelemetry | End-to-end latency visibility across all layers |
| **Notifications** | AWS AppSync | WebSocket push when async job completes |

---