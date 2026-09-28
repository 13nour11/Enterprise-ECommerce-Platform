# Enterprise E-Commerce Platform

A microservices e-commerce backend built with **Spring Boot** and **Spring Cloud**, developed step by step across a microservices course (one `Session-N` branch per session).

It covers service discovery, centralized configuration, an API gateway, resilience patterns, event-driven messaging with Kafka, the Saga pattern, OAuth2 security with Keycloak, observability, Kubernetes, Istio, GitOps, and load testing.

## Architecture

```
                        ┌──────────────┐
   Client ──(JWT)──────▶│ API Gateway  │ :8080   rate limiting (Redis), JWT validation
                        └──────┬───────┘
                               │  lb:// (Eureka)
        ┌──────────────┬───────┴──────┬──────────────────┐
        ▼              ▼              ▼                  ▼
 ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────────┐
 │ Product     │ │ Order       │─▶ Inventory   │ │ Payment       │
 │ :8081       │ │ :8082       │ │ :8084       │ │ :8083         │
 │ Postgres +  │ │ Feign +     │ └─────────────┘ └───────────────┘
 │ Redis cache │ │ Resilience4j│────────────────────────▲
 └─────────────┘ └──────┬──────┘
                        │ Kafka (order-created, saga events)
                        ▼
                ┌───────────────┐
                │ Notification  │ :8089
                └───────────────┘

 Platform: Config Server :8888 · Discovery (Eureka) :8761 · Keycloak :8090
 Observability: Zipkin :9411 · Prometheus :9090 · Grafana :3000
```

## Services

| Service | Port | Responsibility |
|---|---|---|
| `config-server` | 8888 | Spring Cloud Config, serving configuration from a Git repository |
| `discovery-server` | 8761 | Eureka service registry |
| `api-gateway` | 8080 | Spring Cloud Gateway: routing, Redis rate limiting, JWT validation, API versioning |
| `product-server` | 8081 | Product catalog: PostgreSQL, Redis caching, CQRS (command/query split), metrics and tracing |
| `order-service` | 8082 | Order creation: checks inventory (Feign), calls payment with Resilience4j, publishes Kafka events, Saga orchestrator |
| `payment-service` | 8083 | Simulated payment with configurable failure rate and delay, for testing resilience |
| `inventory-service` | 8084 | Stock checks |
| `notification-service` | 8089 | Kafka consumer that logs confirmation and cancellation notifications, with a dead-letter topic |

## Tech Stack

- **Java 21** (API Gateway: Java 17), **Spring Boot 3.x / 4.x**, **Spring Cloud** (Gateway, Config, Eureka, OpenFeign)
- **Resilience4j**: Circuit Breaker, Retry, TimeLimiter, Bulkhead
- **Apache Kafka** (KRaft mode): event-driven communication and the Saga pattern
- **PostgreSQL 16**, **Redis 7** (cache + gateway rate limiter)
- **Keycloak 24**: OAuth2 / OIDC, resource server, client-credentials for service-to-service calls
- **Micrometer + Zipkin** (distributed tracing), **Prometheus + Grafana** (metrics)
- **Docker Compose**, **Kubernetes**, **Helm**, **Istio** (mTLS + canary traffic split), **Argo CD** (GitOps)
- **GitHub Actions** (CI), **k6** (load testing), **JUnit**, **WireMock** and **Pact** (contract tests)

## Getting Started

### Prerequisites

- Docker and Docker Compose
- JDK 21 and Maven (only if you want to run services outside Docker)

### Run everything with Docker Compose

```bash
docker compose up --build -d
```

Check the services:

| UI | URL | Login |
|---|---|---|
| Eureka dashboard | http://localhost:8761 | – |
| Keycloak admin | http://localhost:8090 | `admin` / `admin` (local dev only) |
| Zipkin | http://localhost:9411 | – |
| Prometheus | http://localhost:9090 | – |
| Grafana | http://localhost:3000 | `admin` / `admin` (default) |

Stop everything:

```bash
docker compose down
```

### Run a single service locally

Start the infrastructure in Docker, then run the service with its Maven wrapper:

```bash
docker compose up -d postgres redis kafka config-server discovery-server keycloak
```

```bash
cd product-server/product-server && ./mvnw spring-boot:run
```

## Security (Keycloak)

The realm `ecommerce-platform` is imported automatically from [`keycloak/realm-ecommerce-platform.json`](keycloak/realm-ecommerce-platform.json).

- **Clients**
  - `gateway`: confidential client for users (authorization code flow)
  - `order-service`: service account (client credentials) used when Order Service calls Inventory Service through Feign
- **Roles**: `customer`, `admin`
- **Test users**: `customer1` (customer), `admin1` (admin). These are for local development only.

Gateway rules:
- `GET /api/v1/products/**`: public
- Other product operations (create, update, delete): `ADMIN` role
- Everything else: requires a valid JWT

The Order Service secret can be overridden with the `ORDER_SERVICE_CLIENT_SECRET` environment variable.

## API

All requests go through the gateway at `http://localhost:8080`.

### Products: `/api/v1/products`

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/products` | List all products |
| GET | `/api/v1/products/{id}` | Get a product |
| GET | `/api/v1/products/summary` | List product summaries (projection) |
| GET | `/api/v1/products/{id}/summary` | Get a product summary |
| POST | `/api/v1/products` | Create a product (admin) |
| PUT | `/api/v1/products/{id}` | Update a product (admin) |
| DELETE | `/api/v1/products/{id}` | Delete a product (admin) |

The old `/api/products/**` path still works. The gateway rewrites it to `/api/v1/products` and adds `Deprecation` and `Sunset` headers.

### Orders: `/api/v1/orders`

```http
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{ "productId": "PROD-001", "quantity": 1, "amount": 100.00, "customerId": "1" }
```

Possible `status` values in the response:

| Status | Meaning |
|---|---|
| `CONFIRMED` | Stock available and payment succeeded |
| `REJECTED` | Not enough stock |
| `PENDING` | Payment failed or timed out (circuit breaker / time limiter fallback) |
| `QUEUED` | Too many concurrent payment calls (bulkhead fallback) |

### Inventory: `/api/v1/inventory`

`GET /api/v1/inventory/check?productId=PROD-001&quantity=1` returns `200` when stock is available and `409` when it isn't.

### Payments: `/api/v1/payments`

`POST /api/v1/payments` on Payment Service. It's normally called by Order Service, not by clients.

## Resilience (Order → Payment)

Configured in [`order-service/.../application.yaml`](order-service/order-service/src/main/resources/application.yaml):

| Pattern | Setting |
|---|---|
| Circuit Breaker | Count-based window of 3 calls, opens at 50% failures, waits 5s before half-open |
| Retry | 3 attempts, 500ms with exponential backoff (×2) |
| TimeLimiter | 2s timeout |
| Bulkhead | Max 10 concurrent calls, no waiting |

Payment Service fails on purpose to exercise these patterns: `payment.failure-rate` (default `0.5`) and `payment.delay-ms` (default `3000`).

## Messaging & Saga

Kafka topics:

| Topic | Producer | Consumer |
|---|---|---|
| `order-created` | Order Service | Notification Service |
| `saga-commands` / `saga-results` | Saga orchestrator in Order Service | Order Service |
| `payment-events`, `inventory-events` | – | Notification Service |

`OrderSagaOrchestrator` drives the order through reserve inventory → process payment. If payment fails, it runs the compensating step (release inventory) and marks the order `CANCELLED`.

## Observability

- **Tracing**: Product Service and Order Service export traces to Zipkin. Trace context is kept across async order processing and Feign calls.
- **Metrics**: Product Service exposes `/actuator/prometheus`, which Prometheus scrapes ([`prometheus.yml`](prometheus.yml)).
- **Health**: every service exposes `/actuator/health`. Order Service also exposes `/actuator/circuitbreakers`.

## Kubernetes, Istio & GitOps

Manifests for Product Service are in [`k8s/`](k8s):

| Path | Contents |
|---|---|
| `k8s/product-service/` | Namespace, ConfigMap, Secret, Deployment (stable + canary), Service, HPA, Istio `PeerAuthentication` (strict mTLS), `DestinationRule` and `VirtualService` (80% stable / 20% canary) |
| `k8s/product-service-chart/` | Helm chart (HPA 2–10 replicas at 70% CPU, ServiceAccount, Role) |
| `k8s/argocd/` | Argo CD `Application` that syncs `k8s/product-service` from `main` (auto prune + self-heal) |

Apply the plain manifests (the namespace file is applied first so Istio sidecars are injected on the first apply):

```bash
kubectl apply -f k8s/product-service/
```

Or install with Helm:

```bash
helm install product-service k8s/product-service-chart
```

## CI/CD

[`.github/workflows/product-service-ci.yml`](.github/workflows/product-service-ci.yml) runs on pushes and pull requests to `main` that touch Product Service:

1. **Test**: runs `mvn test` against PostgreSQL and Redis service containers
2. **Build & push**: builds the Docker image and pushes it to GitHub Container Registry (`ghcr.io`), tagged with the branch, `sha-<commit>` and `latest` (on `main`)

## Load Testing (k6)

[`k6/checkout-stress-test.js`](k6/checkout-stress-test.js) ramps up to 150 virtual users against `POST /api/v1/orders` and counts confirmed, pending and queued orders.

```bash
k6 run -e TEST_JWT=<access-token> -e BASE_URL=http://localhost:8080 k6/checkout-stress-test.js
```

## Project Structure

```
.
├── api-gateway/
├── config-server/
├── discovery-server/
├── product-server/
├── order-service/
├── payment-service/
├── inventory-service/
├── notification-service/
├── keycloak/            # Realm import
├── k8s/                 # Kubernetes manifests, Helm chart, Argo CD app
├── k6/                  # Load tests
├── .github/workflows/   # CI pipeline
├── docker-compose.yml
└── prometheus.yml
```

## Course Progress

Each session is kept on its own branch (`session-2` … `Session-24`) and merged into `main`, so you can check out any branch to see the project at that stage.

| Sessions | Topics |
|---|---|
| 1–3 | Product service, service discovery, config server, API gateway with JWT and rate limiting |
| 4–6 | Payment and Order services: Circuit Breaker, Retry, Bulkhead, TimeLimiter, inter-service communication (Feign, JWT propagation) |
| 7 | Distributed transactions: Saga (choreography) with Kafka and compensation |
| 8 | Redis caching |
| 9 | Dockerizing all services |
| 10 | Unit and integration tests for Product Service |
| 11 | Contract testing (Pact) and WireMock tests |
| 12 | Saga orchestrator |
| 13 | CI pipeline (GitHub Actions) and Notification Service |
| 14 | Kubernetes deployment, canary release, Argo CD |
| 15–16 | Kubernetes probes, Secrets, HPA, Helm chart |
| 17 | Tracing, metrics and structured logging |
| 18 | CQRS in Product Service |
| 19–20 | Keycloak, OAuth2 resource server, client credentials |
| 21 | Istio mTLS and weighted traffic split |
| 22 | API versioning |
| 23 | k6 stress test and bottleneck findings |

## Author

**Nourhan Fahmy** – [@13nour11](https://github.com/13nour11)
